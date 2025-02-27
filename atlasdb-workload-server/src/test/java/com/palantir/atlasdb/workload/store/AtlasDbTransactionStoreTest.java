/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.atlasdb.workload.store;

import static com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers.INDEX_REFERENCE;
import static com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers.TABLES_TO_ATLAS_METADATA;
import static com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers.TABLE_1;
import static com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers.TABLE_1_INDEX_1;
import static com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers.TABLE_REFERENCE;
import static com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers.VALUE_ONE;
import static com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers.WORKLOAD_CELL_ONE;
import static com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers.WORKLOAD_CELL_THREE;
import static com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers.WORKLOAD_CELL_TWO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import com.palantir.atlasdb.factory.TransactionManagers;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.transaction.api.ConditionAwareTransactionTask;
import com.palantir.atlasdb.transaction.api.ConflictHandler;
import com.palantir.atlasdb.transaction.api.Transaction;
import com.palantir.atlasdb.transaction.api.TransactionManager;
import com.palantir.atlasdb.transaction.encoding.V1EncodingStrategy;
import com.palantir.atlasdb.transaction.impl.TransactionConstants;
import com.palantir.atlasdb.workload.store.AtlasDbTransactionStore.CommitTimestampProvider;
import com.palantir.atlasdb.workload.transaction.DeleteTransactionAction;
import com.palantir.atlasdb.workload.transaction.SingleCellReadTransactionAction;
import com.palantir.atlasdb.workload.transaction.WitnessToActionVisitor;
import com.palantir.atlasdb.workload.transaction.WriteTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.MaybeWitnessedTransaction;
import com.palantir.atlasdb.workload.transaction.witnessed.WitnessedSingleCellReadTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.WitnessedTransaction;
import com.palantir.atlasdb.workload.transaction.witnessed.WitnessedTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.WitnessedWriteTransactionAction;
import com.palantir.atlasdb.workload.util.AtlasDbUtils;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import com.palantir.logsafe.exceptions.SafeIllegalStateException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public final class AtlasDbTransactionStoreTest {

    private static final long START_TS = 1;
    private static final long COMMIT_TS = 2;

    private TransactionManager manager;

    private AtlasDbTransactionStore store;

    @Before
    public void before() {
        manager = TransactionManagers.createInMemory(Set.of());
        store = AtlasDbTransactionStore.create(manager, TABLES_TO_ATLAS_METADATA);
    }

    @Test
    public void createsTables() {
        assertThat(manager.getKeyValueService().getAllTableNames()).contains(TABLE_REFERENCE, INDEX_REFERENCE);
    }

    @Test
    public void canWriteDataToStore() {
        Optional<WitnessedTransaction> witnessedTransactionPrimaryTable =
                store.readWrite(List.of(WriteTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE, VALUE_ONE)));
        Optional<WitnessedTransaction> witnessedTransactionIndexTable =
                store.readWrite(List.of(WriteTransactionAction.of(TABLE_1_INDEX_1, WORKLOAD_CELL_TWO, VALUE_ONE)));
        assertThat(witnessedTransactionPrimaryTable).isPresent();
        assertThat(witnessedTransactionIndexTable).isPresent();
        assertThat(store.get(TABLE_1, WORKLOAD_CELL_ONE)).contains(VALUE_ONE);
        assertThat(store.get(TABLE_1_INDEX_1, WORKLOAD_CELL_TWO)).contains(VALUE_ONE);
    }

    @Test
    public void witnessedTransactionMaintainsOrder() {
        List<WitnessedTransactionAction> actions = List.of(
                WitnessedWriteTransactionAction.of(TABLE_1, WORKLOAD_CELL_TWO, 100),
                WitnessedSingleCellReadTransactionAction.of(TABLE_1, WORKLOAD_CELL_TWO, Optional.of(100)),
                WitnessedSingleCellReadTransactionAction.of(TABLE_1, WORKLOAD_CELL_THREE, Optional.empty()),
                WitnessedWriteTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE, 24),
                WitnessedSingleCellReadTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE, Optional.of(24)));
        Optional<WitnessedTransaction> maybeTransaction = store.readWrite(actions.stream()
                .map(action -> action.accept(WitnessToActionVisitor.INSTANCE))
                .collect(Collectors.toList()));
        assertThat(maybeTransaction).isPresent().hasValueSatisfying(txn -> {
            assertThat(txn.actions()).containsExactlyElementsOf(actions);
            assertThat(txn.commitTimestamp()).isNotEmpty();
        });
    }

    @Test
    public void readWriteHandlesAllTransactionTypes() {
        store.readWrite(List.of(WriteTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE, VALUE_ONE)));
        assertThat(store.readWrite(List.of(SingleCellReadTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE))))
                .isPresent()
                .map(WitnessedTransaction::actions)
                .map(Iterables::getOnlyElement)
                .map(WitnessedSingleCellReadTransactionAction.class::cast)
                .map(WitnessedSingleCellReadTransactionAction::value)
                .contains(Optional.of(VALUE_ONE));
        store.readWrite(List.of(DeleteTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE)));
        assertThat(store.get(TABLE_1, WORKLOAD_CELL_ONE)).isEmpty();
    }

    @Test
    public void readWriteThrowsWhenTableDoesNotExist() {
        assertThatThrownBy(() ->
                        store.readWrite(List.of(SingleCellReadTransactionAction.of("chocolate", WORKLOAD_CELL_ONE))))
                .isInstanceOf(SafeIllegalArgumentException.class)
                .hasMessageContaining("Transaction action has unknown table.");
    }

    @Test
    public void readWriteReturnsEmptyWhenExceptionThrown() {
        TransactionManager transactionManager = mock(TransactionManager.class);
        KeyValueService keyValueService = mock(KeyValueService.class);
        when(transactionManager.getKeyValueService()).thenReturn(keyValueService);
        when(transactionManager.runTaskWithRetry(any())).thenThrow(new RuntimeException());
        AtlasDbTransactionStore transactionStore = AtlasDbTransactionStore.create(
                transactionManager, Map.of(TABLE_REFERENCE, AtlasDbUtils.tableMetadata(ConflictHandler.SERIALIZABLE)));
        assertThat(transactionStore.readWrite(List.of(SingleCellReadTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE))))
                .isEmpty();
    }

    @Test
    public void readWriteHandlesReadOnlyTransaction() {
        Optional<WitnessedTransaction> witnessedTransaction =
                store.readWrite(List.of(SingleCellReadTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE)));
        assertThat(witnessedTransaction).isPresent();
        assertThat(witnessedTransaction.get().commitTimestamp()).isEmpty();
    }

    @Test
    public void abortedTransactionsReturnEmpty() {
        TransactionManager onlyAbortsManager = spy(manager);
        Transaction abortedTransaction = mock(Transaction.class);
        when(abortedTransaction.isAborted()).thenReturn(true);
        doReturn(abortedTransaction).when(onlyAbortsManager).runTaskWithConditionWithRetry(any(Supplier.class), any());
        AtlasDbTransactionStore onlyAbortsStore =
                AtlasDbTransactionStore.create(onlyAbortsManager, TABLES_TO_ATLAS_METADATA);
        assertThat(onlyAbortsStore.readWrite(List.of(SingleCellReadTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE))))
                .isEmpty();
    }

    @Test
    public void keyAlreadyExistExceptionResultsInMaybeWitnessedTransaction() {
        WriteTransactionAction writeTransactionAction =
                WriteTransactionAction.of(TABLE_1, WORKLOAD_CELL_ONE, VALUE_ONE);
        TransactionManager keyAlreadyExistsExceptionThrowingStore = spy(manager);
        doAnswer(invocation -> {
                    Supplier<CommitTimestampProvider> commitTimestampFetcher = invocation.getArgument(0);
                    ConditionAwareTransactionTask<Void, CommitTimestampProvider, Exception> task =
                            invocation.getArgument(1);
                    return manager.runTaskWithConditionWithRetry(commitTimestampFetcher, (txn, condition) -> {
                        manager.getKeyValueService()
                                .putUnlessExists(
                                        TransactionConstants.TRANSACTION_TABLE,
                                        Map.of(
                                                V1EncodingStrategy.INSTANCE.encodeStartTimestampAsCell(
                                                        txn.getTimestamp()),
                                                Ints.toByteArray(-1)));
                        task.execute(txn, condition);
                        return null;
                    });
                })
                .when(keyAlreadyExistsExceptionThrowingStore)
                .runTaskWithConditionWithRetry(any(Supplier.class), any());
        AtlasDbTransactionStore onlyKeyAlreadyExistsThrowingStore =
                AtlasDbTransactionStore.create(keyAlreadyExistsExceptionThrowingStore, TABLES_TO_ATLAS_METADATA);
        assertThat(onlyKeyAlreadyExistsThrowingStore.readWrite(List.of(writeTransactionAction)))
                .hasValueSatisfying(witnessedTransaction -> {
                    assertThat(witnessedTransaction).isInstanceOf(MaybeWitnessedTransaction.class);
                    assertThat(witnessedTransaction.actions()).containsExactly(writeTransactionAction.witness());
                });
    }

    @Test
    public void getCommitTimestampIsEmptyWhenMatchesStartTimestamp() {
        CommitTimestampProvider commitTimestampProvider = new CommitTimestampProvider();
        commitTimestampProvider.throwIfConditionInvalid(START_TS);
        assertThat(commitTimestampProvider.getCommitTimestampOrThrowIfMaybeNotCommitted(START_TS))
                .isEmpty();
    }

    @Test
    public void getCommitTimestampThrowsWhenNoTimestampSet() {
        assertThatThrownBy(() -> new CommitTimestampProvider().getCommitTimestampOrThrowIfMaybeNotCommitted(START_TS))
                .isInstanceOf(SafeIllegalStateException.class)
                .hasMessageContaining(
                        "Timestamp has not been set, which means the pre commit condition has never been executed");
    }

    @Test
    public void getCommitTimestampReturnsPresentWhenCommitAndStartTimestampMismatch() {
        CommitTimestampProvider commitTimestampProvider = new CommitTimestampProvider();
        commitTimestampProvider.throwIfConditionInvalid(COMMIT_TS);
        assertThat(commitTimestampProvider.getCommitTimestampOrThrowIfMaybeNotCommitted(START_TS))
                .hasValue(COMMIT_TS);
    }
}
