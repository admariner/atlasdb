/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.history;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.palantir.common.streams.KeyedStream;
import com.palantir.history.models.CompletePaxosHistoryForNamespaceAndUseCase;
import com.palantir.history.models.ImmutableCompletePaxosHistoryForNamespaceAndUseCase;
import com.palantir.history.models.ImmutableLearnedAndAcceptedValue;
import com.palantir.history.models.LearnedAndAcceptedValue;
import com.palantir.history.models.PaxosHistoryOnSingleNode;
import com.palantir.history.sqlite.LogVerificationProgressState;
import com.palantir.history.sqlite.SqlitePaxosStateLogHistory;
import com.palantir.history.util.UseCaseUtils;
import com.palantir.paxos.ImmutableNamespaceAndUseCase;
import com.palantir.paxos.NamespaceAndUseCase;
import com.palantir.timelock.history.HistoryQuery;
import com.palantir.timelock.history.LogsForNamespaceAndUseCase;
import com.palantir.timelock.history.PaxosLogWithAcceptedAndLearnedValues;
import com.palantir.timelock.history.TimeLockPaxosHistoryProvider;
import com.palantir.tokens.auth.AuthHeader;

public class PaxosLogHistoryProvider {
    private static final Logger log = LoggerFactory.getLogger(PaxosLogHistoryProvider.class);

    private static final AuthHeader AUTH_HEADER = AuthHeader.valueOf("Bearer omitted");

    private final LogVerificationProgressState logVerificationProgressState;
    private final LocalHistoryLoader localHistoryLoader;
    private final SqlitePaxosStateLogHistory sqlitePaxosStateLogHistory;
    private final List<TimeLockPaxosHistoryProvider> remoteHistoryProviders;
    private Map<NamespaceAndUseCase, Long> verificationProgressStateCache = new ConcurrentHashMap<>();


    public PaxosLogHistoryProvider(DataSource dataSource, List<TimeLockPaxosHistoryProvider> remoteHistoryProviders) {
        this.remoteHistoryProviders = remoteHistoryProviders;
        this.sqlitePaxosStateLogHistory = SqlitePaxosStateLogHistory.create(dataSource);
        this.logVerificationProgressState = LogVerificationProgressState.create(dataSource);
        this.localHistoryLoader = LocalHistoryLoader.create(this.sqlitePaxosStateLogHistory);
    }

    private Set<NamespaceAndUseCase> getNamespaceAndUseCaseTuples() {
        return sqlitePaxosStateLogHistory.getAllNamespaceAndUseCaseTuples()
                .stream()
                .map(namespaceAndUseCase -> ImmutableNamespaceAndUseCase.of(
                        namespaceAndUseCase.namespace(),
                        UseCaseUtils.getPaxosUseCasePrefix(namespaceAndUseCase.useCase())))
                .collect(Collectors.toSet());
    }

    private Long getOrInsertVerificationState(NamespaceAndUseCase namespaceAndUseCase) {
        return logVerificationProgressState.getLastVerifiedSeq(
                namespaceAndUseCase.namespace(), namespaceAndUseCase.useCase());
    }

