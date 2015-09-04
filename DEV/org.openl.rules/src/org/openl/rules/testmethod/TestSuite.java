package org.openl.rules.testmethod;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import org.openl.base.INamedThing;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class TestSuite implements INamedThing {
    public static String VIRTUAL_TEST_SUITE = "Virtual test suite";
    private TestSuiteMethod testSuiteMethod;
    private TestDescription[] tests;

    public TestSuite(TestSuiteMethod testSuiteMethod) {
        this.testSuiteMethod = testSuiteMethod;
        tests = testSuiteMethod.getTests();
    }

    public TestSuite(TestSuiteMethod testSuiteMethod, int... testIndices) {
        this.testSuiteMethod = testSuiteMethod;
        tests = new TestDescription[testIndices.length];
        for (int i = 0; i < testIndices.length; i++) {
            tests[i] = testSuiteMethod.getTest(testIndices[i]);
        }
    }

    public TestSuite(TestDescription... tests) {
        this.tests = tests;
    }

    public TestSuiteMethod getTestSuiteMethod() {
        return testSuiteMethod;
    }

    public TestDescription[] getTests() {
        return tests;
    }

    public TestDescription getTest(int testNumber) {
        return tests[testNumber];
    }

    public TestUnitsResults invokeParallel(final IOpenClass openClass, final long ntimes) {
        TestSuiteExecutor testSuiteExecutor = TestSuiteExecutor.getInstance();
        final int THREAD_COUNT = testSuiteExecutor.getThreadCount();
        Executor threadPoolExecutor = testSuiteExecutor.getExecutor();

        final TestUnitsResults testUnitResults = new TestUnitsResults(this);
        final CountDownLatch countDownLatch = new CountDownLatch(THREAD_COUNT);
        final TestUnit[] testUnitResultsArray = new TestUnit[getNumberOfTests()];
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int numThread = i;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < getNumberOfTests(); j++) {
                            if (j % THREAD_COUNT == numThread) {
                                testUnitResultsArray[j] = executeTest(openClass, j, ntimes);
                            }
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            threadPoolExecutor.execute(runnable);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }
        for (int i = 0; i < getNumberOfTests(); i++) {
            testUnitResults.addTestUnit(testUnitResultsArray[i]);
        }
        return testUnitResults;
    }

    public TestUnitsResults invokeSequentially(final IOpenClass openClass, long ntimes) {

        TestUnitsResults testUnitResults = new TestUnitsResults(this);
        for (int i = 0; i < getNumberOfTests(); i++) {
            final TestUnit testUnit = executeTest(openClass, i, ntimes);
            testUnitResults.addTestUnit(testUnit);
        }

        return testUnitResults;
    }

    private TestUnit executeTest(IOpenClass openClass, int test, long ntimes) {
        TestDescription currentTest = getTest(test);
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        final Object target = openClass.newInstance(env);
        return currentTest.runTest(target, env, ntimes);
    }

    public TestUnitsResults invoke(Object target, IRuntimeEnv env) {
        TestUnitsResults testUnitResults = new TestUnitsResults(this);

        for (int i = 0; i < getNumberOfTests(); i++) {
            TestDescription currentTest = getTest(i);
            testUnitResults.addTestUnit(currentTest.runTest(target, env, 1L));
        }

        return testUnitResults;
    }

    public int getNumberOfTests() {
        return tests.length;
    }

    public String getName() {
        if (testSuiteMethod != null) {
            return testSuiteMethod.getName();
        } else {
            return VIRTUAL_TEST_SUITE;
        }
    }

    /**
     * @return <code>true</code> in case this test suite is virtual, and
     *         <code>false</code> if this test suite corresponds to particular
     *         test table.
     */
    public boolean isVirtualTestSuite() {
        return testSuiteMethod == null;
    }

    public IOpenMethod getTestedMethod() {
        if (testSuiteMethod != null) {
            return testSuiteMethod.getTestedMethod();
        } else {
            return tests[0].getTestedMethod();
        }
    }

    public String getDisplayName(int mode) {
        if (testSuiteMethod != null) {
            return testSuiteMethod.getDisplayName(mode);
        } else {
            return getName();
        }
    }

    public String getUri() {
        if (testSuiteMethod != null) {
            return testSuiteMethod.getSourceUrl();
        } else {
            return getTestedMethod().getInfo().getSourceUrl();
        }
    }

    public static interface IRuntimeEnvFactory {
        public IRuntimeEnv buildIRuntimeEnv();
    }

}
