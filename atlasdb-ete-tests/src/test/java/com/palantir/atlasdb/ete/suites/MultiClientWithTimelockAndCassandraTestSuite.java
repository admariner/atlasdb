/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.atlasdb.ete.suites;

import com.palantir.atlasdb.containers.CassandraEnvironment;
import com.palantir.atlasdb.ete.suiteclasses.CassandraTimestampsEteTest;
import com.palantir.atlasdb.ete.suiteclasses.CoordinationEteTest;
import com.palantir.atlasdb.ete.suiteclasses.LockWithTimelockEteTest;
import com.palantir.atlasdb.ete.suiteclasses.MultipleSchemaVersionsCoordinationEteTest;
import com.palantir.atlasdb.ete.suiteclasses.TargetedSweepEteTest;
import com.palantir.atlasdb.ete.suiteclasses.TimestampManagementEteTest;
import com.palantir.atlasdb.ete.suiteclasses.TodoEteTest;
import com.palantir.atlasdb.ete.utilities.EteSetup;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TodoEteTest.class,
    TargetedSweepEteTest.class,
    CassandraTimestampsEteTest.class,
    TimestampManagementEteTest.class,
    CoordinationEteTest.class,
    MultipleSchemaVersionsCoordinationEteTest.class,
    LockWithTimelockEteTest.class
})
public class MultiClientWithTimelockAndCassandraTestSuite extends EteSetup {
    @ClassRule
    public static final RuleChain COMPOSITION_SETUP = EteSetup.setupCompositionWithTimelock(
            MultiClientWithTimelockAndCassandraTestSuite.class,
            "docker-compose.multi-client-with-timelock-and-cassandra.yml",
            Clients.MULTI,
            CassandraEnvironment.get());
}
