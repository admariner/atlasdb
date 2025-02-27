package com.palantir.atlasdb.blob.generated;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;
import com.google.protobuf.InvalidProtocolBufferException;
import com.palantir.atlasdb.compress.CompressionUtils;
import com.palantir.atlasdb.encoding.PtBytes;
import com.palantir.atlasdb.keyvalue.api.BatchColumnRangeSelection;
import com.palantir.atlasdb.keyvalue.api.Cell;
import com.palantir.atlasdb.keyvalue.api.ColumnRangeSelection;
import com.palantir.atlasdb.keyvalue.api.ColumnRangeSelections;
import com.palantir.atlasdb.keyvalue.api.ColumnSelection;
import com.palantir.atlasdb.keyvalue.api.Namespace;
import com.palantir.atlasdb.keyvalue.api.Prefix;
import com.palantir.atlasdb.keyvalue.api.RangeRequest;
import com.palantir.atlasdb.keyvalue.api.RowResult;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.keyvalue.impl.Cells;
import com.palantir.atlasdb.ptobject.EncodingUtils;
import com.palantir.atlasdb.table.api.AtlasDbDynamicMutablePersistentTable;
import com.palantir.atlasdb.table.api.AtlasDbMutablePersistentTable;
import com.palantir.atlasdb.table.api.AtlasDbNamedMutableTable;
import com.palantir.atlasdb.table.api.AtlasDbNamedPersistentSet;
import com.palantir.atlasdb.table.api.ColumnValue;
import com.palantir.atlasdb.table.api.TypedRowResult;
import com.palantir.atlasdb.table.description.ColumnValueDescription.Compression;
import com.palantir.atlasdb.table.description.ValueType;
import com.palantir.atlasdb.table.generation.ColumnValues;
import com.palantir.atlasdb.table.generation.Descending;
import com.palantir.atlasdb.table.generation.NamedColumnValue;
import com.palantir.atlasdb.transaction.api.AtlasDbConstraintCheckingMode;
import com.palantir.atlasdb.transaction.api.ConstraintCheckingTransaction;
import com.palantir.atlasdb.transaction.api.ImmutableGetRangesQuery;
import com.palantir.atlasdb.transaction.api.Transaction;
import com.palantir.common.base.AbortingVisitor;
import com.palantir.common.base.AbortingVisitors;
import com.palantir.common.base.BatchingVisitable;
import com.palantir.common.base.BatchingVisitableView;
import com.palantir.common.base.BatchingVisitables;
import com.palantir.common.base.Throwables;
import com.palantir.common.collect.IterableView;
import com.palantir.common.persist.Persistable;
import com.palantir.common.persist.Persistable.Hydrator;
import com.palantir.common.persist.Persistables;
import com.palantir.util.AssertUtils;
import com.palantir.util.crypto.Sha256Hash;