    /**
     * Gets history from all the nodes for all unique (namespace, useCase) tuples since last verified sequence numbers
     * i.e. the highest sequence number that was verified since the last time the bounds were reset.
     *
     * @throws  Exception if fails to fetch history from all remote servers
     */
    public List<CompletePaxosHistoryForNamespaceAndUseCase> getHistory() {

        /**
         * map of all unique (namespace, useCase) tuples to their respective last verified sequence numbers.
         */
        Map<NamespaceAndUseCase, Long> lastVerifiedSequences = getNamespaceAndUseCaseToLastVerifiedSeqMap();
        PaxosHistoryOnSingleNode localPaxosHistory = localHistoryLoader.getLocalPaxosHistory(lastVerifiedSequences);

        /**
         * The history queries are built from the lastVerifiedSequences map above,
         * required for conjure endpoints to load remote history
         */
        List<HistoryQuery> historyQueries = getHistoryQueryListForRemoteServers(lastVerifiedSequences);

        /**
         * List of logs from all remotes
         */
        List<List<LogsForNamespaceAndUseCase>> rawHistoryFromAllRemotes = getHistoriesFromRemoteServers(historyQueries);

        /**
         * List of history from all remotes. Each history is map of namespaceAndUseCase to Paxos logs. The Paxos logs
         * are mapped against sequence numbers.
         */
        List<Map<NamespaceAndUseCase, Map<Long, LearnedAndAcceptedValue>>> historyFromAllRemotes
                = buildHistoryFromRemoteResponses(rawHistoryFromAllRemotes);

        /**
         * Consolidate and build complete history for each (namespace, useCase) pair
         * from histories loaded from local and remote servers
         */
        return consolidateAndGetHistoriesAcrossAllNodes(
                lastVerifiedSequences,
                localPaxosHistory,
                historyFromAllRemotes);
    }

    private List<CompletePaxosHistoryForNamespaceAndUseCase> consolidateAndGetHistoriesAcrossAllNodes(
            Map<NamespaceAndUseCase, Long> lastVerifiedSequences,
            PaxosHistoryOnSingleNode localPaxosHistory,
            List<Map<NamespaceAndUseCase, Map<Long, LearnedAndAcceptedValue>>> historyFromAllRemotes) {
        return lastVerifiedSequences.keySet().stream()
                .map(namespaceAndUseCase -> buildCompleteHistory(
                        namespaceAndUseCase,
                        localPaxosHistory,
                        historyFromAllRemotes))
                .collect(Collectors.toList());
    }

    private List<Map<NamespaceAndUseCase, Map<Long, LearnedAndAcceptedValue>>> buildHistoryFromRemoteResponses(
            List<List<LogsForNamespaceAndUseCase>> rawHistoryFromAllRemotes) {
        return rawHistoryFromAllRemotes.stream()
        .map(this::buildHistoryFromRemoteResponse)
        .collect(Collectors.toList());
    }

    private List<List<LogsForNamespaceAndUseCase>> getHistoriesFromRemoteServers(List<HistoryQuery> historyQueries) {
        return remoteHistoryProviders.stream().map(
                remote -> fetchHistoryFromRemote(historyQueries, remote)).collect(Collectors.toList());
    }

    private List<HistoryQuery> getHistoryQueryListForRemoteServers(
            Map<NamespaceAndUseCase, Long> lastVerifiedSequences) {
        return KeyedStream.stream(lastVerifiedSequences)
                .mapEntries(this::buildHistoryQuery)
                .values()
                .collect(Collectors.toList());
    }

    private Map<NamespaceAndUseCase, Long> getNamespaceAndUseCaseToLastVerifiedSeqMap() {
        return KeyedStream
                .of(getNamespaceAndUseCaseTuples().stream())
                .map(namespaceAndUseCase -> verificationProgressStateCache.computeIfAbsent(namespaceAndUseCase,
                        this::getOrInsertVerificationState))
                .collectToMap();
    }

    private CompletePaxosHistoryForNamespaceAndUseCase buildCompleteHistory(NamespaceAndUseCase namespaceAndUseCase,
            PaxosHistoryOnSingleNode localPaxosHistory,
            List<Map<NamespaceAndUseCase, Map<Long, LearnedAndAcceptedValue>>> historyLogsFromRemotes) {

        /**
         * Rather than having two maps - one for learner records and one for acceptor records,
         * we now have a sequence number mapped to pair of (learnedValue, acceptedValue).
         */
        Map<Long, LearnedAndAcceptedValue> consolidatedLocalRecord
                = localPaxosHistory.getConsolidatedLocalAndRemoteRecord(namespaceAndUseCase);

        /**
         * Retrieve history logs of this (namespace, useCase) pair from history logs fetched from remotes.
         */
        List<Map<Long, LearnedAndAcceptedValue>> remoteHistoryLogsForNamespaceAndUseCase
                = extractRemoteHistoryLogsForNamespaceAndUseCase(namespaceAndUseCase, historyLogsFromRemotes);

        ImmutableList<Map<Long, LearnedAndAcceptedValue>> historyLogsAcrossAllNodes
                = combineLocalAndRemoteHistoryLogs(consolidatedLocalRecord, remoteHistoryLogsForNamespaceAndUseCase);

        /**
         * Paxos history for namespace, useCase pair across all nodes in the cluster.
         */
        return ImmutableCompletePaxosHistoryForNamespaceAndUseCase.of(
                namespaceAndUseCase.namespace(),
                namespaceAndUseCase.useCase(),
                historyLogsAcrossAllNodes);
    }

