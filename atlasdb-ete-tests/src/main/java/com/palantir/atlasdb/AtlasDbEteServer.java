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
package com.palantir.atlasdb;

import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Stopwatch;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.palantir.atlasdb.blob.BlobSchema;
import com.palantir.atlasdb.cleaner.CleanupFollower;
import com.palantir.atlasdb.cleaner.Follower;
import com.palantir.atlasdb.config.AtlasDbConfig;
import com.palantir.atlasdb.config.AtlasDbRuntimeConfig;
import com.palantir.atlasdb.coordination.CoordinationService;
import com.palantir.atlasdb.coordination.SimpleCoordinationResource;
import com.palantir.atlasdb.factory.TransactionManagers;
import com.palantir.atlasdb.http.NotInitializedExceptionMapper;
import com.palantir.atlasdb.internalschema.InternalSchemaMetadata;
import com.palantir.atlasdb.internalschema.TransactionSchemaManager;
import com.palantir.atlasdb.internalschema.persistence.CoordinationServices;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.lock.SimpleLockResource;
import com.palantir.atlasdb.sweep.CellsSweeper;
import com.palantir.atlasdb.sweep.SweepTaskRunner;
import com.palantir.atlasdb.sweep.queue.SpecialTimestampsSupplier;
import com.palantir.atlasdb.sweep.queue.TargetedSweepFollower;
import com.palantir.atlasdb.sweep.queue.TargetedSweeper;
import com.palantir.atlasdb.table.description.Schema;
import com.palantir.atlasdb.timestamp.SimpleEteTimestampResource;
import com.palantir.atlasdb.todo.SimpleTodoResource;
import com.palantir.atlasdb.todo.TodoClient;
import com.palantir.atlasdb.todo.TodoSchema;
import com.palantir.atlasdb.transaction.api.TransactionManager;
import com.palantir.atlasdb.transaction.impl.TransactionSchemaVersionEnforcement;
import com.palantir.atlasdb.transaction.service.TransactionService;
import com.palantir.atlasdb.transaction.service.TransactionServices;
import com.palantir.atlasdb.util.MetricsManagers;
import com.palantir.conjure.java.api.config.service.UserAgent;
import com.palantir.conjure.java.server.jersey.ConjureJerseyFeature;
import com.palantir.logsafe.exceptions.SafeIllegalStateException;
import com.palantir.logsafe.logger.SafeLogger;
import com.palantir.logsafe.logger.SafeLoggerFactory;
import com.palantir.tritium.metrics.registry.SharedTaggedMetricRegistries;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.optional.EmptyOptionalException;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class AtlasDbEteServer extends Application<AtlasDbEteConfiguration> {
    public static final long CREATE_TRANSACTION_MANAGER_MAX_WAIT_TIME_SECS = 300;

    private static final SafeLogger log = SafeLoggerFactory.get(AtlasDbEteServer.class);
    private static final long CREATE_TRANSACTION_MANAGER_POLL_INTERVAL_SECS = 5;
    private static final ImmutableSet<Schema> ETE_SCHEMAS =
            ImmutableSet.of(TodoSchema.getSchema(), BlobSchema.getSchema());
    private static final Follower FOLLOWER = CleanupFollower.create(ETE_SCHEMAS);

    public static void main(String[] args) throws Exception {
        new AtlasDbEteServer().run(args);
    }

    @Override
    public void initialize(Bootstrap<AtlasDbEteConfiguration> bootstrap) {
        bootstrap.setMetricRegistry(SharedMetricRegistries.getOrCreate("AtlasDbTest"));
        enableEnvironmentVariablesInConfig(bootstrap);
        bootstrap.getObjectMapper().registerModule(new Jdk8Module());
    }

    @Override
    public void run(AtlasDbEteConfiguration config, final Environment environment) throws Exception {
        TaggedMetricRegistry taggedMetrics = SharedTaggedMetricRegistries.getSingleton();
        TransactionManager txManager = tryToCreateTransactionManager(config, environment, taggedMetrics);
        Supplier<SweepTaskRunner> sweepTaskRunner = Suppliers.memoize(() -> getSweepTaskRunner(txManager));
        TargetedSweeper sweeper = TargetedSweeper.createUninitializedForTest(() -> 1);
        Supplier<TargetedSweeper> sweeperSupplier = Suppliers.memoize(() -> initializeAndGet(sweeper, txManager));
        ensureTransactionSchemaVersionInstalled(config.getAtlasDbConfig(), config.getAtlasDbRuntimeConfig(), txManager);

        environment
                .jersey()
                .register(new SimpleTodoResource(new TodoClient(txManager, sweepTaskRunner, sweeperSupplier)));
        environment.jersey().register(SimpleCoordinationResource.create(txManager));
        environment.jersey().register(ConjureJerseyFeature.INSTANCE);
        environment.jersey().register(new NotInitializedExceptionMapper());
        environment.jersey().register(new SimpleEteTimestampResource(txManager));
        environment.jersey().register(new SimpleLockResource(txManager));
        environment.jersey().register(new EmptyOptionalTo204ExceptionMapper());
    }

    private void ensureTransactionSchemaVersionInstalled(
            AtlasDbConfig config, Optional<AtlasDbRuntimeConfig> runtimeConfig, TransactionManager transactionManager) {
        if (runtimeConfig.isEmpty()
                || runtimeConfig
                        .get()
                        .internalSchema()
                        .targetTransactionsSchemaVersion()
                        .isEmpty()) {
            return;
        }
        CoordinationService<InternalSchemaMetadata> coordinationService = CoordinationServices.createDefault(
                transactionManager.getKeyValueService(),
                transactionManager.getTimestampService(),
                MetricsManagers.createForTests(),
                config.initializeAsync());
        TransactionSchemaManager manager = new TransactionSchemaManager(coordinationService);

        int targetSchemaVersion = runtimeConfig
                .get()
                .internalSchema()
                .targetTransactionsSchemaVersion()
                .get();
        TransactionSchemaVersionEnforcement.ensureTransactionsGoingForwardHaveSchemaVersion(
                manager,
                transactionManager.getTimestampService(),
                transactionManager.getTimestampManagementService(),
                targetSchemaVersion);
    }

    private TransactionManager tryToCreateTransactionManager(
            AtlasDbEteConfiguration config, Environment environment, TaggedMetricRegistry taggedMetricRegistry)
            throws InterruptedException {
        if (config.getAtlasDbConfig().initializeAsync()) {
            return createTransactionManager(
                    config.getAtlasDbConfig(), config.getAtlasDbRuntimeConfig(), environment, taggedMetricRegistry);
        } else {
            return createTransactionManagerWithRetry(
                    config.getAtlasDbConfig(), config.getAtlasDbRuntimeConfig(), environment, taggedMetricRegistry);
        }
    }

    private SweepTaskRunner getSweepTaskRunner(TransactionManager transactionManager) {
        KeyValueService kvs = transactionManager.getKeyValueService();
        LongSupplier ts = transactionManager.getTimestampService()::getFreshTimestamp;
        TransactionService txnService =
                TransactionServices.createRaw(kvs, transactionManager.getTimestampService(), false);
        CleanupFollower follower = CleanupFollower.create(ETE_SCHEMAS);
        CellsSweeper cellsSweeper = new CellsSweeper(transactionManager, kvs, ImmutableList.of(follower));
        return new SweepTaskRunner(kvs, ts, ts, txnService, cellsSweeper);
    }

    private TargetedSweeper initializeAndGet(TargetedSweeper sweeper, TransactionManager txManager) {
        // Intentionally providing the immutable timestamp instead of unreadable to avoid the delay
        sweeper.initializeWithoutRunning(
                new SpecialTimestampsSupplier(txManager::getImmutableTimestamp, txManager::getImmutableTimestamp),
                txManager.getTimelockService(),
                txManager.getKeyValueService(),
                TransactionServices.createRaw(txManager.getKeyValueService(), txManager.getTimestampService(), false),
                new TargetedSweepFollower(ImmutableList.of(FOLLOWER), txManager));
        sweeper.runInBackground();
        return sweeper;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private TransactionManager createTransactionManagerWithRetry(
            AtlasDbConfig config,
            Optional<AtlasDbRuntimeConfig> atlasDbRuntimeConfig,
            Environment environment,
            TaggedMetricRegistry taggedMetricRegistry)
            throws InterruptedException {
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) < CREATE_TRANSACTION_MANAGER_MAX_WAIT_TIME_SECS) {
            try {
                return createTransactionManager(config, atlasDbRuntimeConfig, environment, taggedMetricRegistry);
            } catch (RuntimeException e) {
                log.warn("An error occurred while trying to create transaction manager. Retrying...", e);
                Thread.sleep(CREATE_TRANSACTION_MANAGER_POLL_INTERVAL_SECS);
            }
        }
        throw new SafeIllegalStateException("Timed-out because we were unable to create transaction manager");
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private TransactionManager createTransactionManager(
            AtlasDbConfig config,
            Optional<AtlasDbRuntimeConfig> atlasDbRuntimeConfigOptional,
            Environment environment,
            TaggedMetricRegistry taggedMetricRegistry) {
        return TransactionManagers.builder()
                .config(config)
                .userAgent(UserAgent.of(UserAgent.Agent.of("atlasdb-ete-test", "0.0.0")))
                .globalMetricsRegistry(environment.metrics())
                .globalTaggedMetricRegistry(taggedMetricRegistry)
                .addAllSchemas(ETE_SCHEMAS)
                .runtimeConfigSupplier(() -> atlasDbRuntimeConfigOptional)
                .build()
                .serializable();
    }

    private void enableEnvironmentVariablesInConfig(Bootstrap<AtlasDbEteConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor()));
    }

    private static final class EmptyOptionalTo204ExceptionMapper implements ExceptionMapper<EmptyOptionalException> {
        @Override
        public Response toResponse(EmptyOptionalException exception) {
            return Response.noContent().build();
        }
    }
}
