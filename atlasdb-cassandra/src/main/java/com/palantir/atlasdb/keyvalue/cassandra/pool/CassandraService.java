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
package com.palantir.atlasdb.keyvalue.cassandra.pool;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.Sets;
import com.palantir.atlasdb.cassandra.CassandraKeyValueServiceConfig;
import com.palantir.atlasdb.cassandra.CassandraKeyValueServiceRuntimeConfig;
import com.palantir.atlasdb.cassandra.CassandraServersConfigs.ThriftHostsExtractingVisitor;
import com.palantir.atlasdb.keyvalue.cassandra.Blacklist;
import com.palantir.atlasdb.keyvalue.cassandra.CassandraClient;
import com.palantir.atlasdb.keyvalue.cassandra.CassandraClientPoolingContainer;
import com.palantir.atlasdb.keyvalue.cassandra.CassandraLogHelper;
import com.palantir.atlasdb.keyvalue.cassandra.CassandraServerOrigin;
import com.palantir.atlasdb.keyvalue.cassandra.CassandraUtils;
import com.palantir.atlasdb.keyvalue.cassandra.LightweightOppToken;
import com.palantir.atlasdb.util.MetricsManager;
import com.palantir.common.base.FunctionCheckedException;
import com.palantir.common.base.Throwables;
import com.palantir.common.exception.AtlasDbDependencyException;
import com.palantir.common.pooling.PoolingContainer;
import com.palantir.common.streams.KeyedStream;
import com.palantir.logsafe.Arg;
import com.palantir.logsafe.Safe;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.SafeLoggable;
import com.palantir.logsafe.UnsafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalStateException;
import com.palantir.logsafe.logger.SafeLogger;
import com.palantir.logsafe.logger.SafeLoggerFactory;
import com.palantir.refreshable.Refreshable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PrimitiveIterator.OfInt;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.cassandra.thrift.EndpointDetails;
import org.apache.cassandra.thrift.TokenRange;

public class CassandraService implements AutoCloseable {
    private static final SafeLogger log = SafeLoggerFactory.get(CassandraService.class);
    private static final Interner<ImmutableRangeMap<LightweightOppToken, ImmutableSet<CassandraServer>>>
            tokensInterner = Interners.newWeakInterner();

    private final MetricsManager metricsManager;
    private final CassandraKeyValueServiceConfig config;
    private final Blacklist blacklist;
    private final CassandraClientPoolMetrics poolMetrics;
    private final Refreshable<CassandraKeyValueServiceRuntimeConfig> runtimeConfig;

    private volatile ImmutableRangeMap<LightweightOppToken, ImmutableSet<CassandraServer>> tokenMap =
            ImmutableRangeMap.of();
    private final Map<CassandraServer, CassandraClientPoolingContainer> currentPools = new ConcurrentHashMap<>();
    private volatile ImmutableMap<CassandraServer, String> hostToDatacenter = ImmutableMap.of();

    private List<CassandraServer> cassandraHosts;

    private volatile ImmutableSet<CassandraServer> localHosts = ImmutableSet.of();
    private final Supplier<Optional<HostLocation>> myLocationSupplier;
    private final Supplier<Map<String, String>> hostnameByIpSupplier;

    private final Random random = new Random();

    public CassandraService(
            MetricsManager metricsManager,
            CassandraKeyValueServiceConfig config,
            Refreshable<CassandraKeyValueServiceRuntimeConfig> runtimeConfig,
            Blacklist blacklist,
            CassandraClientPoolMetrics poolMetrics) {
        this.metricsManager = metricsManager;
        this.config = config;
        this.runtimeConfig = runtimeConfig;
        this.myLocationSupplier =
                AsyncSupplier.create(HostLocationSupplier.create(this::getSnitch, config.overrideHostLocation()));
        this.blacklist = blacklist;
        this.poolMetrics = poolMetrics;

        Supplier<Map<String, String>> hostnamesByIpSupplier =
                new HostnamesByIpSupplier(this::getAllNonBlacklistedHosts);
        this.hostnameByIpSupplier = Suppliers.memoizeWithExpiration(hostnamesByIpSupplier::get, 1, TimeUnit.MINUTES);
    }

    @Override
    public void close() {}

