/*
 * (c) Copyright 2021 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.atlasdb.keyvalue.api.cache;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.palantir.atlasdb.keyvalue.api.AtlasLockDescriptorUtils;
import com.palantir.atlasdb.keyvalue.api.CellReference;
import com.palantir.atlasdb.keyvalue.api.ResilientLockWatchProxy;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.keyvalue.api.watch.Sequence;
import com.palantir.atlasdb.keyvalue.api.watch.StartTimestamp;
import com.palantir.atlasdb.transaction.api.TransactionLockWatchFailedException;
import com.palantir.common.streams.KeyedStream;
import com.palantir.lock.LockDescriptor;
import com.palantir.lock.watch.CommitUpdate;
import com.palantir.lock.watch.LockWatchEvent;
import com.palantir.lock.watch.LockWatchEventCache;
import com.palantir.lock.watch.LockWatchVersion;
import com.palantir.lock.watch.TransactionsLockWatchUpdate;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.logger.SafeLogger;
import com.palantir.logsafe.logger.SafeLoggerFactory;
import com.palantir.util.RateLimitedLogger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class LockWatchValueScopingCacheImpl implements LockWatchValueScopingCache {
    private static final SafeLogger log = SafeLoggerFactory.get(LockWatchValueScopingCacheImpl.class);
    private static final RateLimitedLogger rateLimitedLogger = new RateLimitedLogger(log, (1 / 5.0));
    private static final int MAX_CACHE_COUNT = 20_000;
    private final LockWatchEventCache eventCache;
    private final CacheStore cacheStore;

    @GuardedBy("this")
    private final ValueStore valueStore;

    @GuardedBy("this")
    private final SnapshotStore snapshotStore;

    private volatile Optional<LockWatchVersion> currentVersion = Optional.empty();

    private final CacheMetrics cacheMetrics;

    @VisibleForTesting
    LockWatchValueScopingCacheImpl(
            LockWatchEventCache eventCache,
            long maxCacheSize,
            double validationProbability,
            Set<TableReference> watchedTablesFromSchema,
            SnapshotStore snapshotStore,
            Runnable failureCallback,
            CacheMetrics metrics) {
        this.eventCache = eventCache;
        this.snapshotStore = snapshotStore;
        this.valueStore = new ValueStoreImpl(watchedTablesFromSchema, maxCacheSize, metrics);
        this.cacheMetrics = metrics;
        this.cacheStore =
                new CacheStoreImpl(snapshotStore, validationProbability, failureCallback, metrics, MAX_CACHE_COUNT);
    }

    public static LockWatchValueScopingCache create(
            LockWatchEventCache eventCache,
            CacheMetrics metrics,
            long maxCacheSize,
            double validationProbability,
            Set<TableReference> watchedTablesFromSchema) {
        ResilientLockWatchProxy<LockWatchValueScopingCache> proxyFactory =
                ResilientLockWatchProxy.newValueCacheProxyFactory(NoOpLockWatchValueScopingCache.create(), metrics);
        LockWatchValueScopingCache defaultCache = new LockWatchValueScopingCacheImpl(
                eventCache,
                maxCacheSize,
                validationProbability,
                watchedTablesFromSchema,
                SnapshotStoreImpl.create(metrics),
                proxyFactory::fallback,
                metrics);
        proxyFactory.setDelegate(defaultCache);
        return proxyFactory.newValueCacheProxy();
    }

    @Override
    public synchronized void processStartTransactions(Set<Long> startTimestamps) {
        TransactionsLockWatchUpdate updateForTransactions =
                eventCache.getUpdateForTransactions(startTimestamps, currentVersion);

        Optional<LockWatchVersion> latestVersionFromUpdate = computeMaxUpdateVersion(updateForTransactions);

        if (updateForTransactions.clearCache()) {
            clearCache(updateForTransactions, latestVersionFromUpdate);
        }

        updateStores(updateForTransactions);
        updateCurrentVersion(latestVersionFromUpdate);
    }

    @Override
    public synchronized void updateCacheWithCommitTimestampsInformation(Set<Long> startTimestamps) {
        startTimestamps.forEach(this::processCommitUpdate);
    }

    @Override
    public synchronized void ensureStateRemoved(long startTimestamp) {
        StartTimestamp startTs = StartTimestamp.of(startTimestamp);
        snapshotStore.removeTimestamp(startTs);
        cacheStore.removeCache(startTs);
    }

    @Override
    public void onSuccessfulCommit(long startTimestamp) {
        TransactionScopedCache cache = cacheStore.getCache(StartTimestamp.of(startTimestamp));
        cache.finalise();

        Map<CellReference, CacheValue> cachedValues = cache.getValueDigest().loadedValues();
        if (!cachedValues.isEmpty()) {
            eventCache.getEventUpdate(startTimestamp).accept(new CommitUpdate.Visitor<Void>() {
                @Override
                public Void invalidateAll() {
                    // This might happen due to an election or if we exceeded the maximum number of events held in
                    // memory. Either way, the values are just not pushed to the central cache. If it needs to throw
                    // because of read-write conflicts, that is handled in the PreCommitCondition.
                    return null;
                }

                @Override
                public Void invalidateSome(Set<LockDescriptor> invalidatedLocks) {
                    Set<CellReference> invalidatedCells = invalidatedLocks.stream()
                            .map(AtlasLockDescriptorUtils::candidateCells)
                            .flatMap(List::stream)
                            .collect(ImmutableSet.toImmutableSet());

                    final Map<CellReference, CacheValue> toUpdate;
                    if (invalidatedCells.isEmpty()) {
                        toUpdate = cachedValues;
                    } else {
                        toUpdate = KeyedStream.stream(cachedValues)
                                .filterKeys(cellReference -> !invalidatedCells.contains(cellReference))
                                .collectToMap();
                    }

                    synchronized (LockWatchValueScopingCacheImpl.this) {
                        toUpdate.forEach(valueStore::putValue);
                    }
                    return null;
                }
            });
        }
        ensureStateRemoved(startTimestamp);
    }

    /**
     * Retrieval of transaction scoped caches (read-only or otherwise) does not need to be synchronised. The main
     * reason for this is that the only race condition that could conceivably occur is for the state to not exist here
     * when it should. However, in all of these cases, a no-op cache is used instead, and thus the transaction will
     * simply go ahead but with caching disabled (which is guaranteed to be safe).
     */
    @Override
    public TransactionScopedCache getTransactionScopedCache(long startTs) {
        return cacheStore.getCache(StartTimestamp.of(startTs));
    }

    @Override
    public TransactionScopedCache getReadOnlyTransactionScopedCacheForCommit(long startTs) {
        return cacheStore.getReadOnlyCache(StartTimestamp.of(startTs));
    }

    private synchronized void processCommitUpdate(long startTimestamp) {
        StartTimestamp startTs = StartTimestamp.of(startTimestamp);
        TransactionScopedCache cache = cacheStore.getCache(startTs);
        cache.finalise();
        CommitUpdate commitUpdate = eventCache.getCommitUpdate(startTimestamp);
        cacheStore.createReadOnlyCache(startTs, commitUpdate);
    }

    /**
     * In order to maintain the necessary invariants, we need to do the following:
     * <p>
     *  1. For each new event, we apply it to the cache. The effects of this application is described in
     *     {@link LockWatchValueScopingCache}.
     *  2. For each transaction, we must ensure that we store a snapshot of the cache at the sequence corresponding
     *     to the transaction's start timestamp. Note that not every sequence will have a corresponding timestamp, so we
     *     don't bother storing a snapshot for those sequences. Also note that we know that each call here will only
     *     ever have new events, and that consecutive calls to this method will *always* have increasing sequences
     *     (without this last guarantee, we'd need to store snapshots for all sequences).
     *  3. For each transaction, we must create a transaction scoped cache. We do this now as we have tighter guarantees
     *     around when the cache is created, and thus deleted.
     */
    private synchronized void updateStores(TransactionsLockWatchUpdate updateForTransactions) {
        Multimap<Sequence, StartTimestamp> reversedMap = createSequenceTimestampMultimap(updateForTransactions);

        // Without this block, updates with no events would not store a snapshot.
        Optional<Sequence> maybeSeq =
                currentVersion.map(LockWatchVersion::version).map(Sequence::of);
        if (maybeSeq.isPresent()) {
            synchronized (this) {
                snapshotStore.storeSnapshot(maybeSeq.get(), reversedMap.get(maybeSeq.get()), valueStore.getSnapshot());
            }
        }

        for (LockWatchEvent event : updateForTransactions.events()) {
            if (isNewEvent(event)) {
                valueStore.applyEvent(event);
                Sequence sequence = Sequence.of(event.sequence());
                snapshotStore.storeSnapshot(sequence, reversedMap.get(sequence), valueStore.getSnapshot());
            }
        }

        updateForTransactions.startTsToSequence().keySet().stream()
                .map(StartTimestamp::of)
                .forEach(cacheStore::createCache);

        if (valueStore.getSnapshot().hasAnyTablesWatched()) {
            assertNoSnapshotsMissing(reversedMap);
        }
    }

    private synchronized boolean isNewEvent(LockWatchEvent event) {
        return currentVersion
                .map(LockWatchVersion::version)
                .map(current -> current < event.sequence())
                .orElse(true);
    }

    private synchronized void assertNoSnapshotsMissing(Multimap<Sequence, StartTimestamp> reversedMap) {
        Set<Sequence> sequences = reversedMap.keySet();
        if (sequences.stream().map(snapshotStore::getSnapshotForSequence).anyMatch(Optional::isEmpty)) {
            log.warn(
                    "snapshots were not taken for all sequences; logging additional information",
                    SafeArg.of("numSequences", sequences),
                    SafeArg.of(
                            "firstHundredSequences",
                            sequences.stream().limit(100).collect(Collectors.toSet())),
                    SafeArg.of("currentVersion", currentVersion),
                    SafeArg.of("numTransactions", reversedMap.values().size()));
            throw new TransactionLockWatchFailedException("snapshots were not taken for all sequences; this update "
                    + "must have been lost and is now too old to process. Transactions should be retried.");
        }
    }

    private synchronized void updateCurrentVersion(Optional<LockWatchVersion> maybeUpdateVersion) {
        maybeUpdateVersion
                .filter(this::shouldUpdateVersion)
                .ifPresent(updateVersion -> currentVersion = Optional.of(updateVersion));
    }

    private Optional<LockWatchVersion> computeMaxUpdateVersion(TransactionsLockWatchUpdate updateForTransactions) {
        return updateForTransactions.startTsToSequence().values().stream()
                .max(Comparator.comparingLong(LockWatchVersion::version));
    }

    private synchronized boolean shouldUpdateVersion(LockWatchVersion updateVersion) {
        return currentVersion.isEmpty() || currentVersion.orElseThrow().version() < updateVersion.version();
    }

    private synchronized void clearCache(
            TransactionsLockWatchUpdate updateForTransactions, Optional<LockWatchVersion> latestVersionFromUpdate) {
        LockWatchEvent firstEvent = null;
        LockWatchEvent lastEvent = null;
        List<LockWatchEvent> events = updateForTransactions.events();
        if (!events.isEmpty()) {
            firstEvent = events.get(0);
            lastEvent = events.get(events.size() - 1);
        }

        registerAllClearEventAndMaybeLog(latestVersionFromUpdate, firstEvent, lastEvent);
        valueStore.reset();
        snapshotStore.reset();
        cacheStore.reset();
        currentVersion = Optional.empty();
    }

    private void registerAllClearEventAndMaybeLog(
            Optional<LockWatchVersion> latestVersionFromUpdate, LockWatchEvent firstEvent, LockWatchEvent lastEvent) {
        cacheMetrics.increaseCacheStateAllClear();
        rateLimitedLogger.log(logger -> logger.info(
                "Clearing all value cache state",
                SafeArg.of("currentVersion", currentVersion),
                SafeArg.of("latestUpdateFromUpdate", latestVersionFromUpdate),
                SafeArg.of("firstEventSequence", Optional.ofNullable(firstEvent).map(LockWatchEvent::sequence)),
                SafeArg.of("lastEventSequence", Optional.ofNullable(lastEvent).map(LockWatchEvent::sequence))));
    }

    private static Multimap<Sequence, StartTimestamp> createSequenceTimestampMultimap(
            TransactionsLockWatchUpdate updateForTransactions) {
        return KeyedStream.stream(updateForTransactions.startTsToSequence())
                .mapKeys(StartTimestamp::of)
                .map(LockWatchVersion::version)
                .map(Sequence::of)
                .mapEntries((timestamp, sequence) -> Maps.immutableEntry(sequence, timestamp))
                .collectToSetMultimap();
    }
}
