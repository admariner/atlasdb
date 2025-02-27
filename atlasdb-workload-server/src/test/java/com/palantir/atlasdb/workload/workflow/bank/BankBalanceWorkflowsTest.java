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

package com.palantir.atlasdb.workload.workflow.bank;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.MoreExecutors;
import com.palantir.atlasdb.factory.TransactionManagers;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.workload.store.AtlasDbTransactionStore;
import com.palantir.atlasdb.workload.store.InteractiveTransactionStore;
import com.palantir.atlasdb.workload.store.IsolationLevel;
import com.palantir.atlasdb.workload.store.ReadOnlyTransactionStore;
import com.palantir.atlasdb.workload.store.WorkloadCell;
import com.palantir.atlasdb.workload.transaction.ColumnRangeSelection;
import com.palantir.atlasdb.workload.transaction.RowColumnRangeReadTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.WitnessedRowColumnRangeReadTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.WitnessedTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.WitnessedWriteTransactionAction;
import com.palantir.atlasdb.workload.util.AtlasDbUtils;
import com.palantir.atlasdb.workload.workflow.ImmutableTableConfiguration;
import com.palantir.atlasdb.workload.workflow.Workflow;
import com.palantir.atlasdb.workload.workflow.WorkflowHistory;
import com.palantir.common.concurrent.PTExecutors;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import org.junit.Test;

public final class BankBalanceWorkflowsTest {
    private static final String TABLE_NAME = "bank.bank";
    private static final BankBalanceWorkflowConfiguration CONFIGURATION =
            ImmutableBankBalanceWorkflowConfiguration.builder()
                    .tableConfiguration(ImmutableTableConfiguration.builder()
                            .tableName(TABLE_NAME)
                            .isolationLevel(IsolationLevel.SNAPSHOT)
                            .build())
                    .iterationCount(1)
                    .build();

    private final AtomicBoolean skipRunning = new AtomicBoolean();

    private final AtomicReference<Map<Integer, Optional<Integer>>> invalidBalances = new AtomicReference<>();

    private final InteractiveTransactionStore memoryStore = AtlasDbTransactionStore.create(
            TransactionManagers.createInMemory(ImmutableSet.of()),
            ImmutableMap.of(
                    TableReference.createWithEmptyNamespace(TABLE_NAME),
                    AtlasDbUtils.tableMetadata(IsolationLevel.SNAPSHOT)));

    private final Workflow workflow = BankBalanceWorkflows.create(
            memoryStore,
            CONFIGURATION,
            MoreExecutors.listeningDecorator(PTExecutors.newFixedThreadPool(1)),
            skipRunning,
            invalidBalances::set);

    @Test
    public void workflowHistoryTransactionStoreShouldBeReadOnly() {
        WorkflowHistory history = workflow.run();
        assertThat(history.transactionStore())
                .as("should return a read only transaction store")
                .isInstanceOf(ReadOnlyTransactionStore.class);
    }

    @Test
    public void bankWorkflowContainsInitialReadsForBalances() {
        WorkflowHistory history = workflow.run();

        List<WitnessedTransactionAction> actions =
                Iterables.getOnlyElement(history.history()).actions();

        assertThat(actions)
                .contains(WitnessedRowColumnRangeReadTransactionAction.builder()
                        .originalQuery(RowColumnRangeReadTransactionAction.builder()
                                .table(TABLE_NAME)
                                .row(BankBalanceWorkflows.ROW)
                                .columnRangeSelection(ColumnRangeSelection.builder()
                                        .endColumnExclusive(CONFIGURATION.numberOfAccounts())
                                        .build())
                                .build())
                        .columnsAndValues(ImmutableList.of())
                        .build());
    }

    @Test
    public void bankWorkflowContainsInitialWritesForBalances() {
        WorkflowHistory history = workflow.run();

        List<WitnessedTransactionAction> actions =
                Iterables.getOnlyElement(history.history()).actions();

        List<WorkloadCell> actualWrittenCells = actions.stream()
                .filter(WitnessedWriteTransactionAction.class::isInstance)
                .map(WitnessedWriteTransactionAction.class::cast)
                .map(WitnessedWriteTransactionAction::cell)
                .collect(Collectors.toList());

        List<WorkloadCell> expectedWrittenCells = IntStream.range(0, CONFIGURATION.numberOfAccounts())
                .boxed()
                .map(BankBalanceWorkflows::getCellForAccount)
                .collect(Collectors.toList());

        assertThat(actualWrittenCells).containsAll(expectedWrittenCells);
    }

    @Test
    public void bankWorkflowSetsSkipRunningToTrueWhenInvalidBalanceSet() {
        assertThat(skipRunning).isFalse();
        memoryStore.readWrite(txn -> txn.write(TABLE_NAME, BankBalanceWorkflows.getCellForAccount(0), 1));
        workflow.run();
        assertThat(skipRunning).isTrue();
    }

    @Test
    public void bankWorkflowDoesNotWitnessTransactionWhenSkipRunningSetToTrue() {
        skipRunning.set(true);
        assertThat(workflow.run().history()).isEmpty();
    }

    @Test
    public void bankWorkflowCallsConsumerWhenInvalidBalanceSet() {
        assertThat(invalidBalances.get()).isNull();
        memoryStore.readWrite(txn -> txn.write(TABLE_NAME, BankBalanceWorkflows.getCellForAccount(0), 1));
        workflow.run();
        Map<Integer, Optional<Integer>> balances = EntryStream.of(Map.of(0, Optional.of(1)))
                .append(IntStreamEx.range(1, CONFIGURATION.numberOfAccounts())
                        .boxed()
                        .collect(Collectors.toMap(i -> i, _i -> Optional.empty())))
                .toMap();
        assertThat(invalidBalances).hasValue(balances);
    }
}
