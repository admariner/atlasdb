/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.keyvalue.dbkvs.timestamp;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.common.base.Throwables;
import com.palantir.exception.PalantirSqlException;
import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.exceptions.SafeIllegalStateException;
import com.palantir.nexus.db.DBType;
import com.palantir.nexus.db.pool.ConnectionManager;
import com.palantir.nexus.db.pool.RetriableTransactions;
import com.palantir.nexus.db.pool.RetriableTransactions.TransactionResult;
import com.palantir.nexus.db.pool.RetriableWriteTransaction;
import com.palantir.timestamp.MultipleRunningTimestampServiceError;
import com.palantir.timestamp.TimestampBoundStore;

// TODO(hsaraogi): switch to using ptdatabase sql running, which more gracefully supports multiple db types.
public class InDbTimestampBoundStore implements TimestampBoundStore {
    private static final String EMPTY_TABLE_PREFIX = "";

    private final ConnectionManager connManager;
    private final PhysicalBoundStoreStrategy physicalBoundStoreStrategy;

    @GuardedBy("this") // lazy init to avoid db connections in constructors
    private DBType dbType;

    @GuardedBy("this")
    private Long currentLimit = null;

    /**
     * Use only if you have already initialized the timestamp table. This exists for legacy support.
     */
    public InDbTimestampBoundStore(ConnectionManager connManager, TableReference timestampTable) {
        this(connManager, new MultiSequencePhysicalBoundStoreStrategy(timestampTable, "chocolate"));
    }

    public static InDbTimestampBoundStore create(ConnectionManager connManager, TableReference timestampTable) {
        return InDbTimestampBoundStore.create(connManager, timestampTable, EMPTY_TABLE_PREFIX);
    }

    public static InDbTimestampBoundStore create(
            ConnectionManager connManager,
            TableReference timestampTable,
            String tablePrefixString) {
        InDbTimestampBoundStore inDbTimestampBoundStore = new InDbTimestampBoundStore(
                connManager,
                new MultiSequencePhysicalBoundStoreStrategy(timestampTable, "tom"));
        inDbTimestampBoundStore.init();
        return inDbTimestampBoundStore;
    }

    private InDbTimestampBoundStore(ConnectionManager connManager,
            PhysicalBoundStoreStrategy physicalBoundStoreStrategy) {
        this.connManager = Preconditions.checkNotNull(connManager, "connectionManager is required");
        this.physicalBoundStoreStrategy = physicalBoundStoreStrategy;
    }

    private void init() {
        try (Connection conn = connManager.getConnection()) {
            physicalBoundStoreStrategy.createTimestampTable(conn, this::getDbType);
        } catch (SQLException error) {
            throw PalantirSqlException.create(error);
        }
    }

    private interface Operation {
        long run(Connection connection, @Nullable Long oldLimit) throws SQLException;
    }

    @GuardedBy("this")
    private long runOperation(final Operation operation) {
        TransactionResult<Long> result = RetriableTransactions.run(connManager, new RetriableWriteTransaction<Long>() {
            @GuardedBy("InDbTimestampBoundStore.this")
            @Override
            public Long run(Connection connection) throws SQLException {
                Long oldLimit = physicalBoundStoreStrategy.readLimit(connection);
                if (currentLimit != null) {
                    if (oldLimit != null) {
                        if (currentLimit.equals(oldLimit)) {
                            // match, good
                        } else {
                            // mismatch
                            throw new MultipleRunningTimestampServiceError(
                                    "Timestamp limit changed underneath us (limit in memory: " + currentLimit
                                            + ", limit in DB: " + oldLimit + "). This may indicate that "
                                            + "another timestamp service is running against this database!  "
                                            + "This may indicate that your services are not properly configured "
                                            + "to run in an HA configuration, or to have a CLI run against them.");
                        }
                    } else {
                        // disappearance
                        throw new SafeIllegalStateException(
                                "Unable to retrieve a timestamp when expected. "
                                        + "This service is in a dangerous state and should be taken down "
                                        + "until a new safe timestamp value can be established in the KVS. "
                                        + "Please contact support.");
                    }
                } else {
                    // first read, no check to be done
                }
                return operation.run(connection, oldLimit);
            }
        });
        switch (result.getStatus()) {
            case SUCCESSFUL:
                currentLimit = result.getResultValue();
                return currentLimit;
            case UNKNOWN:
            case FAILED:
                if (result.getStatus() == RetriableTransactions.TransactionStatus.UNKNOWN) {
                    // Since the DB's state is unknown, the in-memory state can't be confirmed, so get rid of it.
                    currentLimit = null;
                }

                Throwable error = result.getError();
                if (error instanceof SQLException) {
                    throw PalantirSqlException.create((SQLException) error);
                }
                throw Throwables.rewrapAndThrowUncheckedException(error);
            default:
                throw new IllegalStateException("Unrecognized transaction status " + result.getStatus());
        }
    }

    @Override
    public synchronized long getUpperLimit() {
        return runOperation((connection, oldLimit) -> {
            if (oldLimit != null) {
                return oldLimit;
            }

            final long startVal = 10000;
            physicalBoundStoreStrategy.createLimit(connection, startVal);
            return startVal;
        });
    }

    @Override
    public synchronized void storeUpperLimit(final long limit) {
        runOperation((connection, oldLimit) -> {
            if (oldLimit != null) {
                physicalBoundStoreStrategy.writeLimit(connection, limit);
            } else {
                physicalBoundStoreStrategy.createLimit(connection, limit);
            }

            return limit;
        });
    }

    @GuardedBy("this")
    private DBType getDbType(Connection connection) {
        if (dbType == null) {
            dbType = ConnectionDbTypes.getDbType(connection);
        }
        return dbType;
    }
}