    public ImmutableMap<CassandraServer, CassandraServerOrigin> refreshTokenRangesAndGetServers() {
        // explicitly not using immutable builders to deduplicate nodes
        Set<CassandraServer> servers = new HashSet<>();
        Map<CassandraServer, String> hostToDatacentersThisRefresh = new HashMap<>();

        try {
            ImmutableRangeMap.Builder<LightweightOppToken, ImmutableSet<CassandraServer>> newTokenRing =
                    ImmutableRangeMap.builder();

            // grab latest token ring view from a random node in the cluster and update local hosts
            List<TokenRange> tokenRanges = getTokenRanges();
            localHosts = refreshLocalHosts(tokenRanges);

            // RangeMap needs a little help with weird 1-node, 1-vnode, this-entire-feature-is-useless case
            if (tokenRanges.size() == 1) {
                EndpointDetails onlyEndpoint = Iterables.getOnlyElement(
                        Iterables.getOnlyElement(tokenRanges).getEndpoint_details());
                CassandraServer onlyHost = getAddressForHost(onlyEndpoint.getHost());
                newTokenRing.put(Range.all(), ImmutableSet.of(onlyHost));
                servers.add(onlyHost);
                hostToDatacentersThisRefresh.put(onlyHost, onlyEndpoint.getDatacenter());
            } else { // normal case, large cluster with many vnodes
                for (TokenRange tokenRange : tokenRanges) {
                    List<EndpointDetails> endpointDetails = tokenRange.getEndpoint_details();
                    ImmutableSet.Builder<CassandraServer> hostsBuilder =
                            ImmutableSet.builderWithExpectedSize(endpointDetails.size());
                    for (EndpointDetails endpoint : endpointDetails) {
                        CassandraServer cassandraServer = getAddressForHostThrowUnchecked(endpoint.getHost());
                        hostToDatacentersThisRefresh.put(cassandraServer, endpoint.getDatacenter());
                        hostsBuilder.add(cassandraServer);
                        servers.add(cassandraServer);
                    }

                    ImmutableSet<CassandraServer> hosts = hostsBuilder.build();
                    LightweightOppToken startToken = LightweightOppToken.fromHex(tokenRange.getStart_token());
                    LightweightOppToken endToken = LightweightOppToken.fromHex(tokenRange.getEnd_token());
                    if (startToken.compareTo(endToken) <= 0) {
                        newTokenRing.put(Range.openClosed(startToken, endToken), hosts);
                    } else {
                        // Handle wrap-around
                        newTokenRing.put(Range.greaterThan(startToken), hosts);
                        newTokenRing.put(Range.atMost(endToken), hosts);
                    }
                }
            }
            tokenMap = tokensInterner.intern(newTokenRing.build());
            hostToDatacenter = ImmutableMap.copyOf(hostToDatacentersThisRefresh);
            logHostToDatacenterMapping(hostToDatacenter);
            return CassandraServerOrigin.mapAllServersToOrigin(servers, CassandraServerOrigin.TOKEN_RANGE);
        } catch (Exception e) {
            log.info(
                    "Couldn't grab new token ranges for token aware cassandra mapping. We will retry in {} seconds.",
                    SafeArg.of("poolRefreshIntervalSeconds", config.poolRefreshIntervalSeconds()),
                    e);

            // Attempt to re-resolve addresses from the configuration; this is important owing to certain race
            // conditions where the entire pool becomes invalid between refreshes.
            ImmutableMap<CassandraServer, CassandraServerOrigin> resolvedConfigAddresses =
                    getCurrentServerListFromConfig();

            ImmutableMap<CassandraServer, CassandraServerOrigin> lastKnownAddresses =
                    CassandraServerOrigin.mapAllServersToOrigin(
                            tokenMap.asMapOfRanges().values().stream().flatMap(Collection::stream),
                            CassandraServerOrigin.LAST_KNOWN);

            return ImmutableMap.<CassandraServer, CassandraServerOrigin>builder()
                    .putAll(lastKnownAddresses)
                    .putAll(resolvedConfigAddresses)
                    .buildKeepingLast();
        }
    }

    /**
     * It is expected that config provides list of servers that are directly reachable and do not require special IP
     * resolution.
     * */
    public ImmutableMap<CassandraServer, CassandraServerOrigin> getCurrentServerListFromConfig() {
        return CassandraServerOrigin.mapAllServersToOrigin(
                getServersSocketAddressesFromConfig().stream()
                        .map(cassandraHost -> CassandraServer.of(cassandraHost.getHostString(), cassandraHost)),
                CassandraServerOrigin.CONFIG);
    }