    private List<Map<Long, LearnedAndAcceptedValue>> extractRemoteHistoryLogsForNamespaceAndUseCase(
            NamespaceAndUseCase namespaceAndUseCase,
            List<Map<NamespaceAndUseCase, Map<Long, LearnedAndAcceptedValue>>> historyLogsFromRemotes) {
        return historyLogsFromRemotes
                .stream()
                .map(history -> history.getOrDefault(namespaceAndUseCase, ImmutableMap.of()))
                .collect(Collectors.toList());
    }

    private ImmutableList<Map<Long, LearnedAndAcceptedValue>> combineLocalAndRemoteHistoryLogs(
            Map<Long, LearnedAndAcceptedValue> consolidatedLocalRecord,
            List<Map<Long, LearnedAndAcceptedValue>> remoteHistoryLogsForNamespaceAndUseCase) {
        return ImmutableList
                .<Map<Long, LearnedAndAcceptedValue>>builder()
                .addAll(remoteHistoryLogsForNamespaceAndUseCase)
                .add(consolidatedLocalRecord)
                .build();
    }

    private Map<NamespaceAndUseCase, Map<Long, LearnedAndAcceptedValue>> buildHistoryFromRemoteResponse(
            List<LogsForNamespaceAndUseCase> logsForNamespaceAndUseCases) {
        /**
         * Build sequence number wise mapped history for each (namespace, useCase) pair from history logs
         * provided by remote
         */
        Map<NamespaceAndUseCase, List<PaxosLogWithAcceptedAndLearnedValues>> namespaceWisePaxosLogs
                = logsForNamespaceAndUseCases.stream().collect(Collectors
                .toMap(LogsForNamespaceAndUseCase::getNamespaceAndUseCase, LogsForNamespaceAndUseCase::getLogs));

        return KeyedStream.stream(namespaceWisePaxosLogs).map(this::mapLogsAgainstSequenceNumbers).collectToMap();
    }

    private Map<Long, LearnedAndAcceptedValue> mapLogsAgainstSequenceNumbers(NamespaceAndUseCase unused,
            List<PaxosLogWithAcceptedAndLearnedValues> logs) {
        return logs.stream()
                .collect(Collectors
                .toMap(PaxosLogWithAcceptedAndLearnedValues::getSeq,
                        log -> ImmutableLearnedAndAcceptedValue.of(log.getPaxosValue(), log.getAcceptedState())));
    }

    private List<LogsForNamespaceAndUseCase> fetchHistoryFromRemote(List<HistoryQuery> historyQueries,
            TimeLockPaxosHistoryProvider remote) {
        try {
            return remote.getPaxosHistory(AUTH_HEADER, historyQueries);
        } catch (Exception exception) {
            log.warn("The remote failed to provide the history,"
                    + "we cannot perform corruption checks without history from all nodes.", exception);
            throw exception;
        }
    }

    private Map.Entry<NamespaceAndUseCase, HistoryQuery> buildHistoryQuery(
            NamespaceAndUseCase namespaceAndUseCase, Long seq) {
        return Maps.immutableEntry(namespaceAndUseCase, HistoryQuery.of(namespaceAndUseCase, seq));
    }
}
