package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/** The cases a suite runs, and the results it gives for the cases that did not run. */
class TestSuiteTest {

    private final TestDescription first = mock(TestDescription.class);
    private final TestDescription second = mock(TestDescription.class);
    private final TestDescription third = mock(TestDescription.class);
    private final ITestUnit firstResult = mock(ITestUnit.class);
    private final ITestUnit secondResult = mock(ITestUnit.class);
    private final ITestUnit thirdResult = mock(ITestUnit.class);
    private final TestRunner runner = mock(TestRunner.class);
    private final IOpenClass openClass = mock(IOpenClass.class);

    @AfterEach
    void clearInterrupt() {
        // A test that leaves its thread interrupted would stop the next one.
        Thread.interrupted();
    }

    @Test
    void runsEveryCaseInTurn() {
        when(runner.runTest(same(first), any(), any(), anyInt())).thenReturn(firstResult);
        when(runner.runTest(same(second), any(), any(), anyInt())).thenReturn(secondResult);
        when(runner.runTest(same(third), any(), any(), anyInt())).thenReturn(thirdResult);

        var results = suite().invokeSequentially(openClass, 1);

        assertEquals(List.of(firstResult, secondResult, thirdResult), results.getTestUnits());
    }

    @Test
    void runsNoFurtherCaseOnceTheThreadIsAskedToStop() {
        when(runner.runTest(same(first), any(), any(), anyInt())).thenReturn(firstResult);
        when(runner.runTest(same(second), any(), any(), anyInt())).thenAnswer(call -> {
            Thread.currentThread().interrupt();
            return secondResult;
        });
        when(runner.runTest(same(third), any(), any(), anyInt())).thenReturn(thirdResult);

        var results = suite().invokeSequentially(openClass, 1);

        assertEquals(List.of(firstResult, secondResult), results.getTestUnits());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void runsNoFurtherCaseOnceTheRulesOfACaseAreInterrupted() {
        var interrupted = mock(IOpenMethod.class);
        when(interrupted.invoke(any(), any(), any()))
                .thenThrow(new IllegalStateException(new InterruptedException("sleep interrupted")));
        when(first.getTestedMethod()).thenReturn(interrupted);
        var builder = mock(ITestResultBuilder.class);
        when(builder.build(same(first), any(), any(), anyLong())).thenReturn(firstResult);
        var method = mock(TestSuiteMethod.class);
        when(method.getTests()).thenReturn(new TestDescription[]{first, second});

        var results = new TestSuite(method, new TestRunner(builder)).invokeSequentially(openClass, 1);

        assertEquals(List.of(firstResult), results.getTestUnits());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void runsNoCaseInParallelWhenTheCallerIsAskedToStopBeforehand() {
        var executor = new TestSuiteExecutor(2);
        try {
            Thread.currentThread().interrupt();

            var results = suite().invokeParallel(executor, openClass, 1);

            assertEquals(List.of(), results.getTestUnits());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            executor.destroy();
        }
    }

    @Test
    void leavesOutACaseThatFailedToRunInParallel() {
        when(runner.runTest(same(first), any(), any(), anyInt())).thenReturn(firstResult);
        when(runner.runTest(same(second), any(), any(), anyInt())).thenThrow(new IllegalStateException("broken"));
        when(runner.runTest(same(third), any(), any(), anyInt())).thenReturn(thirdResult);
        var executor = new TestSuiteExecutor(3);
        try {
            var results = suite().invokeParallel(executor, openClass, 1);

            assertEquals(List.of(firstResult, thirdResult), results.getTestUnits());
        } finally {
            executor.destroy();
        }
    }

    @Test
    void stopsTheCasesRunningInParallelAndRunsNoFurtherOnceTheCallerIsAskedToStop() {
        var caller = Thread.currentThread();
        var cases = new TestDescription[6];
        for (var i = 0; i < cases.length; i++) {
            cases[i] = mock(TestDescription.class);
        }
        var results = new ITestUnit[cases.length];
        // The first case of each thread is running when the caller is asked to stop, and runs until it is asked to
        // stop too. The caller is asked only once both wait to be stopped: a case stopped while it still waits for the
        // other one would fail rather than end.
        var bothRunning = new CountDownLatch(2);
        var bothWaitToStop = new CountDownLatch(2);
        for (var i = 0; i < cases.length; i++) {
            results[i] = mock(ITestUnit.class);
            var result = results[i];
            var interruptsCaller = i == 0;
            when(runner.runTest(same(cases[i]), any(), any(), anyInt())).thenAnswer(call -> {
                bothRunning.countDown();
                assertTrue(bothRunning.await(5, TimeUnit.SECONDS));
                bothWaitToStop.countDown();
                if (interruptsCaller) {
                    assertTrue(bothWaitToStop.await(5, TimeUnit.SECONDS));
                    caller.interrupt();
                }
                assertThrows(InterruptedException.class, () -> new CountDownLatch(1).await(5, TimeUnit.SECONDS));
                return result;
            });
        }
        var method = mock(TestSuiteMethod.class);
        when(method.getTests()).thenReturn(cases);
        var executor = new TestSuiteExecutor(2);
        try {
            var ran = new TestSuite(method, runner).invokeParallel(executor, openClass, 1);

            assertEquals(List.of(results[0], results[1]), ran.getTestUnits());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            executor.destroy();
        }
    }

    private TestSuite suite() {
        var method = mock(TestSuiteMethod.class);
        when(method.getTests()).thenReturn(new TestDescription[]{first, second, third});
        return new TestSuite(method, runner);
    }
}