    private ImmutableSet<InetSocketAddress> getServersSocketAddressesFromConfig() {
        return runtimeConfig.get().servers().accept(ThriftHostsExtractingVisitor.INSTANCE);
    }

    private void logHostToDatacenterMapping(Map<CassandraServer, String> hostToDatacentersThisRefresh) {
        if (log.isDebugEnabled()) {
            Map<String, String> hostAddressToDatacenter = KeyedStream.stream(hostToDatacentersThisRefresh)
                    .mapKeys(CassandraServer::cassandraHostName)
                    .collectToMap();
            log.debug(
                    "Logging host -> datacenter mapping following a refresh",
                    SafeArg.of("hostAddressToDatacenter", hostAddressToDatacenter));
        }
    }

    private List<TokenRange> getTokenRanges() throws Exception {
        return getRandomGoodHost().runWithPooledResource(CassandraUtils.getDescribeRing(config));
    }

    private String getSnitch() {
        try {
            return getRandomGoodHost()
                    .runWithPooledResource((FunctionCheckedException<CassandraClient, String, Exception>)
                            CassandraClient::describe_snitch);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ImmutableSet<CassandraServer> refreshLocalHosts(List<TokenRange> tokenRanges) {
        Optional<HostLocation> myLocation = myLocationSupplier.get();

        if (myLocation.isEmpty()) {
            return ImmutableSet.of();
        }

        ImmutableSet<CassandraServer> newLocalHosts = tokenRanges.stream()
                .map(TokenRange::getEndpoint_details)
                .flatMap(Collection::stream)
                .filter(details -> isHostLocal(details, myLocation.get()))
                .map(EndpointDetails::getHost)
                .map(this::getAddressForHostThrowUnchecked)
                .collect(ImmutableSet.toImmutableSet());

        if (newLocalHosts.isEmpty()) {
            log.warn("No local hosts found");
        }

        return newLocalHosts;
    }

    private static boolean isHostLocal(EndpointDetails details, HostLocation myLocation) {
        return details.isSetDatacenter()
                && details.isSetRack()
                && details.isSetHost()
                && myLocation.isProbablySameRackAs(details.getDatacenter(), details.getRack());
    }

    @VisibleForTesting
    void setLocalHosts(ImmutableSet<CassandraServer> localHosts) {
        this.localHosts = localHosts;
    }

    public Set<CassandraServer> getLocalHosts() {
        return localHosts;
    }

    private CassandraServer getAddressForHostThrowUnchecked(String host) {
        try {
            return getAddressForHost(host);
        } catch (UnknownHostException e) {
            throw Throwables.rewrapAndThrowUncheckedException(e);
        }
    }

    @VisibleForTesting
    CassandraServer getAddressForHost(String inputHost) throws UnknownHostException {
        Map<String, String> hostnamesByIp = hostnameByIpSupplier.get();
        String cassandraHostName = hostnamesByIp.getOrDefault(inputHost, inputHost);
        InetAddress[] resolvedHosts = InetAddress.getAllByName(cassandraHostName);
        int knownPort = getKnownPort();

        // It is okay to have reachable proxies that do not have a hostname
        ImmutableSet.Builder<InetSocketAddress> proxies = ImmutableSet.builderWithExpectedSize(resolvedHosts.length);
        for (InetAddress host : resolvedHosts) {
            proxies.add(new InetSocketAddress(host, knownPort));
        }
        return CassandraServer.of(cassandraHostName, proxies.build());
    }

    private int getKnownPort() throws UnknownHostException {
        // Avoid allocations and intermediate collection as this is on hot path
        return only(Stream.concat(
                        currentPools.keySet().stream().map(CassandraServer::proxy),
                        getServersSocketAddressesFromConfig().stream())
                .mapToInt(InetSocketAddress::getPort));
    }

    @VisibleForTesting
    static int only(IntStream intStream) throws UnknownHostException {
        OfInt iterator = intStream.iterator();
        if (!iterator.hasNext()) {
            throw new UnknownHostException("No single known port");
        }
        int port = iterator.next();
        while (iterator.hasNext()) {
            if (port != iterator.next()) {
                throw new UnknownHostException("No single known port");
            }
        }
        return port;
    }

    private ImmutableSet<CassandraServer> getHostsFor(byte[] key) {
        return tokenMap.get(new LightweightOppToken(key));
    }

    public Optional<CassandraClientPoolingContainer> getRandomGoodHostForPredicate(
            Predicate<CassandraServer> predicate) {
        return getRandomGoodHostForPredicate(predicate, ImmutableSet.of());
    }

    public Optional<CassandraClientPoolingContainer> getRandomGoodHostForPredicate(
            Predicate<CassandraServer> predicate, Set<CassandraServer> triedNodes) {
        ImmutableSet<CassandraServer> hostsMatchingPredicate =
                currentPools.keySet().stream().filter(predicate).collect(ImmutableSet.toImmutableSet());
        ImmutableMap<CassandraServer, String> hostToDatacenterSnapshot = hostToDatacenter; // volatile read
        Map<String, Long> triedDatacenters = triedNodes.stream()
                .map(hostToDatacenterSnapshot::get)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Optional<Long> maximumAttemptsPerDatacenter =
                triedDatacenters.values().stream().max(Long::compareTo);
        Set<String> maximallyAttemptedDatacenters = KeyedStream.stream(triedDatacenters)
                .filter(attempts -> Objects.equals(
                        attempts,
                        maximumAttemptsPerDatacenter.orElseThrow(() -> new SafeIllegalStateException(
                                "Unexpectedly could not find the max attempts per datacenter"))))
                .keys()
                .collect(Collectors.toSet());

        ImmutableSet<CassandraServer> hostsInPermittedDatacenters = hostsMatchingPredicate.stream()
                .filter(pool -> {
                    String datacenter = hostToDatacenterSnapshot.get(pool);
                    return datacenter == null || !maximallyAttemptedDatacenters.contains(datacenter);
                })
                .collect(ImmutableSet.toImmutableSet());
        ImmutableSet<CassandraServer> filteredHosts =
                hostsInPermittedDatacenters.isEmpty() ? hostsMatchingPredicate : hostsInPermittedDatacenters;

        if (filteredHosts.isEmpty()) {
            log.info("No hosts match the provided predicate.");
            return Optional.empty();
        }

        Set<CassandraServer> livingHosts = blacklist.filterBlacklistedHostsFrom(filteredHosts);
        if (livingHosts.isEmpty()) {
            log.info("There are no known live hosts in the connection pool matching the predicate. We're choosing"
                    + " one at random in a last-ditch attempt at forward progress.");
            livingHosts = filteredHosts;
        }

        Optional<CassandraServer> randomLivingHost = getRandomHostByActiveConnections(livingHosts);
        return randomLivingHost.map(currentPools::get);
    }

    public List<PoolingContainer<CassandraClient>> getAllNonBlacklistedHosts() {
        ImmutableMap<CassandraServer, CassandraClientPoolingContainer> pools = ImmutableMap.copyOf(getPools());
        return blacklist.filterBlacklistedHostsFrom(pools.keySet()).stream()
                .map(pools::get)
                .filter(Objects::nonNull)
                .collect(ImmutableList.toImmutableList());
    }

    public CassandraClientPoolingContainer getRandomGoodHost() {
        return getRandomGoodHostForPredicate(address -> true).orElseThrow(NoHostsAvailableException::new);
    }

    private String getRingViewDescription() {
        return CassandraLogHelper.tokenMap(tokenMap).toString();
    }

    public RangeMap<LightweightOppToken, ImmutableSet<CassandraServer>> getTokenMap() {
        return tokenMap;
    }

    public Map<CassandraServer, CassandraClientPoolingContainer> getPools() {
        return currentPools;
    }

    @VisibleForTesting
    Set<CassandraServer> maybeFilterLocalHosts(Set<CassandraServer> hosts) {
        if (random.nextDouble() < config.localHostWeighting()) {
            Set<CassandraServer> localFilteredHosts = Sets.intersection(localHosts, hosts);
            if (!localFilteredHosts.isEmpty()) {
                return localFilteredHosts;
            }
        }

        return hosts;
    }

    @VisibleForTesting
    Optional<CassandraServer> getRandomHostByActiveConnections(Set<CassandraServer> desiredHosts) {
        Set<CassandraServer> localFilteredHosts = maybeFilterLocalHosts(desiredHosts);
        Map<CassandraServer, CassandraClientPoolingContainer> matchingPools =
                ImmutableMap.copyOf(Maps.filterKeys(currentPools, localFilteredHosts::contains));
        if (matchingPools.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(WeightedServers.create(matchingPools).getRandomServer());
    }

    public void debugLogStateOfPool() {
        if (log.isDebugEnabled()) {
            StringBuilder currentState = new StringBuilder();
            currentState.append(String.format(
                    "POOL STATUS: Current blacklist = %s,%n current hosts in pool = %s%n",
                    blacklist.describeBlacklistedHosts(), currentPools.keySet().toString()));
            for (Map.Entry<CassandraServer, CassandraClientPoolingContainer> entry : currentPools.entrySet()) {
                int activeCheckouts = entry.getValue().getActiveCheckouts();
                int totalAllowed = entry.getValue().getPoolSize();

                currentState.append(String.format(
                        "\tPOOL STATUS: Pooled host %s has %s out of %s connections checked out.%n",
                        entry.getKey(),
                        activeCheckouts > 0 ? Integer.toString(activeCheckouts) : "(unknown)",
                        totalAllowed > 0 ? Integer.toString(totalAllowed) : "(not bounded)"));
            }
            log.debug("Current pool state: {}", UnsafeArg.of("currentState", currentState.toString()));
        }
    }

    public CassandraServer getRandomCassandraNodeForKey(byte[] key) {
        ImmutableSet<CassandraServer> hostsForKey = getHostsFor(key);

        if (hostsForKey == null) {
            if (config.autoRefreshNodes()) {
                log.info("We attempted to route your query to a cassandra host that already contains the relevant data."
                        + " However, the mapping of which host contains which data is not available yet."
                        + " We will choose a random host instead.");
            }
            return getRandomGoodHost().getCassandraServer();
        }

        Set<CassandraServer> liveOwnerHosts = blacklist.filterBlacklistedHostsFrom(hostsForKey);

        if (!liveOwnerHosts.isEmpty()) {
            Optional<CassandraServer> activeHost = getRandomHostByActiveConnections(liveOwnerHosts);
            if (activeHost.isPresent()) {
                return activeHost.get();
            }
        }

        log.warn(
                "Perf / cluster stability issue. Token aware query routing has failed because there are no known "
                        + "live hosts that claim ownership of the given range."
                        + " Falling back to choosing a random live node."
                        + " Current host blacklist is {}."
                        + " Current state logged at TRACE",
                SafeArg.of("blacklistedNodesCount", blacklist.size()));

        // The following trace can cause high memory pressure
        if (log.isTraceEnabled()) {
            log.trace(
                    "Current ring view is: {}." + " Current host blacklist is {}.",
                    SafeArg.of("tokenMap", getRingViewDescription()),
                    SafeArg.of("blacklistedNodes", blacklist.blacklistDetails()));
        }
        return getRandomGoodHost().getCassandraServer();
    }

    public CassandraClientPoolingContainer createPool(CassandraServer server) {
        int currentPoolNumber = cassandraHosts.indexOf(server) + 1;
        return new CassandraClientPoolingContainer(metricsManager, server, config, currentPoolNumber, poolMetrics);
    }

    public void addPool(CassandraServer server) {
        addPool(server, createPool(server));
    }

    public void addPool(CassandraServer cassandraServer, CassandraClientPoolingContainer container) {
        currentPools.put(cassandraServer, container);
    }

    /**
     * Removes the pool from the set of current pools. Note that this shuts down all idle connections, but active ones
     * remain alive until they are returned to the pool, whereby they are destroyed immediately. Threads waiting on the
     * pool will be interrupted.
     */
    public CassandraClientPoolingContainer removePool(CassandraServer removedServerAddress) {
        blacklist.remove(removedServerAddress);
        return currentPools.remove(removedServerAddress);
    }

    public void cacheInitialCassandraHosts() {
        ImmutableSet<CassandraServer> thriftSocket =
                getCurrentServerListFromConfig().keySet();

        cassandraHosts = thriftSocket.stream()
                .sorted(Comparator.comparing(CassandraServer::cassandraHostName))
                .collect(Collectors.toList());
        cassandraHosts.forEach(this::addPool);
    }

    public void clearInitialCassandraHosts() {
        cassandraHosts = Collections.emptyList();
    }

    @VisibleForTesting
    void overrideHostToDatacenterMapping(ImmutableMap<CassandraServer, String> hostToDatacenterOverride) {
        this.hostToDatacenter = hostToDatacenterOverride;
    }

    private static final class NoHostsAvailableException extends AtlasDbDependencyException implements SafeLoggable {

        private static final String MESSAGE = "No hosts available.";

        NoHostsAvailableException() {
            super(MESSAGE);
        }

        @Override
        public @Safe String getLogMessage() {
            return MESSAGE;
        }

        @Override
        public List<Arg<?>> getArgs() {
            return List.of();
        }
    }
}
