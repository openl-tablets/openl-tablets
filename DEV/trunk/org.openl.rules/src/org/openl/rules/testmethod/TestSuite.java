package org.openl.rules.testmethod;

import org.openl.base.INamedThing;
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

    public TestUnitsResults invoke(Object target, IRuntimeEnv env, int ntimes) {
        TestUnitsResults testUnitResults = new TestUnitsResults(this);

        for (int i = 0; i < getNumberOfTests(); i++) {
            TestDescription currentTest = getTest(i);
            testUnitResults.addTestUnit(currentTest.runTest(target, env, ntimes));
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
}
