package org.openl.rules.testmethod;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableIds;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

@Slf4j
public class TestSuite implements INamedThing {
    public static final String VIRTUAL_TEST_SUITE = "Virtual test suite";
    @Getter
    private TestSuiteMethod testSuiteMethod;
    @Getter
    private final TestDescription[] tests;
    private TestRunner testRunner = new TestRunner(TestUnit.Builder.getInstance());

    public TestSuite(TestSuiteMethod testSuiteMethod) {
        this.testSuiteMethod = testSuiteMethod;
        tests = testSuiteMethod.getTests();
    }

    public TestSuite(TestSuiteMethod testSuiteMethod, TestRunner testRunner) {
        this.testSuiteMethod = testSuiteMethod;
        tests = testSuiteMethod.getTests();
        this.testRunner = testRunner;
    }

    public TestSuite(TestSuiteMethod testSuiteMethod, int... testIndices) {
        this.testSuiteMethod = testSuiteMethod;
        tests = new TestDescription[testIndices.length];
        for (var i = 0; i < testIndices.length; i++) {
            tests[i] = testSuiteMethod.getTest(testIndices[i]);
        }
    }

    public TestSuite(TestDescription... tests) {
        this.tests = tests;
    }

    public TestDescription getTest(int testNumber) {
        return tests[testNumber];
    }

    /**
     * Runs the cases on the threads of the executor, each thread taking every n-th case.
     *
     * <p>A caller asked to stop has the threads run no further case, and asks the threads running a case to stop too.
     * It waits for the cases already running, and the results hold the cases that ran; the caller stays interrupted,
     * so it sees why the results are short.
     */
    public TestUnitsResults invokeParallel(TestSuiteExecutor testSuiteExecutor,
                                           final IOpenClass openClass,
                                           final int ntimes) {
        final var THREAD_COUNT = testSuiteExecutor.getThreadCount();
        var threadPoolExecutor = testSuiteExecutor.getExecutor();

        final var testUnitResults = new TestUnitsResults(this);
        final var countDownLatch = new CountDownLatch(THREAD_COUNT);
        final ITestUnit[] testUnitResultsArray = new ITestUnit[getNumberOfTests()];
        // A caller asked to stop before the threads start has them run no case at all.
        final var workers = new Workers(Thread.currentThread().isInterrupted());

        for (var i = 0; i < THREAD_COUNT; i++) {
            final var numThread = i;
            Runnable runnable = () -> {
                try {
                    for (var j = numThread; j < getNumberOfTests() && workers.enter(); j += THREAD_COUNT) {
                        try {
                            testUnitResultsArray[j] = executeTest(openClass, j, ntimes);
                        } finally {
                            workers.leave();
                        }
                    }
                } catch (Exception e) {
                    log.error("Test execution failed", e);
                } finally {
                    countDownLatch.countDown();
                }
            };
            threadPoolExecutor.execute(runnable);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            workers.stop();
            awaitRunningCases(countDownLatch);
            Thread.currentThread().interrupt();
        }
        // A case that did not run - the caller was interrupted, or the case failed to run - has no result to show.
        for (ITestUnit testUnit : testUnitResultsArray) {
            if (testUnit != null) {
                testUnitResults.addTestUnit(testUnit);
            }
        }
        return testUnitResults;
    }

    /** The threads running a case of a parallel run, asked to stop together with the run. */
    private static final class Workers {
        private final Set<Thread> running = new HashSet<>();
        private boolean stopped;

        Workers(boolean stopped) {
            this.stopped = stopped;
        }

        /** Lets the thread take a case, unless the run is asked to stop. */
        synchronized boolean enter() {
            if (stopped) {
                return false;
            }
            running.add(Thread.currentThread());
            return true;
        }

        /** The thread ran its case: asking the run to stop no longer reaches it. */
        synchronized void leave() {
            running.remove(Thread.currentThread());
        }

        /** Takes no further case, and asks the threads running a case to stop. */
        synchronized void stop() {
            stopped = true;
            running.forEach(Thread::interrupt);
        }
    }

    /**
     * Waits for the cases the threads are running when the caller is asked to stop, so that their results are read
     * once they are written. The caller restores its interruption afterwards.
     */
    @SuppressWarnings("java:S2142")
    private static void awaitRunningCases(CountDownLatch countDownLatch) {
        var ended = false;
        while (!ended) {
            try {
                countDownLatch.await();
                ended = true;
            } catch (InterruptedException again) {
                // Asked to stop once more: the cases that are running end all the same.
            }
        }
    }

    public TestUnitsResults invokeSequentially(final IOpenClass openClass, int ntimes) {
        return invokeSequentially(openClass, ntimes, null);
    }

    /**
     * Runs the cases one after another, attaching {@code tracer} to each test's environment so a debug session can
     * observe it.
     *
     * <p>A thread asked to stop runs no further case. The results hold the cases that ran, and the thread stays
     * interrupted, so the caller sees why the results are short.
     */
    public TestUnitsResults invokeSequentially(final IOpenClass openClass, int ntimes, Tracer tracer) {
        var testUnitResults = new TestUnitsResults(this);
        for (var i = 0; i < getNumberOfTests() && !Thread.currentThread().isInterrupted(); i++) {
            final var testUnit = executeTest(openClass, i, ntimes, tracer);
            testUnitResults.addTestUnit(testUnit);
        }

        return testUnitResults;
    }

    public ITestUnit executeTest(IOpenClass openClass, int test, int ntimes) {
        return executeTest(openClass, test, ntimes, null);
    }

    public ITestUnit executeTest(IOpenClass openClass, int test, int ntimes, Tracer tracer) {
        var currentTest = getTest(test);
        var env = new SimpleRulesVM().getRuntimeEnv();
        env.setTracer(tracer);
        final var target = openClass.newInstance(env);
        return testRunner.runTest(currentTest, target, env, ntimes);
    }

    public TestUnitsResults invoke(Object target, IRuntimeEnv env) {
        var testUnitResults = new TestUnitsResults(this);

        for (var i = 0; i < getNumberOfTests(); i++) {
            var currentTest = getTest(i);
            var testUnit = testRunner.runTest(currentTest, target, env, 1);
            testUnitResults.addTestUnit(testUnit);
        }

        return testUnitResults;
    }

    public int getNumberOfTests() {
        return tests.length;
    }

    @Override
    public String getName() {
        if (testSuiteMethod != null) {
            return testSuiteMethod.getName();
        }
        return VIRTUAL_TEST_SUITE;
    }

    public IOpenMethod getTestedMethod() {
        if (testSuiteMethod != null) {
            return testSuiteMethod.getTestedMethod();
        }
        return tests[0].getTestedMethod();
    }

    @Override
    public String getDisplayName(int mode) {
        if (testSuiteMethod != null) {
            return testSuiteMethod.getDisplayName(mode);
        }
        return getName();
    }

    public String getUri() {
        if (testSuiteMethod != null) {
            return testSuiteMethod.getSourceUrl();
        }
        return getTestedMethod().getInfo().getSourceUrl();
    }

    /**
     * The id of the table the suite runs: the test table, or the rule table run with the input it is given.
     *
     * @return the id of the table, or {@code null} when the table is not known
     */
    public @Nullable String getTableId() {
        return TableIds.idOf(testSuiteMethod != null ? testSuiteMethod.getInfo() : getTestedMethod().getInfo());
    }
}
