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

package com.palantir.atlasdb.workload.invariant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.palantir.atlasdb.workload.store.ColumnAndValue;
import com.palantir.atlasdb.workload.store.ReadableTransactionStore;
import com.palantir.atlasdb.workload.transaction.ColumnRangeSelection;
import com.palantir.atlasdb.workload.transaction.RowColumnRangeReadTransactionAction;
import com.palantir.atlasdb.workload.transaction.WitnessedTransactionsBuilder;
import com.palantir.atlasdb.workload.transaction.WorkloadTestHelpers;
import com.palantir.atlasdb.workload.transaction.witnessed.InvalidWitnessedRowColumnRangeReadTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.InvalidWitnessedSingleCellTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.InvalidWitnessedTransaction;
import com.palantir.atlasdb.workload.transaction.witnessed.InvalidWitnessedTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.WitnessedRowColumnRangeReadTransactionAction;
import com.palantir.atlasdb.workload.transaction.witnessed.WitnessedTransaction;
import com.palantir.atlasdb.workload.workflow.ImmutableWorkflowHistory;
import com.palantir.atlasdb.workload.workflow.WorkflowHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class SnapshotInvariantTest {
    @Mock
    private ReadableTransactionStore readableTransactionStore;

    @Test
    public void catchesWriteWriteConflict() {
        List<InvalidWitnessedTransaction> invalidTransactions = new ArrayList<>();
        List<WitnessedTransaction> transactions = new WitnessedTransactionsBuilder(WorkloadTestHelpers.TABLE_1)
                .startTransaction(1L)
                .write(5, 10, WorkloadTestHelpers.VALUE_ONE)
                .endTransaction(10L)
                .startTransaction(5L)
                .write(5, 10, WorkloadTestHelpers.VALUE_ONE)
                .endTransaction(20L)
                .build();
        WorkflowHistory workflowHistory = ImmutableWorkflowHistory.builder()
                .history(transactions)
                .transactionStore(readableTransactionStore)
                .build();
        SnapshotInvariant.INSTANCE.accept(workflowHistory, invalidTransactions::addAll);

        InvalidWitnessedTransaction invalidTransaction = Iterables.getOnlyElement(invalidTransactions);
        assertThat(invalidTransaction.transaction().startTimestamp()).isEqualTo(5L);
        assertThat(invalidTransaction.transaction().commitTimestamp()).contains(20L);

        InvalidWitnessedTransactionAction invalidAction = Iterables.getOnlyElement(invalidTransaction.invalidActions());
        assertThat(invalidAction)
                .isInstanceOfSatisfying(InvalidWitnessedSingleCellTransactionAction.class, invalidSingleCellAction -> {
                    assertThat(invalidSingleCellAction.action().table()).isEqualTo(WorkloadTestHelpers.TABLE_1);
                    assertThat(invalidSingleCellAction.mismatchedValue())
                            .isEqualTo(MismatchedValue.of(
                                    ValueAndMaybeTimestamp.of(WorkloadTestHelpers.VALUE_ONE, 1L),
                                    ValueAndMaybeTimestamp.empty()));
                });
    }

    @Test
    public void catchesAbaWriteWriteConflictForDeletes() {
        List<InvalidWitnessedTransaction> invalidTransactions = new ArrayList<>();
        List<WitnessedTransaction> transactions = new WitnessedTransactionsBuilder(WorkloadTestHelpers.TABLE_1)
                .startTransaction(1L)
                .delete(5, 10)
                .endTransaction(10L)
                .startTransaction(5L)
                .write(5, 10, WorkloadTestHelpers.VALUE_ONE)
                .endTransaction(20L)
                .build();
        WorkflowHistory workflowHistory = ImmutableWorkflowHistory.builder()
                .history(transactions)
                .transactionStore(readableTransactionStore)
                .build();
        SnapshotInvariant.INSTANCE.accept(workflowHistory, invalidTransactions::addAll);

        InvalidWitnessedTransaction invalidTransaction = Iterables.getOnlyElement(invalidTransactions);
        assertThat(invalidTransaction.transaction().startTimestamp()).isEqualTo(5L);
        assertThat(invalidTransaction.transaction().commitTimestamp()).contains(20L);

        InvalidWitnessedTransactionAction invalidAction = Iterables.getOnlyElement(invalidTransaction.invalidActions());
        assertThat(invalidAction)
                .isInstanceOfSatisfying(InvalidWitnessedSingleCellTransactionAction.class, invalidSingleCellAction -> {
                    assertThat(invalidSingleCellAction.action().table()).isEqualTo(WorkloadTestHelpers.TABLE_1);
                    assertThat(invalidSingleCellAction.mismatchedValue())
                            .isEqualTo(MismatchedValue.of(
                                    ValueAndMaybeTimestamp.of(Optional.empty(), 1L), ValueAndMaybeTimestamp.empty()));
                });
    }

    @Test
    public void doesNotCatchWriteSkew() {
        List<InvalidWitnessedTransaction> invalidTransactions = new ArrayList<>();
        List<WitnessedTransaction> transactions = new WitnessedTransactionsBuilder(WorkloadTestHelpers.TABLE_1)
                .startTransaction(1L)
                .write(5, 10, WorkloadTestHelpers.VALUE_ONE)
                .endTransaction(10L)
                .startTransaction(5L)
                .read(5, 10)
                .write(6, 10, WorkloadTestHelpers.VALUE_TWO)
                .endTransaction(20L)
                .build();
        WorkflowHistory workflowHistory = ImmutableWorkflowHistory.builder()
                .history(transactions)
                .transactionStore(readableTransactionStore)
                .build();
        SnapshotInvariant.INSTANCE.accept(workflowHistory, invalidTransactions::addAll);
        assertThat(invalidTransactions).isEmpty();
    }

    @Test
    public void doesNotCatchReadOnlyTransactionAnomaly() {
        List<InvalidWitnessedTransaction> invalidTransactions = new ArrayList<>();
        List<WitnessedTransaction> transactions = new WitnessedTransactionsBuilder(WorkloadTestHelpers.TABLE_1)
                .startTransaction(1L)
                .write(5, 10, WorkloadTestHelpers.VALUE_ONE)
                .endTransaction(10L)
                .startTransaction(11L)
                .read(5, 10, WorkloadTestHelpers.VALUE_ONE)
                .endTransaction(20L)
                .startTransaction(12L)
                .write(5, 10, WorkloadTestHelpers.VALUE_TWO)
                .endTransaction(13L)
                .build();
        WorkflowHistory workflowHistory = ImmutableWorkflowHistory.builder()
                .history(transactions)
                .transactionStore(readableTransactionStore)
                .build();
        SnapshotInvariant.INSTANCE.accept(workflowHistory, invalidTransactions::addAll);
        assertThat(invalidTransactions).isEmpty();
    }

    @Test
    public void properlyReplaysReadsAndWrites() {
        List<InvalidWitnessedTransaction> invalidTransactions = new ArrayList<>();
        List<WitnessedTransaction> transactions = new WitnessedTransactionsBuilder(WorkloadTestHelpers.TABLE_1)
                .startTransaction(1L)
                .write(5, 10, WorkloadTestHelpers.VALUE_ONE)
                .endTransaction(10L)
                .startTransaction(5L)
                .read(5, 10)
                .endTransaction(15L)
                .startTransaction(16L)
                .read(5, 10, WorkloadTestHelpers.VALUE_ONE)
                .endTransaction(17L)
                .build();
        WorkflowHistory workflowHistory = ImmutableWorkflowHistory.builder()
                .history(transactions)
                .transactionStore(readableTransactionStore)
                .build();
        SnapshotInvariant.INSTANCE.accept(workflowHistory, invalidTransactions::addAll);
        assertThat(invalidTransactions).isEmpty();
    }

    @Test
    public void rowColumnRangeScanNotExpectedToReadColumnsCommittingAfterOurStart() {
        List<InvalidWitnessedTransaction> invalidTransactions = new ArrayList<>();
        List<WitnessedTransaction> transactions = new WitnessedTransactionsBuilder(WorkloadTestHelpers.TABLE_1)
                .startTransaction(1L)
                .write(5, 10, WorkloadTestHelpers.VALUE_ONE)
                .endTransaction(10L)
                .startTransaction(5L)
                .write(5, 20, WorkloadTestHelpers.VALUE_TWO)
                .endTransaction(15L)
                .startTransaction(12L)
                .write(5, 30, WorkloadTestHelpers.VALUE_ONE + WorkloadTestHelpers.VALUE_TWO)
                .rowColumnRangeRead(
                        5,
                        ColumnRangeSelection.builder().build(),
                        ImmutableList.of(
                                ColumnAndValue.of(10, WorkloadTestHelpers.VALUE_ONE),
                                ColumnAndValue.of(30, WorkloadTestHelpers.VALUE_ONE + WorkloadTestHelpers.VALUE_TWO)))
                .endTransaction(21L)
                .build();
        WorkflowHistory workflowHistory = ImmutableWorkflowHistory.builder()
                .history(transactions)
                .transactionStore(readableTransactionStore)
                .build();
        SnapshotInvariant.INSTANCE.accept(workflowHistory, invalidTransactions::addAll);
        assertThat(invalidTransactions).isEmpty();
    }

    @Test
    public void rowColumnRangeScanFlagsColumnCommittingAfterOurStartAsViolating() {
        List<InvalidWitnessedTransaction> invalidTransactions = new ArrayList<>();
        List<WitnessedTransaction> transactions = new WitnessedTransactionsBuilder(WorkloadTestHelpers.TABLE_1)
                .startTransaction(5L)
                .write(5, 20, WorkloadTestHelpers.VALUE_TWO)
                .endTransaction(15L)
                .startTransaction(12L)
                .write(5, 30, WorkloadTestHelpers.VALUE_ONE)
                .rowColumnRangeRead(
                        5,
                        ColumnRangeSelection.builder().build(),
                        ImmutableList.of(
                                ColumnAndValue.of(20, WorkloadTestHelpers.VALUE_TWO),
                                ColumnAndValue.of(30, WorkloadTestHelpers.VALUE_ONE)))
                .endTransaction(21L)
                .build();
        WorkflowHistory workflowHistory = ImmutableWorkflowHistory.builder()
                .history(transactions)
                .transactionStore(readableTransactionStore)
                .build();
        SnapshotInvariant.INSTANCE.accept(workflowHistory, invalidTransactions::addAll);

        InvalidWitnessedTransaction invalidTransaction = Iterables.getOnlyElement(invalidTransactions);
        assertThat(invalidTransaction.transaction().startTimestamp()).isEqualTo(12L);
        assertThat(invalidTransaction.transaction().commitTimestamp()).contains(21L);

        InvalidWitnessedTransactionAction invalidAction = Iterables.getOnlyElement(invalidTransaction.invalidActions());
        assertThat(invalidAction)
                .isInstanceOfSatisfying(
                        InvalidWitnessedRowColumnRangeReadTransactionAction.class, invalidRowColumnRangeRead -> {
                            assertThat(invalidRowColumnRangeRead.witness())
                                    .isEqualTo(WitnessedRowColumnRangeReadTransactionAction.builder()
                                            .originalQuery(RowColumnRangeReadTransactionAction.builder()
                                                    .table(WorkloadTestHelpers.TABLE_1)
                                                    .row(5)
                                                    .columnRangeSelection(ColumnRangeSelection.builder()
                                                            .build())
                                                    .build())
                                            .columnsAndValues(ImmutableList.of(
                                                    ColumnAndValue.of(20, WorkloadTestHelpers.VALUE_TWO),
                                                    ColumnAndValue.of(30, WorkloadTestHelpers.VALUE_ONE)))
                                            .build());
                            assertThat(invalidRowColumnRangeRead.expectedColumnsAndValues())
                                    .containsExactly(ColumnAndValue.of(30, WorkloadTestHelpers.VALUE_ONE));
                        });
    }
}
