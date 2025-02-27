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
public final class HotspottyDataStreamHashAidxTable implements
        AtlasDbDynamicMutablePersistentTable<HotspottyDataStreamHashAidxTable.HotspottyDataStreamHashAidxRow,
                                                HotspottyDataStreamHashAidxTable.HotspottyDataStreamHashAidxColumn,
                                                HotspottyDataStreamHashAidxTable.HotspottyDataStreamHashAidxColumnValue,
                                                HotspottyDataStreamHashAidxTable.HotspottyDataStreamHashAidxRowResult> {
    private final Transaction t;
    private final List<HotspottyDataStreamHashAidxTrigger> triggers;
    private final static String rawTableName = "hotspottyData_stream_hash_aidx";
    private final TableReference tableRef;
    private final static ColumnSelection allColumns = ColumnSelection.all();

    static HotspottyDataStreamHashAidxTable of(Transaction t, Namespace namespace) {
        return new HotspottyDataStreamHashAidxTable(t, namespace, ImmutableList.<HotspottyDataStreamHashAidxTrigger>of());
    }

    static HotspottyDataStreamHashAidxTable of(Transaction t, Namespace namespace, HotspottyDataStreamHashAidxTrigger trigger, HotspottyDataStreamHashAidxTrigger... triggers) {
        return new HotspottyDataStreamHashAidxTable(t, namespace, ImmutableList.<HotspottyDataStreamHashAidxTrigger>builder().add(trigger).add(triggers).build());
    }

    static HotspottyDataStreamHashAidxTable of(Transaction t, Namespace namespace, List<HotspottyDataStreamHashAidxTrigger> triggers) {
        return new HotspottyDataStreamHashAidxTable(t, namespace, triggers);
    }

    private HotspottyDataStreamHashAidxTable(Transaction t, Namespace namespace, List<HotspottyDataStreamHashAidxTrigger> triggers) {
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
     * HotspottyDataStreamHashAidxRow {
     *   {@literal Sha256Hash hash};
     * }
     * </pre>
     */
    public static final class HotspottyDataStreamHashAidxRow implements Persistable, Comparable<HotspottyDataStreamHashAidxRow> {
        private final Sha256Hash hash;

        public static HotspottyDataStreamHashAidxRow of(Sha256Hash hash) {
            return new HotspottyDataStreamHashAidxRow(hash);
        }

        private HotspottyDataStreamHashAidxRow(Sha256Hash hash) {
            this.hash = hash;
        }

        public Sha256Hash getHash() {
            return hash;
        }

        public static Function<HotspottyDataStreamHashAidxRow, Sha256Hash> getHashFun() {
            return new Function<HotspottyDataStreamHashAidxRow, Sha256Hash>() {
                @Override
                public Sha256Hash apply(HotspottyDataStreamHashAidxRow row) {
                    return row.hash;
                }
            };
        }

        public static Function<Sha256Hash, HotspottyDataStreamHashAidxRow> fromHashFun() {
            return new Function<Sha256Hash, HotspottyDataStreamHashAidxRow>() {
                @Override
                public HotspottyDataStreamHashAidxRow apply(Sha256Hash row) {
                    return HotspottyDataStreamHashAidxRow.of(row);
                }
            };
        }

        @Override
        public byte[] persistToBytes() {
            byte[] hashBytes = hash.getBytes();
            return EncodingUtils.add(hashBytes);
        }

        public static final Hydrator<HotspottyDataStreamHashAidxRow> BYTES_HYDRATOR = new Hydrator<HotspottyDataStreamHashAidxRow>() {
            @Override
            public HotspottyDataStreamHashAidxRow hydrateFromBytes(byte[] __input) {
                int __index = 0;
                Sha256Hash hash = new Sha256Hash(EncodingUtils.get32Bytes(__input, __index));
                __index += 32;
                return new HotspottyDataStreamHashAidxRow(hash);
            }
        };

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("hash", hash)
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
            HotspottyDataStreamHashAidxRow other = (HotspottyDataStreamHashAidxRow) obj;
            return Objects.equals(hash, other.hash);
        }

        @SuppressWarnings("ArrayHashCode")
        @Override
        public int hashCode() {
            return Objects.hashCode(hash);
        }

        @Override
        public int compareTo(HotspottyDataStreamHashAidxRow o) {
            return ComparisonChain.start()
                .compare(this.hash, o.hash)
                .result();
        }
    }

    /**
     * <pre>
     * HotspottyDataStreamHashAidxColumn {
     *   {@literal Long streamId};
     * }
     * </pre>
     */
    public static final class HotspottyDataStreamHashAidxColumn implements Persistable, Comparable<HotspottyDataStreamHashAidxColumn> {
        private final long streamId;

        public static HotspottyDataStreamHashAidxColumn of(long streamId) {
            return new HotspottyDataStreamHashAidxColumn(streamId);
        }

        private HotspottyDataStreamHashAidxColumn(long streamId) {
            this.streamId = streamId;
        }

        public long getStreamId() {
            return streamId;
        }

        public static Function<HotspottyDataStreamHashAidxColumn, Long> getStreamIdFun() {
            return new Function<HotspottyDataStreamHashAidxColumn, Long>() {
                @Override
                public Long apply(HotspottyDataStreamHashAidxColumn row) {
                    return row.streamId;
                }
            };
        }

        public static Function<Long, HotspottyDataStreamHashAidxColumn> fromStreamIdFun() {
            return new Function<Long, HotspottyDataStreamHashAidxColumn>() {
                @Override
                public HotspottyDataStreamHashAidxColumn apply(Long row) {
                    return HotspottyDataStreamHashAidxColumn.of(row);
                }
            };
        }

        @Override
        public byte[] persistToBytes() {
            byte[] streamIdBytes = EncodingUtils.encodeSignedVarLong(streamId);
            return EncodingUtils.add(streamIdBytes);
        }

        public static final Hydrator<HotspottyDataStreamHashAidxColumn> BYTES_HYDRATOR = new Hydrator<HotspottyDataStreamHashAidxColumn>() {
            @Override
            public HotspottyDataStreamHashAidxColumn hydrateFromBytes(byte[] __input) {
                int __index = 0;
                Long streamId = EncodingUtils.decodeSignedVarLong(__input, __index);
                __index += EncodingUtils.sizeOfSignedVarLong(streamId);
                return new HotspottyDataStreamHashAidxColumn(streamId);
            }
        };

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("streamId", streamId)
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
            HotspottyDataStreamHashAidxColumn other = (HotspottyDataStreamHashAidxColumn) obj;
            return Objects.equals(streamId, other.streamId);
        }

        @SuppressWarnings("ArrayHashCode")
        @Override
        public int hashCode() {
            return Objects.hashCode(streamId);
        }

        @Override
        public int compareTo(HotspottyDataStreamHashAidxColumn o) {
            return ComparisonChain.start()
                .compare(this.streamId, o.streamId)
                .result();
        }
    }

    public interface HotspottyDataStreamHashAidxTrigger {
        public void putHotspottyDataStreamHashAidx(Multimap<HotspottyDataStreamHashAidxRow, ? extends HotspottyDataStreamHashAidxColumnValue> newRows);
    }

    /**
     * <pre>
     * Column name description {
     *   {@literal Long streamId};
     * }
     * Column value description {
     *   type: Long;
     * }
     * </pre>
     */
    public static final class HotspottyDataStreamHashAidxColumnValue implements ColumnValue<Long> {
        private final HotspottyDataStreamHashAidxColumn columnName;
        private final Long value;

        public static HotspottyDataStreamHashAidxColumnValue of(HotspottyDataStreamHashAidxColumn columnName, Long value) {
            return new HotspottyDataStreamHashAidxColumnValue(columnName, value);
        }

        private HotspottyDataStreamHashAidxColumnValue(HotspottyDataStreamHashAidxColumn columnName, Long value) {
            this.columnName = columnName;
            this.value = value;
        }

        public HotspottyDataStreamHashAidxColumn getColumnName() {
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

        public static Function<HotspottyDataStreamHashAidxColumnValue, HotspottyDataStreamHashAidxColumn> getColumnNameFun() {
            return new Function<HotspottyDataStreamHashAidxColumnValue, HotspottyDataStreamHashAidxColumn>() {
                @Override
                public HotspottyDataStreamHashAidxColumn apply(HotspottyDataStreamHashAidxColumnValue columnValue) {
                    return columnValue.getColumnName();
                }
            };
        }

        public static Function<HotspottyDataStreamHashAidxColumnValue, Long> getValueFun() {
            return new Function<HotspottyDataStreamHashAidxColumnValue, Long>() {
                @Override
                public Long apply(HotspottyDataStreamHashAidxColumnValue columnValue) {
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

    public static final class HotspottyDataStreamHashAidxRowResult implements TypedRowResult {
        private final HotspottyDataStreamHashAidxRow rowName;
        private final ImmutableSet<HotspottyDataStreamHashAidxColumnValue> columnValues;

        public static HotspottyDataStreamHashAidxRowResult of(RowResult<byte[]> rowResult) {
            HotspottyDataStreamHashAidxRow rowName = HotspottyDataStreamHashAidxRow.BYTES_HYDRATOR.hydrateFromBytes(rowResult.getRowName());
            Set<HotspottyDataStreamHashAidxColumnValue> columnValues = Sets.newHashSetWithExpectedSize(rowResult.getColumns().size());
            for (Entry<byte[], byte[]> e : rowResult.getColumns().entrySet()) {
                HotspottyDataStreamHashAidxColumn col = HotspottyDataStreamHashAidxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
                Long value = HotspottyDataStreamHashAidxColumnValue.hydrateValue(e.getValue());
                columnValues.add(HotspottyDataStreamHashAidxColumnValue.of(col, value));
            }
            return new HotspottyDataStreamHashAidxRowResult(rowName, ImmutableSet.copyOf(columnValues));
        }

        private HotspottyDataStreamHashAidxRowResult(HotspottyDataStreamHashAidxRow rowName, ImmutableSet<HotspottyDataStreamHashAidxColumnValue> columnValues) {
            this.rowName = rowName;
            this.columnValues = columnValues;
        }

        @Override
        public HotspottyDataStreamHashAidxRow getRowName() {
            return rowName;
        }

        public Set<HotspottyDataStreamHashAidxColumnValue> getColumnValues() {
            return columnValues;
        }

        public static Function<HotspottyDataStreamHashAidxRowResult, HotspottyDataStreamHashAidxRow> getRowNameFun() {
            return new Function<HotspottyDataStreamHashAidxRowResult, HotspottyDataStreamHashAidxRow>() {
                @Override
                public HotspottyDataStreamHashAidxRow apply(HotspottyDataStreamHashAidxRowResult rowResult) {
                    return rowResult.rowName;
                }
            };
        }

        public static Function<HotspottyDataStreamHashAidxRowResult, ImmutableSet<HotspottyDataStreamHashAidxColumnValue>> getColumnValuesFun() {
            return new Function<HotspottyDataStreamHashAidxRowResult, ImmutableSet<HotspottyDataStreamHashAidxColumnValue>>() {
                @Override
                public ImmutableSet<HotspottyDataStreamHashAidxColumnValue> apply(HotspottyDataStreamHashAidxRowResult rowResult) {
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
    public void delete(HotspottyDataStreamHashAidxRow row, HotspottyDataStreamHashAidxColumn column) {
        delete(ImmutableMultimap.of(row, column));
    }

    @Override
    public void delete(Iterable<HotspottyDataStreamHashAidxRow> rows) {
        Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumn> toRemove = HashMultimap.create();
        Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> result = getRowsMultimap(rows);
        for (Entry<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> e : result.entries()) {
            toRemove.put(e.getKey(), e.getValue().getColumnName());
        }
        delete(toRemove);
    }

    @Override
    public void delete(Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumn> values) {
        t.delete(tableRef, ColumnValues.toCells(values));
    }

    @Override
    public void put(HotspottyDataStreamHashAidxRow rowName, Iterable<HotspottyDataStreamHashAidxColumnValue> values) {
        put(ImmutableMultimap.<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue>builder().putAll(rowName, values).build());
    }

    @Override
    public void put(HotspottyDataStreamHashAidxRow rowName, HotspottyDataStreamHashAidxColumnValue... values) {
        put(ImmutableMultimap.<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue>builder().putAll(rowName, values).build());
    }

    @Override
    public void put(Multimap<HotspottyDataStreamHashAidxRow, ? extends HotspottyDataStreamHashAidxColumnValue> values) {
        t.useTable(tableRef, this);
        t.put(tableRef, ColumnValues.toCellValues(values));
        for (HotspottyDataStreamHashAidxTrigger trigger : triggers) {
            trigger.putHotspottyDataStreamHashAidx(values);
        }
    }

    @Override
    public void touch(Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumn> values) {
        Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> currentValues = get(values);
        put(currentValues);
        Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumn> toDelete = HashMultimap.create(values);
        for (Map.Entry<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> e : currentValues.entries()) {
            toDelete.remove(e.getKey(), e.getValue().getColumnName());
        }
        delete(toDelete);
    }

    public static ColumnSelection getColumnSelection(Collection<HotspottyDataStreamHashAidxColumn> cols) {
        return ColumnSelection.create(Collections2.transform(cols, Persistables.persistToBytesFunction()));
    }

    public static ColumnSelection getColumnSelection(HotspottyDataStreamHashAidxColumn... cols) {
        return getColumnSelection(Arrays.asList(cols));
    }

    @Override
    public Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> get(Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumn> cells) {
        Set<Cell> rawCells = ColumnValues.toCells(cells);
        Map<Cell, byte[]> rawResults = t.get(tableRef, rawCells);
        Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> rowMap = ArrayListMultimap.create();
        for (Entry<Cell, byte[]> e : rawResults.entrySet()) {
            if (e.getValue().length > 0) {
                HotspottyDataStreamHashAidxRow row = HotspottyDataStreamHashAidxRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey().getRowName());
                HotspottyDataStreamHashAidxColumn col = HotspottyDataStreamHashAidxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey().getColumnName());
                Long val = HotspottyDataStreamHashAidxColumnValue.hydrateValue(e.getValue());
                rowMap.put(row, HotspottyDataStreamHashAidxColumnValue.of(col, val));
            }
        }
        return rowMap;
    }

    @Override
    public List<HotspottyDataStreamHashAidxColumnValue> getRowColumns(HotspottyDataStreamHashAidxRow row) {
        return getRowColumns(row, allColumns);
    }

    @Override
    public List<HotspottyDataStreamHashAidxColumnValue> getRowColumns(HotspottyDataStreamHashAidxRow row, ColumnSelection columns) {
        byte[] bytes = row.persistToBytes();
        RowResult<byte[]> rowResult = t.getRows(tableRef, ImmutableSet.of(bytes), columns).get(bytes);
        if (rowResult == null) {
            return ImmutableList.of();
        } else {
            List<HotspottyDataStreamHashAidxColumnValue> ret = Lists.newArrayListWithCapacity(rowResult.getColumns().size());
            for (Entry<byte[], byte[]> e : rowResult.getColumns().entrySet()) {
                HotspottyDataStreamHashAidxColumn col = HotspottyDataStreamHashAidxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
                Long val = HotspottyDataStreamHashAidxColumnValue.hydrateValue(e.getValue());
                ret.add(HotspottyDataStreamHashAidxColumnValue.of(col, val));
            }
            return ret;
        }
    }

    @Override
    public Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> getRowsMultimap(Iterable<HotspottyDataStreamHashAidxRow> rows) {
        return getRowsMultimapInternal(rows, allColumns);
    }

    @Override
    public Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> getRowsMultimap(Iterable<HotspottyDataStreamHashAidxRow> rows, ColumnSelection columns) {
        return getRowsMultimapInternal(rows, columns);
    }

    private Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> getRowsMultimapInternal(Iterable<HotspottyDataStreamHashAidxRow> rows, ColumnSelection columns) {
        SortedMap<byte[], RowResult<byte[]>> results = t.getRows(tableRef, Persistables.persistAll(rows), columns);
        return getRowMapFromRowResults(results.values());
    }

    private static Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> getRowMapFromRowResults(Collection<RowResult<byte[]>> rowResults) {
        Multimap<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue> rowMap = ArrayListMultimap.create();
        for (RowResult<byte[]> result : rowResults) {
            HotspottyDataStreamHashAidxRow row = HotspottyDataStreamHashAidxRow.BYTES_HYDRATOR.hydrateFromBytes(result.getRowName());
            for (Entry<byte[], byte[]> e : result.getColumns().entrySet()) {
                HotspottyDataStreamHashAidxColumn col = HotspottyDataStreamHashAidxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
                Long val = HotspottyDataStreamHashAidxColumnValue.hydrateValue(e.getValue());
                rowMap.put(row, HotspottyDataStreamHashAidxColumnValue.of(col, val));
            }
        }
        return rowMap;
    }

    @Override
    public Map<HotspottyDataStreamHashAidxRow, BatchingVisitable<HotspottyDataStreamHashAidxColumnValue>> getRowsColumnRange(Iterable<HotspottyDataStreamHashAidxRow> rows, BatchColumnRangeSelection columnRangeSelection) {
        Map<byte[], BatchingVisitable<Map.Entry<Cell, byte[]>>> results = t.getRowsColumnRange(tableRef, Persistables.persistAll(rows), columnRangeSelection);
        Map<HotspottyDataStreamHashAidxRow, BatchingVisitable<HotspottyDataStreamHashAidxColumnValue>> transformed = Maps.newHashMapWithExpectedSize(results.size());
        for (Entry<byte[], BatchingVisitable<Map.Entry<Cell, byte[]>>> e : results.entrySet()) {
            HotspottyDataStreamHashAidxRow row = HotspottyDataStreamHashAidxRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
            BatchingVisitable<HotspottyDataStreamHashAidxColumnValue> bv = BatchingVisitables.transform(e.getValue(), result -> {
                HotspottyDataStreamHashAidxColumn col = HotspottyDataStreamHashAidxColumn.BYTES_HYDRATOR.hydrateFromBytes(result.getKey().getColumnName());
                Long val = HotspottyDataStreamHashAidxColumnValue.hydrateValue(result.getValue());
                return HotspottyDataStreamHashAidxColumnValue.of(col, val);
            });
            transformed.put(row, bv);
        }
        return transformed;
    }

    @Override
    public Iterator<Map.Entry<HotspottyDataStreamHashAidxRow, HotspottyDataStreamHashAidxColumnValue>> getRowsColumnRange(Iterable<HotspottyDataStreamHashAidxRow> rows, ColumnRangeSelection columnRangeSelection, int batchHint) {
        Iterator<Map.Entry<Cell, byte[]>> results = t.getRowsColumnRange(getTableRef(), Persistables.persistAll(rows), columnRangeSelection, batchHint);
        return Iterators.transform(results, e -> {
            HotspottyDataStreamHashAidxRow row = HotspottyDataStreamHashAidxRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey().getRowName());
            HotspottyDataStreamHashAidxColumn col = HotspottyDataStreamHashAidxColumn.BYTES_HYDRATOR.hydrateFromBytes(e.getKey().getColumnName());
            Long val = HotspottyDataStreamHashAidxColumnValue.hydrateValue(e.getValue());
            HotspottyDataStreamHashAidxColumnValue colValue = HotspottyDataStreamHashAidxColumnValue.of(col, val);
            return Maps.immutableEntry(row, colValue);
        });
    }

    @Override
    public Map<HotspottyDataStreamHashAidxRow, Iterator<HotspottyDataStreamHashAidxColumnValue>> getRowsColumnRangeIterator(Iterable<HotspottyDataStreamHashAidxRow> rows, BatchColumnRangeSelection columnRangeSelection) {
        Map<byte[], Iterator<Map.Entry<Cell, byte[]>>> results = t.getRowsColumnRangeIterator(tableRef, Persistables.persistAll(rows), columnRangeSelection);
        Map<HotspottyDataStreamHashAidxRow, Iterator<HotspottyDataStreamHashAidxColumnValue>> transformed = Maps.newHashMapWithExpectedSize(results.size());
        for (Entry<byte[], Iterator<Map.Entry<Cell, byte[]>>> e : results.entrySet()) {
            HotspottyDataStreamHashAidxRow row = HotspottyDataStreamHashAidxRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
            Iterator<HotspottyDataStreamHashAidxColumnValue> bv = Iterators.transform(e.getValue(), result -> {
                HotspottyDataStreamHashAidxColumn col = HotspottyDataStreamHashAidxColumn.BYTES_HYDRATOR.hydrateFromBytes(result.getKey().getColumnName());
                Long val = HotspottyDataStreamHashAidxColumnValue.hydrateValue(result.getValue());
                return HotspottyDataStreamHashAidxColumnValue.of(col, val);
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

    public BatchingVisitableView<HotspottyDataStreamHashAidxRowResult> getAllRowsUnordered() {
        return getAllRowsUnordered(allColumns);
    }

    public BatchingVisitableView<HotspottyDataStreamHashAidxRowResult> getAllRowsUnordered(ColumnSelection columns) {
        return BatchingVisitables.transform(t.getRange(tableRef, RangeRequest.builder()
                .retainColumns(optimizeColumnSelection(columns)).build()),
                new Function<RowResult<byte[]>, HotspottyDataStreamHashAidxRowResult>() {
            @Override
            public HotspottyDataStreamHashAidxRowResult apply(RowResult<byte[]> input) {
                return HotspottyDataStreamHashAidxRowResult.of(input);
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
    static String __CLASS_HASH = "NOiRKvLXsadPhJ4N/aae6Q==";
}
