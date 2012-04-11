/**
 * Created Mar 23, 2007
 */
package org.openl.rules.ui.tests.results;

import java.util.Arrays;
import java.util.Comparator;

import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Wrap a number of tests being run, with their results.
 * 
 * @author snshor
 *
 */
public class RanTestsResults {
    
    private Test[] tests;

    public RanTestsResults(IOpenMethod[] testsToRun, String[] testNames) {

        tests = new Test[testsToRun.length];
        for (int i = 0; i < testNames.length; i++) {
            tests[i] = new Test((TestSuiteMethod) testsToRun[i], testNames[i]);
        }
    }

    private Test findTest(String testName) {
        for (Test test : tests) {
            if (test.getTestName().equals(testName)) {
                return test;
            }
        }

        throw new RuntimeException("Test " + testName + " not found");
    }

    public Test[] getTests() {

        Comparator<Test> c = new Comparator<Test>() {

            public int compare(Test t1, Test t2) {
                if (t2.getTestUnitsResults() != null && t1.getTestUnitsResults() != null) {
                    int cmp = t2.getTestUnitsResults().getNumberOfFailures() - t1.getTestUnitsResults().getNumberOfFailures();
                    if (cmp != 0) {
                        return cmp;
                    }
                }

                return t1.getTestName().compareTo(t2.getTestName());
            }

        };
        Arrays.sort(tests, c);

        return tests;
    }

    public int numberOfFailedTests() {
        int cnt = 0;
        for (Test test : tests) {
            cnt += test.getTestUnitsResults().getNumberOfFailures() > 0 ? 1 : 0;
        }

        return cnt;
    }

    public Object run(String testName, int tid, Object target, IRuntimeEnv env, int ntimes) {
        Test test = findTest(testName);
        return test.run(tid, target, env, ntimes);
    }

    public void setResults(TestUnitsResults[] ttr) {
        for (int i = 0; i < ttr.length; i++) {
            tests[i].setTestUnitsResults(ttr[i]);
        }
    }

    public void setTests(Test[] tests) {
        this.tests = tests;
    }

    public int totalNumberOfFailures() {
        int cnt = 0;
        for (Test test: tests) {
            cnt += test.getTestUnitsResults().getNumberOfFailures();
        }
        return cnt;
    }

    public int totalNumberOfTestUnits() {
        int cnt = 0;
        for (Test test: tests) {
            cnt += test.getTestUnitsResults().getNumberOfTestUnits();
        }
        return cnt;
    }

}
