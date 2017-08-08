/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.atlasdb.timelock.clock;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.palantir.atlasdb.http.AtlasDbHttpClients;
import com.palantir.atlasdb.util.AtlasDbMetrics;
import com.palantir.common.concurrent.NamedThreadFactory;

/**
 * ClockSkewMonitor keeps track of the system time of the other nodes in the cluster, and compares it to the local
 * clock. It's purpose is to monitor if the other nodes' clock progress at the same pace as the local clock.
 */
public final class ClockSkewMonitor {
    @VisibleForTesting
    static final Duration PAUSE_BETWEEN_REQUESTS = Duration.of(1, ChronoUnit.SECONDS);

    private final ClockSkewEvents events;
    private final Map<String, ClockService> monitorByServer;
    private final Map<String, RequestTime> previousRequestsByServer;
    private final ScheduledExecutorService executorService;
    private final ClockService localClockService;

    public static ClockSkewMonitor create(Set<String> remoteServers, Optional<SSLSocketFactory> optionalSecurity) {
        Map<String, ClockService> monitors = Maps.toMap(remoteServers,
                (remoteServer) -> AtlasDbHttpClients.createProxy(optionalSecurity, remoteServer, ClockService.class));

        Map<String, RequestTime> previousRequests = Maps.newHashMap();
        for (String server : remoteServers) {
            previousRequests.put(server, RequestTime.EMPTY);
        }

        return new ClockSkewMonitor(monitors, previousRequests,
                new ClockSkewEvents(AtlasDbMetrics.getMetricRegistry()),
                Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("clock-skew-monitor", true)),
                new ClockServiceImpl());
    }

    @VisibleForTesting
    ClockSkewMonitor(
            Map<String, ClockService> monitorByServer,
            Map<String, RequestTime> previousRequestsByServer,
            ClockSkewEvents events,
            ScheduledExecutorService executorService,
            ClockService localClockService) {
        this.monitorByServer = monitorByServer;
        this.previousRequestsByServer = previousRequestsByServer;
        this.events = events;
        this.executorService = executorService;
        this.localClockService = localClockService;
    }

    public void runInBackground() {
        executorService.scheduleAtFixedRate(this::runOnce, 0, PAUSE_BETWEEN_REQUESTS.toNanos(), TimeUnit.NANOSECONDS);
    }

    private void runOnce() {
        try {
            runInternal();
        } catch (Throwable t) {
            events.exception(t);
        }
    }

    private void runInternal() {
        monitorByServer.forEach((server, remoteClockService) -> {
            long localTimeAtStart = localClockService.getSystemTimeInNanos();
            long remoteSystemTime = remoteClockService.getSystemTimeInNanos();
            long localTimeAtEnd = localClockService.getSystemTimeInNanos();

            RequestTime previousRequest = previousRequestsByServer.get(server);
            RequestTime newRequest = new RequestTime(localTimeAtStart, localTimeAtEnd, remoteSystemTime);
            if (!previousRequest.equals(RequestTime.EMPTY)) {
                new ClockSkewComparer(server, events, previousRequest, newRequest).compare();
            }

            previousRequestsByServer.put(server, newRequest);
        });
    }
}
