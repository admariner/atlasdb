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

package com.palantir.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import com.palantir.logsafe.exceptions.SafeRuntimeException;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockMakers;
import org.mockito.MockSettings;

public class SafeShutdownRunnerTest {
    private static final RuntimeException EXCEPTION_1 = new RuntimeException("test");
    private static final RuntimeException EXCEPTION_2 = new RuntimeException("bleh");

    private final MockSettings mockSettings = withSettings().mockMaker(MockMakers.SUBCLASS);
    private Runnable mockRunnable = mock(Runnable.class, mockSettings);
    private Runnable throwingRunnable = mock(Runnable.class, mockSettings);
    private Runnable blockingUninterruptibleRunnable = mock(Runnable.class, mockSettings);

    @Before
    public void setupMocks() {
        doAnswer(invocation -> {
                    new Semaphore(0).acquireUninterruptibly();
                    return null;
                })
                .when(blockingUninterruptibleRunnable)
                .run();
        doThrow(EXCEPTION_1).when(throwingRunnable).run();
    }

    @Test
    public void runnerRunsOneTask() {
        SafeShutdownRunner runner = SafeShutdownRunner.createWithCachedThreadpool(Duration.ofSeconds(1));

        runner.shutdownSafely(mockRunnable);

        verify(mockRunnable, times(1)).run();
        assertThatCode(runner::close).doesNotThrowAnyException();
    }

    @Test
    public void exceptionsAreSuppressedAndReportedWhenClosing() {
        SafeShutdownRunner runner = SafeShutdownRunner.createWithCachedThreadpool(Duration.ofSeconds(1));

        assertThatCode(() -> runner.shutdownSafely(throwingRunnable)).doesNotThrowAnyException();
        assertThatThrownBy(runner::close)
                .isInstanceOf(SafeRuntimeException.class)
                .hasSuppressedException(EXCEPTION_1);
    }

    @Test
    public void slowTasksTimeOutWithoutThrowing() {
        SafeShutdownRunner runner = SafeShutdownRunner.createWithCachedThreadpool(Duration.ofMillis(50));

        runner.shutdownSafely(blockingUninterruptibleRunnable);
        runner.shutdownSafely(blockingUninterruptibleRunnable);

        closeAndAssertNumberOfTimeouts(runner, 2);
        verify(blockingUninterruptibleRunnable, times(2)).run();
    }

    @Test
    public void otherTasksStillRunInPresenceOfSlowTasksThatTimeOut() {
        SafeShutdownRunner runner = SafeShutdownRunner.createWithCachedThreadpool(Duration.ofMillis(50));

        runner.shutdownSafely(blockingUninterruptibleRunnable);
        runner.shutdownSafely(blockingUninterruptibleRunnable);
        runner.shutdownSafely(mockRunnable);
        runner.shutdownSafely(blockingUninterruptibleRunnable);
        runner.shutdownSafely(blockingUninterruptibleRunnable);
        runner.shutdownSafely(mockRunnable);

        verify(blockingUninterruptibleRunnable, times(4)).run();
        verify(mockRunnable, times(2)).run();

        closeAndAssertNumberOfTimeouts(runner, 4);
    }

    private void closeAndAssertNumberOfTimeouts(SafeShutdownRunner runner, int number) {
        assertThatThrownBy(runner::close).isInstanceOf(RuntimeException.class).satisfies(exception -> {
            Throwable[] suppressed = exception.getSuppressed();
            assertThat(suppressed).hasSize(number);
            Stream.of(suppressed).forEach(th -> assertThat(th).isInstanceOf(TimeoutException.class));
        });
    }
}
