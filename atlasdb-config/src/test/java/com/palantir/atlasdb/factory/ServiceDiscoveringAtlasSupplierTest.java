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
package com.palantir.atlasdb.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

import com.palantir.atlasdb.AtlasDbConstants;
import com.palantir.atlasdb.spi.AtlasDbFactory;
import com.palantir.atlasdb.spi.KeyValueServiceConfig;
import com.palantir.atlasdb.spi.KeyValueServiceConfigHelper;
import com.palantir.atlasdb.util.MetricsManager;
import com.palantir.atlasdb.util.MetricsManagers;
import com.palantir.refreshable.Refreshable;
import com.palantir.timestamp.ManagedTimestampService;
import java.io.IOException;
import java.util.Optional;
import org.junit.Test;

public class ServiceDiscoveringAtlasSupplierTest {
    private final KeyValueServiceConfigHelper kvsConfig = () -> AutoServiceAnnotatedAtlasDbFactory.TYPE;
    private final KeyValueServiceConfigHelper invalidKvsConfig = () -> "should not be found kvs";
    private final AtlasDbFactory delegate = new AutoServiceAnnotatedAtlasDbFactory();
    private final MetricsManager metrics = MetricsManagers.createForTests();

    @Test
    public void delegateToFactoriesAnnotatedWithAutoService() {
        ServiceDiscoveringAtlasSupplier atlasSupplier = createAtlasSupplier(kvsConfig);

        assertThat(atlasSupplier.getKeyValueService())
                .as("delegates createRawKeyValueService")
                .isEqualTo(delegate.createRawKeyValueService(
                        metrics,
                        kvsConfig,
                        Refreshable.only(Optional.empty()),
                        Optional.empty(),
                        AtlasDbFactory.THROWING_FRESH_TIMESTAMP_SOURCE,
                        AtlasDbFactory.DEFAULT_INITIALIZE_ASYNC));

        ManagedTimestampService timestampService = mock(ManagedTimestampService.class);
        AutoServiceAnnotatedAtlasDbFactory.nextTimestampServiceToReturn(timestampService);

        assertThat(atlasSupplier.getManagedTimestampService())
                .as("delegates getManagedTimestampService")
                .isEqualTo(timestampService);
    }

    @Test
    public void notAllowConstructionWithoutAValidBackingFactory() {
        assertThatIllegalStateException()
                .isThrownBy(() -> createAtlasSupplier(invalidKvsConfig))
                .withMessageContaining("No atlas provider")
                .withMessageContaining(invalidKvsConfig.type());
    }

    @Test
    public void returnDifferentTimestampServicesOnSubsequentCalls() {
        ServiceDiscoveringAtlasSupplier supplier = createAtlasSupplier(kvsConfig);
        AutoServiceAnnotatedAtlasDbFactory.nextTimestampServiceToReturn(
                mock(ManagedTimestampService.class), mock(ManagedTimestampService.class));

        assertThat(supplier.getManagedTimestampService())
                .as("Need to get a newly-initialized timestamp service in case leadership changed between calls")
                .isNotSameAs(supplier.getManagedTimestampService());
    }

    @Test
    public void alwaysSaveThreadDumpsToTheSameFile() throws IOException {
        String firstPath = ServiceDiscoveringAtlasSupplier.saveThreadDumps();
        String secondPath = ServiceDiscoveringAtlasSupplier.saveThreadDumps();

        assertThat(firstPath).isEqualTo(secondPath);
    }

    private ServiceDiscoveringAtlasSupplier createAtlasSupplier(KeyValueServiceConfig providedKvsConfig) {
        return new ServiceDiscoveringAtlasSupplier(
                metrics,
                providedKvsConfig,
                Refreshable.only(Optional.empty()),
                Optional.empty(),
                Optional.empty(),
                AtlasDbConstants.DEFAULT_INITIALIZE_ASYNC,
                true,
                AtlasDbFactory.THROWING_FRESH_TIMESTAMP_SOURCE);
    }
}
