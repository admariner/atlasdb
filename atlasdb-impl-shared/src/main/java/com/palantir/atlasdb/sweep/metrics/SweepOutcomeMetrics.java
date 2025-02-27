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
package com.palantir.atlasdb.sweep.metrics;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.SlidingTimeWindowArrayReservoir;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.palantir.atlasdb.AtlasDbMetricNames;
import com.palantir.atlasdb.metrics.MetricPublicationFilter;
import com.palantir.atlasdb.sweep.BackgroundSweeperImpl;
import com.palantir.atlasdb.util.MetricsManager;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.immutables.value.Value;

@Value.Enclosing
public final class SweepOutcomeMetrics {
    public static final ImmutableList<SweepOutcome> LEGACY_OUTCOMES = ImmutableList.copyOf(SweepOutcome.values());
    public static final ImmutableList<SweepOutcome> TARGETED_OUTCOMES = ImmutableList.of(
            SweepOutcome.NOT_ENOUGH_DB_NODES_ONLINE,
            SweepOutcome.SUCCESS,
            SweepOutcome.ERROR,
            SweepOutcome.NOTHING_TO_SWEEP);

    private final Supplier<Metrics> metrics;

    private SweepOutcomeMetrics(Supplier<Metrics> metrics) {
        this.metrics = metrics;
    }

    public static SweepOutcomeMetrics registerLegacy(MetricsManager metricsManager) {
        return new SweepOutcomeMetrics(buildMetrics(
                metricsManager, LEGACY_OUTCOMES, ImmutableMap.of(), BackgroundSweeperImpl.class, Optional.empty()));
    }

    public static SweepOutcomeMetrics registerTargeted(
            MetricsManager metricsManager, Map<String, String> strategyTag, MetricPublicationFilter filter) {
        return new SweepOutcomeMetrics(buildMetrics(
                metricsManager, TARGETED_OUTCOMES, strategyTag, TargetedSweepMetrics.class, Optional.of(filter)));
    }

    public void registerOccurrenceOf(SweepOutcome outcome) {
        if (outcome == SweepOutcome.FATAL) {
            metrics.get().fatal().set(true);
            return;
        }

        metrics.get().reservoir().update(outcome.ordinal());
    }

    private static Supplier<Metrics> buildMetrics(
            MetricsManager manager,
            List<SweepOutcome> outcomes,
            Map<String, String> additionalTags,
            Class<?> forClass,
            Optional<MetricPublicationFilter> filter) {
        return Suppliers.memoize(() -> {
            Metrics metrics = ImmutableSweepOutcomeMetrics.Metrics.builder().build();
            outcomes.forEach(outcome -> {
                ImmutableMap<String, String> tags = ImmutableMap.<String, String>builder()
                        .putAll(additionalTags)
                        .put(AtlasDbMetricNames.TAG_OUTCOME, outcome.name())
                        .buildOrThrow();
                filter.ifPresent(presentFilter ->
                        manager.addMetricFilter(forClass, AtlasDbMetricNames.SWEEP_OUTCOME, tags, filter.get()));
                manager.registerOrGet(
                        forClass, AtlasDbMetricNames.SWEEP_OUTCOME, () -> getOutcomeCount(metrics, outcome), tags);
            });
            return metrics;
        });
    }

    private static Long getOutcomeCount(Metrics metrics, SweepOutcome outcome) {
        if (outcome == SweepOutcome.FATAL) {
            return metrics.fatal().get() ? 1L : 0L;
        }

        return Arrays.stream(metrics.reservoir().getSnapshot().getValues())
                .filter(l -> l == outcome.ordinal())
                .count();
    }

    @Value.Immutable
    interface Metrics {
        @Value.Default
        default Reservoir reservoir() {
            return new SlidingTimeWindowArrayReservoir(60L, TimeUnit.SECONDS);
        }

        @Value.Default
        default AtomicBoolean fatal() {
            return new AtomicBoolean();
        }
    }
}