@Generated("com.palantir.atlasdb.table.description.render.TableRenderer")
@SuppressWarnings({"all", "deprecation"})
public final class HotspottyDataStreamIdxTable implements
        AtlasDbDynamicMutablePersistentTable<HotspottyDataStreamIdxTable.HotspottyDataStreamIdxRow,
                                                HotspottyDataStreamIdxTable.HotspottyDataStreamIdxColumn,
                                                HotspottyDataStreamIdxTable.HotspottyDataStreamIdxColumnValue,
                                                HotspottyDataStreamIdxTable.HotspottyDataStreamIdxRowResult> {
    private final Transaction t;
    private final List<HotspottyDataStreamIdxTrigger> triggers;
    private final static String rawTableName = "hotspottyData_stream_idx";
    private final TableReference tableRef;
    private final static ColumnSelection allColumns = ColumnSelection.all();

    static HotspottyDataStreamIdxTable of(Transaction t, Namespace namespace) {
        return new HotspottyDataStreamIdxTable(t, namespace, ImmutableList.<HotspottyDataStreamIdxTrigger>of());
    }

    static HotspottyDataStreamIdxTable of(Transaction t, Namespace namespace, HotspottyDataStreamIdxTrigger trigger, HotspottyDataStreamIdxTrigger... triggers) {
        return new HotspottyDataStreamIdxTable(t, namespace, ImmutableList.<HotspottyDataStreamIdxTrigger>builder().add(trigger).add(triggers).build());
    }

    static HotspottyDataStreamIdxTable of(Transaction t, Namespace namespace, List<HotspottyDataStreamIdxTrigger> triggers) {
        return new HotspottyDataStreamIdxTable(t, namespace, triggers);
    }

    private HotspottyDataStreamIdxTable(Transaction t, Namespace namespace, List<HotspottyDataStreamIdxTrigger> triggers) {
        this.t = t;
        this.tableRef = TableReference.create(namespace, rawTableName);
        this.triggers = triggers;
    }

    public static String getRawTableName() {
        return rawTableName;
    }

    public TableReference getTableRef() {
        return tableRef;
    }

    public String getTableName() {
        return tableRef.getQualifiedName();
    }

    public Namespace getNamespace() {
        return tableRef.getNamespace();
    }

    /**
     * <pre>
     * HotspottyDataStreamIdxRow {
     *   {@literal Long id};
     * }
     * </pre>
     */
    public static final class HotspottyDataStreamIdxRow implements Persistable, Comparable<HotspottyDataStreamIdxRow> {
        private final long id;

        public static HotspottyDataStreamIdxRow of(long id) {
            return new HotspottyDataStreamIdxRow(id);
        }

        private HotspottyDataStreamIdxRow(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }

        public static Function<HotspottyDataStreamIdxRow, Long> getIdFun() {
            return new Function<HotspottyDataStreamIdxRow, Long>() {
                @Override
                public Long apply(HotspottyDataStreamIdxRow row) {
                    return row.id;
                }
            };
        }

        public static Function<Long, HotspottyDataStreamIdxRow> fromIdFun() {
            return new Function<Long, HotspottyDataStreamIdxRow>() {
                @Override
                public HotspottyDataStreamIdxRow apply(Long row) {
                    return HotspottyDataStreamIdxRow.of(row);
                }
            };
        }

        @Override
        public byte[] persistToBytes() {
            byte[] idBytes = EncodingUtils.encodeSignedVarLong(id);
            return EncodingUtils.add(idBytes);
        }

        public static final Hydrator<HotspottyDataStreamIdxRow> BYTES_HYDRATOR = new Hydrator<HotspottyDataStreamIdxRow>() {
            @Override
            public HotspottyDataStreamIdxRow hydrateFromBytes(byte[] __input) {
                int __index = 0;
                Long id = EncodingUtils.decodeSignedVarLong(__input, __index);
                __index += EncodingUtils.sizeOfSignedVarLong(id);
                return new HotspottyDataStreamIdxRow(id);
            }
        };

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("id", id)
                .toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HotspottyDataStreamIdxRow other = (HotspottyDataStreamIdxRow) obj;
            return Objects.equals(id, other.id);
        }

        @SuppressWarnings("ArrayHashCode")
        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }

        @Override
        public int compareTo(HotspottyDataStreamIdxRow o) {
            return ComparisonChain.start()
                .compare(this.id, o.id)
                .result();
        }
    }

    /**
     * <pre>
     * HotspottyDataStreamIdxColumn {
     *   {@literal byte[] reference};
     * }
     * </pre>
     */
    public static final class HotspottyDataStreamIdxColumn implements Persistable, Comparable<HotspottyDataStreamIdxColumn> {
        private final byte[] reference;

        public static HotspottyDataStreamIdxColumn of(byte[] reference) {
            return new HotspottyDataStreamIdxColumn(reference);
        }

        private HotspottyDataStreamIdxColumn(byte[] reference) {
            this.reference = reference;
        }

        public byte[] getReference() {
            return reference;
        }

        public static Function<HotspottyDataStreamIdxColumn, byte[]> getReferenceFun() {
            return new Function<HotspottyDataStreamIdxColumn, byte[]>() {
                @Override
                public byte[] apply(HotspottyDataStreamIdxColumn row) {
                    return row.reference;
                }
            };
        }

        public static Function<byte[], HotspottyDataStreamIdxColumn> fromReferenceFun() {
            return new Function<byte[], HotspottyDataStreamIdxColumn>() {
                @Override
                public HotspottyDataStreamIdxColumn apply(byte[] row) {
                    return HotspottyDataStreamIdxColumn.of(row);
                }
            };
        }

        @Override
        public byte[] persistToBytes() {
            byte[] referenceBytes = EncodingUtils.encodeSizedBytes(reference);
            return EncodingUtils.add(referenceBytes);
        }

        public static final Hydrator<HotspottyDataStreamIdxColumn> BYTES_HYDRATOR = new Hydrator<HotspottyDataStreamIdxColumn>() {
            @Override
            public HotspottyDataStreamIdxColumn hydrateFromBytes(byte[] __input) {
                int __index = 0;
                byte[] reference = EncodingUtils.decodeSizedBytes(__input, __index);
                __index += EncodingUtils.sizeOfSizedBytes(reference);
                return new HotspottyDataStreamIdxColumn(reference);
            }
        };

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("reference", reference)
                .toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HotspottyDataStreamIdxColumn other = (HotspottyDataStreamIdxColumn) obj;
            return Arrays.equals(reference, other.reference);
        }

        @SuppressWarnings("ArrayHashCode")
        @Override
        public int hashCode() {
            return Objects.hashCode(reference);
        }

        @Override
        public int compareTo(HotspottyDataStreamIdxColumn o) {
            return ComparisonChain.start()
                .compare(this.reference, o.reference, UnsignedBytes.lexicographicalComparator())
                .result();
        }
    }

    public interface HotspottyDataStreamIdxTrigger {
        public void putHotspottyDataStreamIdx(Multimap<HotspottyDataStreamIdxRow, ? extends HotspottyDataStreamIdxColumnValue> newRows);
    }

    /**
     * <pre>
     * Column name description {
     *   {@literal byte[] reference};
     * }
     * Column value description {
     *   type: Long;
     * }
     * </pre>
     */
    public static final class HotspottyDataStreamIdxColumnValue implements ColumnValue<Long> {
        private final HotspottyDataStreamIdxColumn columnName;
        private final Long value;

        public static HotspottyDataStreamIdxColumnValue of(HotspottyDataStreamIdxColumn columnName, Long value) {
            return new HotspottyDataStreamIdxColumnValue(columnName, value);
        }

        private HotspottyDataStreamIdxColumnValue(HotspottyDataStreamIdxColumn columnName, Long value) {
            this.columnName = columnName;
            this.value = value;
        }

        public HotspottyDataStreamIdxColumn getColumnName() {
            return columnName;
        }

        @Override
        public Long getValue() {
            return value;
        }

        @Override
        public byte[] persistColumnName() {
            return columnName.persistToBytes();
        }

        @Override
        public byte[] persistValue() {
            byte[] bytes = EncodingUtils.encodeUnsignedVarLong(value);
            return CompressionUtils.compress(bytes, Compression.NONE);
        }

        public static Long hydrateValue(byte[] bytes) {
            bytes = CompressionUtils.decompress(bytes, Compression.NONE);
            return EncodingUtils.decodeUnsignedVarLong(bytes, 0);
        }

        public static Function<HotspottyDataStreamIdxColumnValue, HotspottyDataStreamIdxColumn> getColumnNameFun() {
            return new Function<HotspottyDataStreamIdxColumnValue, HotspottyDataStreamIdxColumn>() {
                @Override
                public HotspottyDataStreamIdxColumn apply(HotspottyDataStreamIdxColumnValue columnValue) {
                    return columnValue.getColumnName();
                }
            };
        }

        public static Function<HotspottyDataStreamIdxColumnValue, Long> getValueFun() {
            return new Function<HotspottyDataStreamIdxColumnValue, Long>() {
                @Override
                public Long apply(HotspottyDataStreamIdxColumnValue columnValue) {
                    return columnValue.getValue();
                }
            };
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("ColumnName", this.columnName)
                .add("Value", this.value)
                .toString();
        }
    }

    public static final class HotspottyDataStreamIdxRowResult implements TypedRowResult {
        private final HotspottyDataStreamIdxRow rowName;
        private final ImmutableSet<HotspottyDataStreamIdxColumnValue> columnValues;

        public static HotspottyDataStreamIdxRowResult of(RowResult<byte[]> rowResult) {
            HotspottyDataStreamIdxRow rowName = HotspottyDataStreamIdxRow.BYTES_HYDRATOR.hydrateFromBytes(rowResult.getRowName());
            Set<HotspottyDataStreamIdxColumnValue> columnValues = Sets.newHashSetWithExpectedSize(rowResult.getColumns().size());
            for (Entry<byte[], byte[]> e : rowResult.getColumns().entrySet()) {
                HotspottyDataStreamIdxColumn col = HotspottyDataStreamIdxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
                Long value = HotspottyDataStreamIdxColumnValue.hydrateValue(e.getValue());
                columnValues.add(HotspottyDataStreamIdxColumnValue.of(col, value));
            }
            return new HotspottyDataStreamIdxRowResult(rowName, ImmutableSet.copyOf(columnValues));
        }

        private HotspottyDataStreamIdxRowResult(HotspottyDataStreamIdxRow rowName, ImmutableSet<HotspottyDataStreamIdxColumnValue> columnValues) {
            this.rowName = rowName;
            this.columnValues = columnValues;
        }

        @Override
        public HotspottyDataStreamIdxRow getRowName() {
            return rowName;
        }

        public Set<HotspottyDataStreamIdxColumnValue> getColumnValues() {
            return columnValues;
        }

        public static Function<HotspottyDataStreamIdxRowResult, HotspottyDataStreamIdxRow> getRowNameFun() {
            return new Function<HotspottyDataStreamIdxRowResult, HotspottyDataStreamIdxRow>() {
                @Override
                public HotspottyDataStreamIdxRow apply(HotspottyDataStreamIdxRowResult rowResult) {
                    return rowResult.rowName;
                }
            };
        }

        public static Function<HotspottyDataStreamIdxRowResult, ImmutableSet<HotspottyDataStreamIdxColumnValue>> getColumnValuesFun() {
            return new Function<HotspottyDataStreamIdxRowResult, ImmutableSet<HotspottyDataStreamIdxColumnValue>>() {
                @Override
                public ImmutableSet<HotspottyDataStreamIdxColumnValue> apply(HotspottyDataStreamIdxRowResult rowResult) {
                    return rowResult.columnValues;
                }
            };
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("RowName", getRowName())
                .add("ColumnValues", getColumnValues())
                .toString();
        }
    }

    @Override
    public void delete(HotspottyDataStreamIdxRow row, HotspottyDataStreamIdxColumn column) {
        delete(ImmutableMultimap.of(row, column));
    }

    @Override
    public void delete(Iterable<HotspottyDataStreamIdxRow> rows) {
        Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumn> toRemove = HashMultimap.create();
        Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> result = getRowsMultimap(rows);
        for (Entry<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> e : result.entries()) {
            toRemove.put(e.getKey(), e.getValue().getColumnName());
        }
        delete(toRemove);
    }

    @Override
    public void delete(Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumn> values) {
        t.delete(tableRef, ColumnValues.toCells(values));
    }

    @Override
    public void put(HotspottyDataStreamIdxRow rowName, Iterable<HotspottyDataStreamIdxColumnValue> values) {
        put(ImmutableMultimap.<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue>builder().putAll(rowName, values).build());
    }

    @Override
    public void put(HotspottyDataStreamIdxRow rowName, HotspottyDataStreamIdxColumnValue... values) {
        put(ImmutableMultimap.<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue>builder().putAll(rowName, values).build());
    }

    @Override
    public void put(Multimap<HotspottyDataStreamIdxRow, ? extends HotspottyDataStreamIdxColumnValue> values) {
        t.useTable(tableRef, this);
        t.put(tableRef, ColumnValues.toCellValues(values));
        for (HotspottyDataStreamIdxTrigger trigger : triggers) {
            trigger.putHotspottyDataStreamIdx(values);
        }
    }

    @Override
    public void touch(Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumn> values) {
        Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> currentValues = get(values);
        put(currentValues);
        Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumn> toDelete = HashMultimap.create(values);
        for (Map.Entry<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> e : currentValues.entries()) {
            toDelete.remove(e.getKey(), e.getValue().getColumnName());
        }
        delete(toDelete);
    }

    public static ColumnSelection getColumnSelection(Collection<HotspottyDataStreamIdxColumn> cols) {
        return ColumnSelection.create(Collections2.transform(cols, Persistables.persistToBytesFunction()));
    }

    public static ColumnSelection getColumnSelection(HotspottyDataStreamIdxColumn... cols) {
        return getColumnSelection(Arrays.asList(cols));
    }

    @Override
    public Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> get(Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumn> cells) {
        Set<Cell> rawCells = ColumnValues.toCells(cells);
        Map<Cell, byte[]> rawResults = t.get(tableRef, rawCells);
        Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> rowMap = ArrayListMultimap.create();
        for (Entry<Cell, byte[]> e : rawResults.entrySet()) {
            if (e.getValue().length > 0) {
                HotspottyDataStreamIdxRow row = HotspottyDataStreamIdxRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey().getRowName());
                HotspottyDataStreamIdxColumn col = HotspottyDataStreamIdxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey().getColumnName());
                Long val = HotspottyDataStreamIdxColumnValue.hydrateValue(e.getValue());
                rowMap.put(row, HotspottyDataStreamIdxColumnValue.of(col, val));
            }
        }
        return rowMap;
    }

    @Override
    public List<HotspottyDataStreamIdxColumnValue> getRowColumns(HotspottyDataStreamIdxRow row) {
        return getRowColumns(row, allColumns);
    }

    @Override
    public List<HotspottyDataStreamIdxColumnValue> getRowColumns(HotspottyDataStreamIdxRow row, ColumnSelection columns) {
        byte[] bytes = row.persistToBytes();
        RowResult<byte[]> rowResult = t.getRows(tableRef, ImmutableSet.of(bytes), columns).get(bytes);
        if (rowResult == null) {
            return ImmutableList.of();
        } else {
            List<HotspottyDataStreamIdxColumnValue> ret = Lists.newArrayListWithCapacity(rowResult.getColumns().size());
            for (Entry<byte[], byte[]> e : rowResult.getColumns().entrySet()) {
                HotspottyDataStreamIdxColumn col = HotspottyDataStreamIdxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
                Long val = HotspottyDataStreamIdxColumnValue.hydrateValue(e.getValue());
                ret.add(HotspottyDataStreamIdxColumnValue.of(col, val));
            }
            return ret;
        }
    }

    @Override
    public Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> getRowsMultimap(Iterable<HotspottyDataStreamIdxRow> rows) {
        return getRowsMultimapInternal(rows, allColumns);
    }

    @Override
    public Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> getRowsMultimap(Iterable<HotspottyDataStreamIdxRow> rows, ColumnSelection columns) {
        return getRowsMultimapInternal(rows, columns);
    }

    private Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> getRowsMultimapInternal(Iterable<HotspottyDataStreamIdxRow> rows, ColumnSelection columns) {
        SortedMap<byte[], RowResult<byte[]>> results = t.getRows(tableRef, Persistables.persistAll(rows), columns);
        return getRowMapFromRowResults(results.values());
    }

    private static Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> getRowMapFromRowResults(Collection<RowResult<byte[]>> rowResults) {
        Multimap<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue> rowMap = ArrayListMultimap.create();
        for (RowResult<byte[]> result : rowResults) {
            HotspottyDataStreamIdxRow row = HotspottyDataStreamIdxRow.BYTES_HYDRATOR.hydrateFromBytes(result.getRowName());
            for (Entry<byte[], byte[]> e : result.getColumns().entrySet()) {
                HotspottyDataStreamIdxColumn col = HotspottyDataStreamIdxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
                Long val = HotspottyDataStreamIdxColumnValue.hydrateValue(e.getValue());
                rowMap.put(row, HotspottyDataStreamIdxColumnValue.of(col, val));
            }
        }
        return rowMap;
    }

    @Override
    public Map<HotspottyDataStreamIdxRow, BatchingVisitable<HotspottyDataStreamIdxColumnValue>> getRowsColumnRange(Iterable<HotspottyDataStreamIdxRow> rows, BatchColumnRangeSelection columnRangeSelection) {
        Map<byte[], BatchingVisitable<Map.Entry<Cell, byte[]>>> results = t.getRowsColumnRange(tableRef, Persistables.persistAll(rows), columnRangeSelection);
        Map<HotspottyDataStreamIdxRow, BatchingVisitable<HotspottyDataStreamIdxColumnValue>> transformed = Maps.newHashMapWithExpectedSize(results.size());
        for (Entry<byte[], BatchingVisitable<Map.Entry<Cell, byte[]>>> e : results.entrySet()) {
            HotspottyDataStreamIdxRow row = HotspottyDataStreamIdxRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
            BatchingVisitable<HotspottyDataStreamIdxColumnValue> bv = BatchingVisitables.transform(e.getValue(), result -> {
                HotspottyDataStreamIdxColumn col = HotspottyDataStreamIdxColumn.BYTES_HYDRATOR.hydrateFromBytes(result.getKey().getColumnName());
                Long val = HotspottyDataStreamIdxColumnValue.hydrateValue(result.getValue());
                return HotspottyDataStreamIdxColumnValue.of(col, val);
            });
            transformed.put(row, bv);
        }
        return transformed;
    }

    @Override
    public Iterator<Map.Entry<HotspottyDataStreamIdxRow, HotspottyDataStreamIdxColumnValue>> getRowsColumnRange(Iterable<HotspottyDataStreamIdxRow> rows, ColumnRangeSelection columnRangeSelection, int batchHint) {
        Iterator<Map.Entry<Cell, byte[]>> results = t.getRowsColumnRange(getTableRef(), Persistables.persistAll(rows), columnRangeSelection, batchHint);
        return Iterators.transform(results, e -> {
            HotspottyDataStreamIdxRow row = HotspottyDataStreamIdxRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey().getRowName());
            HotspottyDataStreamIdxColumn col = HotspottyDataStreamIdxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey().getColumnName());
            Long val = HotspottyDataStreamIdxColumnValue.hydrateValue(e.getValue());
            HotspottyDataStreamIdxColumnValue colValue = HotspottyDataStreamIdxColumnValue.of(col, val);
            return Maps.immutableEntry(row, colValue);
        });
    }

    @Override
    public Map<HotspottyDataStreamIdxRow, Iterator<HotspottyDataStreamIdxColumnValue>> getRowsColumnRangeIterator(Iterable<HotspottyDataStreamIdxRow> rows, BatchColumnRangeSelection columnRangeSelection) {
        Map<byte[], Iterator<Map.Entry<Cell, byte[]>>> results = t.getRowsColumnRangeIterator(tableRef, Persistables.persistAll(rows), columnRangeSelection);
        Map<HotspottyDataStreamIdxRow, Iterator<HotspottyDataStreamIdxColumnValue>> transformed = Maps.newHashMapWithExpectedSize(results.size());
        for (Entry<byte[], Iterator<Map.Entry<Cell, byte[]>>> e : results.entrySet()) {
            HotspottyDataStreamIdxRow row = HotspottyDataStreamIdxRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
            Iterator<HotspottyDataStreamIdxColumnValue> bv = Iterators.transform(e.getValue(), result -> {
                HotspottyDataStreamIdxColumn col = HotspottyDataStreamIdxColumn.BYTES_HYDRATOR.hydrateFromBytes(result.getKey().getColumnName());
                Long val = HotspottyDataStreamIdxColumnValue.hydrateValue(result.getValue());
                return HotspottyDataStreamIdxColumnValue.of(col, val);
            });
            transformed.put(row, bv);
        }
        return transformed;
    }

    private ColumnSelection optimizeColumnSelection(ColumnSelection columns) {
        if (columns.allColumnsSelected()) {
            return allColumns;
        }
        return columns;
    }

    public BatchingVisitableView<HotspottyDataStreamIdxRowResult> getAllRowsUnordered() {
        return getAllRowsUnordered(allColumns);
    }

    public BatchingVisitableView<HotspottyDataStreamIdxRowResult> getAllRowsUnordered(ColumnSelection columns) {
        return BatchingVisitables.transform(t.getRange(tableRef, RangeRequest.builder()
                .retainColumns(optimizeColumnSelection(columns)).build()),
                new Function<RowResult<byte[]>, HotspottyDataStreamIdxRowResult>() {
            @Override
            public HotspottyDataStreamIdxRowResult apply(RowResult<byte[]> input) {
                return HotspottyDataStreamIdxRowResult.of(input);
            }
        });
    }

    @Override
    public List<String> findConstraintFailures(Map<Cell, byte[]> writes,
                                               ConstraintCheckingTransaction transaction,
                                               AtlasDbConstraintCheckingMode constraintCheckingMode) {
        return ImmutableList.of();
    }

    @Override
    public List<String> findConstraintFailuresNoRead(Map<Cell, byte[]> writes,
                                                     AtlasDbConstraintCheckingMode constraintCheckingMode) {
        return ImmutableList.of();
    }

    /**
     * This exists to avoid unused import warnings
     * {@link AbortingVisitor}
     * {@link AbortingVisitors}
     * {@link ArrayListMultimap}
     * {@link Arrays}
     * {@link AssertUtils}
     * {@link AtlasDbConstraintCheckingMode}
     * {@link AtlasDbDynamicMutablePersistentTable}
     * {@link AtlasDbMutablePersistentTable}
     * {@link AtlasDbNamedMutableTable}
     * {@link AtlasDbNamedPersistentSet}
     * {@link BatchColumnRangeSelection}
     * {@link BatchingVisitable}
     * {@link BatchingVisitableView}
     * {@link BatchingVisitables}
     * {@link BiFunction}
     * {@link Bytes}
     * {@link Callable}
     * {@link Cell}
     * {@link Cells}
     * {@link Collection}
     * {@link Collections2}
     * {@link ColumnRangeSelection}
     * {@link ColumnRangeSelections}
     * {@link ColumnSelection}
     * {@link ColumnValue}
     * {@link ColumnValues}
     * {@link ComparisonChain}
     * {@link Compression}
     * {@link CompressionUtils}
     * {@link ConstraintCheckingTransaction}
     * {@link Descending}
     * {@link EncodingUtils}
     * {@link Entry}
     * {@link EnumSet}
     * {@link Function}
     * {@link Generated}
     * {@link HashMultimap}
     * {@link HashSet}
     * {@link Hashing}
     * {@link Hydrator}
     * {@link ImmutableGetRangesQuery}
     * {@link ImmutableList}
     * {@link ImmutableMap}
     * {@link ImmutableMultimap}
     * {@link ImmutableSet}
     * {@link InvalidProtocolBufferException}
     * {@link IterableView}
     * {@link Iterables}
     * {@link Iterator}
     * {@link Iterators}
     * {@link Joiner}
     * {@link List}
     * {@link Lists}
     * {@link Map}
     * {@link Maps}
     * {@link MoreObjects}
     * {@link Multimap}
     * {@link Multimaps}
     * {@link NamedColumnValue}
     * {@link Namespace}
     * {@link Nullable}
     * {@link Objects}
     * {@link Optional}
     * {@link Persistable}
     * {@link Persistables}
     * {@link Prefix}
     * {@link PtBytes}
     * {@link RangeRequest}
     * {@link RowResult}
     * {@link Set}
     * {@link Sets}
     * {@link Sha256Hash}
     * {@link SortedMap}
     * {@link Stream}
     * {@link Supplier}
     * {@link TableReference}
     * {@link Throwables}
     * {@link TimeUnit}
     * {@link Transaction}
     * {@link TypedRowResult}
     * {@link UUID}
     * {@link UnsignedBytes}
     * {@link ValueType}
     */
    static String __CLASS_HASH = "lWy4z6m1ZmwcuY4d/ez8Rw==";
}
