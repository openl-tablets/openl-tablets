package org.openl.rules.ui.tests.results;

import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Representation of one test table.
 *
 */
public class Test {
    /**
     * test suit method
     */
    private TestSuiteMethod method;
    
    /**
     * results of running the test(contains results for every test unit).
     */
    private TestUnitsResults testUnitsResults;
    
    /**
     * the name of the test.
     */
    private String testName;
    
    public Test(TestSuiteMethod method, String testName) {
        this.method = method;
        this.testName = testName;
    }

    public TestSuiteMethod getMethod() {
        return method;
    }

    public TestUnitsResults getTestUnitsResults() {
        return testUnitsResults;
    }

    public TestDescription getTestDescription(int i) {
        return method.getTestDescriptions()[i];
    }

    public TestDescription[] getTestDescriptions() {
        return method.getTestDescriptions();
    }

    public String getUri() {
        return method.getSourceUrl();
    }

    public String getTestName() {
        return testName;
    }

    public int ntests() {
        return method.getNumberOfTests();
    }

    public Object run(int tid, Object target, IRuntimeEnv env, int ntimes) {
        return method.run(tid, target, env, ntimes);
    }

    public void setMethod(TestSuiteMethod method) {
        this.method = method;
    }

    public void setTestUnitsResults(TestUnitsResults result) {
        this.testUnitsResults = result;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }
}