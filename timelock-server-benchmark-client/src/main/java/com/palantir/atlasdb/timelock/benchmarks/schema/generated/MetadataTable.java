package com.palantir.atlasdb.timelock.benchmarks.schema.generated;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Generated;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
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
@SuppressWarnings("all")
public final class MetadataTable implements
        AtlasDbMutablePersistentTable<MetadataTable.MetadataRow,
                                         MetadataTable.MetadataNamedColumnValue<?>,
                                         MetadataTable.MetadataRowResult>,
        AtlasDbNamedMutableTable<MetadataTable.MetadataRow,
                                    MetadataTable.MetadataNamedColumnValue<?>,
                                    MetadataTable.MetadataRowResult> {
    private final Transaction t;
    private final List<MetadataTrigger> triggers;
    private final static String rawTableName = "Metadata";
    private final TableReference tableRef;
    private final static ColumnSelection allColumns = getColumnSelection(MetadataNamedColumn.values());

    static MetadataTable of(Transaction t, Namespace namespace) {
        return new MetadataTable(t, namespace, ImmutableList.<MetadataTrigger>of());
    }

    static MetadataTable of(Transaction t, Namespace namespace, MetadataTrigger trigger, MetadataTrigger... triggers) {
        return new MetadataTable(t, namespace, ImmutableList.<MetadataTrigger>builder().add(trigger).add(triggers).build());
    }

    static MetadataTable of(Transaction t, Namespace namespace, List<MetadataTrigger> triggers) {
        return new MetadataTable(t, namespace, triggers);
    }

    private MetadataTable(Transaction t, Namespace namespace, List<MetadataTrigger> triggers) {
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
     * MetadataRow {
     *   {@literal Long hashOfRowComponents};
     *   {@literal String key};
     * }
     * </pre>
     */
    public static final class MetadataRow implements Persistable, Comparable<MetadataRow> {
        private final long hashOfRowComponents;
        private final String key;

        public static MetadataRow of(String key) {
            long hashOfRowComponents = computeHashFirstComponents(key);
            return new MetadataRow(hashOfRowComponents, key);
        }

        private MetadataRow(long hashOfRowComponents, String key) {
            this.hashOfRowComponents = hashOfRowComponents;
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public static Function<MetadataRow, String> getKeyFun() {
            return new Function<MetadataRow, String>() {
                @Override
                public String apply(MetadataRow row) {
                    return row.key;
                }
            };
        }

        public static Function<String, MetadataRow> fromKeyFun() {
            return new Function<String, MetadataRow>() {
                @Override
                public MetadataRow apply(String row) {
                    return MetadataRow.of(row);
                }
            };
        }

        @Override
        public byte[] persistToBytes() {
            byte[] hashOfRowComponentsBytes = PtBytes.toBytes(Long.MIN_VALUE ^ hashOfRowComponents);
            byte[] keyBytes = EncodingUtils.encodeVarString(key);
            return EncodingUtils.add(hashOfRowComponentsBytes, keyBytes);
        }

        public static final Hydrator<MetadataRow> BYTES_HYDRATOR = new Hydrator<MetadataRow>() {
            @Override
            public MetadataRow hydrateFromBytes(byte[] __input) {
                int __index = 0;
                Long hashOfRowComponents = Long.MIN_VALUE ^ PtBytes.toLong(__input, __index);
                __index += 8;
                String key = EncodingUtils.decodeVarString(__input, __index);
                __index += EncodingUtils.sizeOfVarString(key);
                return new MetadataRow(hashOfRowComponents, key);
            }
        };

        public static long computeHashFirstComponents(String key) {
            byte[] keyBytes = EncodingUtils.encodeVarString(key);
            return Hashing.murmur3_128().hashBytes(EncodingUtils.add(keyBytes)).asLong();
        }

        public static RangeRequest.Builder createPrefixRangeUnsorted(String key) {
            long hashOfRowComponents = computeHashFirstComponents(key);
            byte[] hashOfRowComponentsBytes = PtBytes.toBytes(Long.MIN_VALUE ^ hashOfRowComponents);
            byte[] keyBytes = EncodingUtils.encodeVarString(key);
            return RangeRequest.builder().prefixRange(EncodingUtils.add(hashOfRowComponentsBytes, keyBytes));
        }

        public static Prefix prefixUnsorted(String key) {
            long hashOfRowComponents = computeHashFirstComponents(key);
            byte[] hashOfRowComponentsBytes = PtBytes.toBytes(Long.MIN_VALUE ^ hashOfRowComponents);
            byte[] keyBytes = EncodingUtils.encodeVarString(key);
            return new Prefix(EncodingUtils.add(hashOfRowComponentsBytes, keyBytes));
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("hashOfRowComponents", hashOfRowComponents)
                .add("key", key)
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
            MetadataRow other = (MetadataRow) obj;
            return Objects.equals(hashOfRowComponents, other.hashOfRowComponents) && Objects.equals(key, other.key);
        }

        @SuppressWarnings("ArrayHashCode")
        @Override
        public int hashCode() {
            return Arrays.deepHashCode(new Object[]{ hashOfRowComponents, key });
        }

        @Override
        public int compareTo(MetadataRow o) {
            return ComparisonChain.start()
                .compare(this.hashOfRowComponents, o.hashOfRowComponents)
                .compare(this.key, o.key)
                .result();
        }
    }

    public interface MetadataNamedColumnValue<T> extends NamedColumnValue<T> { /* */ }

    /**
     * <pre>
     * Column value description {
     *   type: byte[];
     * }
     * </pre>
     */
    public static final class Data implements MetadataNamedColumnValue<byte[]> {
        private final byte[] value;

        public static Data of(byte[] value) {
            return new Data(value);
        }

        private Data(byte[] value) {
            this.value = value;
        }

        @Override
        public String getColumnName() {
            return "data";
        }

        @Override
        public String getShortColumnName() {
            return "d";
        }

        @Override
        public byte[] getValue() {
            return value;
        }

        @Override
        public byte[] persistValue() {
            byte[] bytes = value;
            return CompressionUtils.compress(bytes, Compression.NONE);
        }

        @Override
        public byte[] persistColumnName() {
            return PtBytes.toCachedBytes("d");
        }

        public static final Hydrator<Data> BYTES_HYDRATOR = new Hydrator<Data>() {
            @Override
            public Data hydrateFromBytes(byte[] bytes) {
                bytes = CompressionUtils.decompress(bytes, Compression.NONE);
                return of(EncodingUtils.getBytesFromOffsetToEnd(bytes, 0));
            }
        };

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("Value", this.value)
                .toString();
        }
    }

    public interface MetadataTrigger {
        public void putMetadata(Multimap<MetadataRow, ? extends MetadataNamedColumnValue<?>> newRows);
    }

    public static final class MetadataRowResult implements TypedRowResult {
        private final RowResult<byte[]> row;

        public static MetadataRowResult of(RowResult<byte[]> row) {
            return new MetadataRowResult(row);
        }

        private MetadataRowResult(RowResult<byte[]> row) {
            this.row = row;
        }

        @Override
        public MetadataRow getRowName() {
            return MetadataRow.BYTES_HYDRATOR.hydrateFromBytes(row.getRowName());
        }

        public static Function<MetadataRowResult, MetadataRow> getRowNameFun() {
            return new Function<MetadataRowResult, MetadataRow>() {
                @Override
                public MetadataRow apply(MetadataRowResult rowResult) {
                    return rowResult.getRowName();
                }
            };
        }

        public static Function<RowResult<byte[]>, MetadataRowResult> fromRawRowResultFun() {
            return new Function<RowResult<byte[]>, MetadataRowResult>() {
                @Override
                public MetadataRowResult apply(RowResult<byte[]> rowResult) {
                    return new MetadataRowResult(rowResult);
                }
            };
        }

        public boolean hasData() {
            return row.getColumns().containsKey(PtBytes.toCachedBytes("d"));
        }

        public byte[] getData() {
            byte[] bytes = row.getColumns().get(PtBytes.toCachedBytes("d"));
            if (bytes == null) {
                return null;
            }
            Data value = Data.BYTES_HYDRATOR.hydrateFromBytes(bytes);
            return value.getValue();
        }

        public static Function<MetadataRowResult, byte[]> getDataFun() {
            return new Function<MetadataRowResult, byte[]>() {
                @Override
                public byte[] apply(MetadataRowResult rowResult) {
                    return rowResult.getData();
                }
            };
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass().getSimpleName())
                .add("RowName", getRowName())
                .add("Data", getData())
                .toString();
        }
    }

    public enum MetadataNamedColumn {
        DATA {
            @Override
            public byte[] getShortName() {
                return PtBytes.toCachedBytes("d");
            }
        };

        public abstract byte[] getShortName();

        public static Function<MetadataNamedColumn, byte[]> toShortName() {
            return new Function<MetadataNamedColumn, byte[]>() {
                @Override
                public byte[] apply(MetadataNamedColumn namedColumn) {
                    return namedColumn.getShortName();
                }
            };
        }
    }

    public static ColumnSelection getColumnSelection(Collection<MetadataNamedColumn> cols) {
        return ColumnSelection.create(Collections2.transform(cols, MetadataNamedColumn.toShortName()));
    }

    public static ColumnSelection getColumnSelection(MetadataNamedColumn... cols) {
        return getColumnSelection(Arrays.asList(cols));
    }

    private static final Map<String, Hydrator<? extends MetadataNamedColumnValue<?>>> shortNameToHydrator =
            ImmutableMap.<String, Hydrator<? extends MetadataNamedColumnValue<?>>>builder()
                .put("d", Data.BYTES_HYDRATOR)
                .build();

    public Map<MetadataRow, byte[]> getDatas(Collection<MetadataRow> rows) {
        Map<Cell, MetadataRow> cells = Maps.newHashMapWithExpectedSize(rows.size());
        for (MetadataRow row : rows) {
            cells.put(Cell.create(row.persistToBytes(), PtBytes.toCachedBytes("d")), row);
        }
        Map<Cell, byte[]> results = t.get(tableRef, cells.keySet());
        Map<MetadataRow, byte[]> ret = Maps.newHashMapWithExpectedSize(results.size());
        for (Entry<Cell, byte[]> e : results.entrySet()) {
            byte[] val = Data.BYTES_HYDRATOR.hydrateFromBytes(e.getValue()).getValue();
            ret.put(cells.get(e.getKey()), val);
        }
        return ret;
    }

    public void putData(MetadataRow row, byte[] value) {
        put(ImmutableMultimap.of(row, Data.of(value)));
    }

    public void putData(Map<MetadataRow, byte[]> map) {
        Map<MetadataRow, MetadataNamedColumnValue<?>> toPut = Maps.newHashMapWithExpectedSize(map.size());
        for (Entry<MetadataRow, byte[]> e : map.entrySet()) {
            toPut.put(e.getKey(), Data.of(e.getValue()));
        }
        put(Multimaps.forMap(toPut));
    }

    @Override
    public void put(Multimap<MetadataRow, ? extends MetadataNamedColumnValue<?>> rows) {
        t.useTable(tableRef, this);
        t.put(tableRef, ColumnValues.toCellValues(rows));
        for (MetadataTrigger trigger : triggers) {
            trigger.putMetadata(rows);
        }
    }

    public void deleteData(MetadataRow row) {
        deleteData(ImmutableSet.of(row));
    }

    public void deleteData(Iterable<MetadataRow> rows) {
        byte[] col = PtBytes.toCachedBytes("d");
        Set<Cell> cells = Cells.cellsWithConstantColumn(Persistables.persistAll(rows), col);
        t.delete(tableRef, cells);
    }

    @Override
    public void delete(MetadataRow row) {
        delete(ImmutableSet.of(row));
    }

    @Override
    public void delete(Iterable<MetadataRow> rows) {
        List<byte[]> rowBytes = Persistables.persistAll(rows);
        Set<Cell> cells = Sets.newHashSetWithExpectedSize(rowBytes.size());
        cells.addAll(Cells.cellsWithConstantColumn(rowBytes, PtBytes.toCachedBytes("d")));
        t.delete(tableRef, cells);
    }

    public Optional<MetadataRowResult> getRow(MetadataRow row) {
        return getRow(row, allColumns);
    }

    public Optional<MetadataRowResult> getRow(MetadataRow row, ColumnSelection columns) {
        byte[] bytes = row.persistToBytes();
        RowResult<byte[]> rowResult = t.getRows(tableRef, ImmutableSet.of(bytes), columns).get(bytes);
        if (rowResult == null) {
            return Optional.empty();
        } else {
            return Optional.of(MetadataRowResult.of(rowResult));
        }
    }

    @Override
    public List<MetadataRowResult> getRows(Iterable<MetadataRow> rows) {
        return getRows(rows, allColumns);
    }

    @Override
    public List<MetadataRowResult> getRows(Iterable<MetadataRow> rows, ColumnSelection columns) {
        SortedMap<byte[], RowResult<byte[]>> results = t.getRows(tableRef, Persistables.persistAll(rows), columns);
        List<MetadataRowResult> rowResults = Lists.newArrayListWithCapacity(results.size());
        for (RowResult<byte[]> row : results.values()) {
            rowResults.add(MetadataRowResult.of(row));
        }
        return rowResults;
    }

    @Override
    public List<MetadataNamedColumnValue<?>> getRowColumns(MetadataRow row) {
        return getRowColumns(row, allColumns);
    }

    @Override
    public List<MetadataNamedColumnValue<?>> getRowColumns(MetadataRow row, ColumnSelection columns) {
        byte[] bytes = row.persistToBytes();
        RowResult<byte[]> rowResult = t.getRows(tableRef, ImmutableSet.of(bytes), columns).get(bytes);
        if (rowResult == null) {
            return ImmutableList.of();
        } else {
            List<MetadataNamedColumnValue<?>> ret = Lists.newArrayListWithCapacity(rowResult.getColumns().size());
            for (Entry<byte[], byte[]> e : rowResult.getColumns().entrySet()) {
                ret.add(shortNameToHydrator.get(PtBytes.toString(e.getKey())).hydrateFromBytes(e.getValue()));
            }
            return ret;
        }
    }

    @Override
    public Multimap<MetadataRow, MetadataNamedColumnValue<?>> getRowsMultimap(Iterable<MetadataRow> rows) {
        return getRowsMultimapInternal(rows, allColumns);
    }

    @Override
    public Multimap<MetadataRow, MetadataNamedColumnValue<?>> getRowsMultimap(Iterable<MetadataRow> rows, ColumnSelection columns) {
        return getRowsMultimapInternal(rows, columns);
    }

    private Multimap<MetadataRow, MetadataNamedColumnValue<?>> getRowsMultimapInternal(Iterable<MetadataRow> rows, ColumnSelection columns) {
        SortedMap<byte[], RowResult<byte[]>> results = t.getRows(tableRef, Persistables.persistAll(rows), columns);
        return getRowMapFromRowResults(results.values());
    }

    private static Multimap<MetadataRow, MetadataNamedColumnValue<?>> getRowMapFromRowResults(Collection<RowResult<byte[]>> rowResults) {
        Multimap<MetadataRow, MetadataNamedColumnValue<?>> rowMap = HashMultimap.create();
        for (RowResult<byte[]> result : rowResults) {
            MetadataRow row = MetadataRow.BYTES_HYDRATOR.hydrateFromBytes(result.getRowName());
            for (Entry<byte[], byte[]> e : result.getColumns().entrySet()) {
                rowMap.put(row, shortNameToHydrator.get(PtBytes.toString(e.getKey())).hydrateFromBytes(e.getValue()));
            }
        }
        return rowMap;
    }

    @Override
    public Map<MetadataRow, BatchingVisitable<MetadataNamedColumnValue<?>>> getRowsColumnRange(Iterable<MetadataRow> rows, BatchColumnRangeSelection columnRangeSelection) {
        Map<byte[], BatchingVisitable<Map.Entry<Cell, byte[]>>> results = t.getRowsColumnRange(tableRef, Persistables.persistAll(rows), columnRangeSelection);
        Map<MetadataRow, BatchingVisitable<MetadataNamedColumnValue<?>>> transformed = Maps.newHashMapWithExpectedSize(results.size());
        for (Entry<byte[], BatchingVisitable<Map.Entry<Cell, byte[]>>> e : results.entrySet()) {
            MetadataRow row = MetadataRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
            BatchingVisitable<MetadataNamedColumnValue<?>> bv = BatchingVisitables.transform(e.getValue(), result -> {
                return shortNameToHydrator.get(PtBytes.toString(result.getKey().getColumnName())).hydrateFromBytes(result.getValue());
            });
            transformed.put(row, bv);
        }
        return transformed;
    }

    @Override
    public Iterator<Map.Entry<MetadataRow, MetadataNamedColumnValue<?>>> getRowsColumnRange(Iterable<MetadataRow> rows, ColumnRangeSelection columnRangeSelection, int batchHint) {
        Iterator<Map.Entry<Cell, byte[]>> results = t.getRowsColumnRange(getTableRef(), Persistables.persistAll(rows), columnRangeSelection, batchHint);
        return Iterators.transform(results, e -> {
            MetadataRow row = MetadataRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey().getRowName());
            MetadataNamedColumnValue<?> colValue = shortNameToHydrator.get(PtBytes.toString(e.getKey().getColumnName())).hydrateFromBytes(e.getValue());
            return Maps.immutableEntry(row, colValue);
        });
    }

    @Override
    public Map<MetadataRow, Iterator<MetadataNamedColumnValue<?>>> getRowsColumnRangeIterator(Iterable<MetadataRow> rows, BatchColumnRangeSelection columnRangeSelection) {
        Map<byte[], Iterator<Map.Entry<Cell, byte[]>>> results = t.getRowsColumnRangeIterator(tableRef, Persistables.persistAll(rows), columnRangeSelection);
        Map<MetadataRow, Iterator<MetadataNamedColumnValue<?>>> transformed = Maps.newHashMapWithExpectedSize(results.size());
        for (Entry<byte[], Iterator<Map.Entry<Cell, byte[]>>> e : results.entrySet()) {
            MetadataRow row = MetadataRow.BYTES_HYDRATOR.hydrateFromBytes(e.getKey());
            Iterator<MetadataNamedColumnValue<?>> bv = Iterators.transform(e.getValue(), result -> {
                return shortNameToHydrator.get(PtBytes.toString(result.getKey().getColumnName())).hydrateFromBytes(result.getValue());
            });
            transformed.put(row, bv);
        }
        return transformed;
    }

    public BatchingVisitableView<MetadataRowResult> getRange(RangeRequest range) {
        if (range.getColumnNames().isEmpty()) {
            range = range.getBuilder().retainColumns(allColumns).build();
        }
        return BatchingVisitables.transform(t.getRange(tableRef, range), new Function<RowResult<byte[]>, MetadataRowResult>() {
            @Override
            public MetadataRowResult apply(RowResult<byte[]> input) {
                return MetadataRowResult.of(input);
            }
        });
    }

    @Deprecated
    public IterableView<BatchingVisitable<MetadataRowResult>> getRanges(Iterable<RangeRequest> ranges) {
        Iterable<BatchingVisitable<RowResult<byte[]>>> rangeResults = t.getRanges(tableRef, ranges);
        return IterableView.of(rangeResults).transform(
                new Function<BatchingVisitable<RowResult<byte[]>>, BatchingVisitable<MetadataRowResult>>() {
            @Override
            public BatchingVisitable<MetadataRowResult> apply(BatchingVisitable<RowResult<byte[]>> visitable) {
                return BatchingVisitables.transform(visitable, new Function<RowResult<byte[]>, MetadataRowResult>() {
                    @Override
                    public MetadataRowResult apply(RowResult<byte[]> row) {
                        return MetadataRowResult.of(row);
                    }
                });
            }
        });
    }

    public <T> Stream<T> getRanges(Iterable<RangeRequest> ranges,
                                   int concurrencyLevel,
                                   BiFunction<RangeRequest, BatchingVisitable<MetadataRowResult>, T> visitableProcessor) {
        return t.getRanges(tableRef, ranges, concurrencyLevel,
                (rangeRequest, visitable) -> visitableProcessor.apply(rangeRequest, BatchingVisitables.transform(visitable, MetadataRowResult::of)));
    }

    public <T> Stream<T> getRanges(Iterable<RangeRequest> ranges,
                                   BiFunction<RangeRequest, BatchingVisitable<MetadataRowResult>, T> visitableProcessor) {
        return t.getRanges(tableRef, ranges,
                (rangeRequest, visitable) -> visitableProcessor.apply(rangeRequest, BatchingVisitables.transform(visitable, MetadataRowResult::of)));
    }

    public Stream<BatchingVisitable<MetadataRowResult>> getRangesLazy(Iterable<RangeRequest> ranges) {
        Stream<BatchingVisitable<RowResult<byte[]>>> rangeResults = t.getRangesLazy(tableRef, ranges);
        return rangeResults.map(visitable -> BatchingVisitables.transform(visitable, MetadataRowResult::of));
    }

    public void deleteRange(RangeRequest range) {
        deleteRanges(ImmutableSet.of(range));
    }

    public void deleteRanges(Iterable<RangeRequest> ranges) {
        BatchingVisitables.concat(getRanges(ranges))
                          .transform(MetadataRowResult.getRowNameFun())
                          .batchAccept(1000, new AbortingVisitor<List<MetadataRow>, RuntimeException>() {
            @Override
            public boolean visit(List<MetadataRow> rows) {
                delete(rows);
                return true;
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
    static String __CLASS_HASH = "UZuTxEuqfi5EFgXzP/IndA==";
}
