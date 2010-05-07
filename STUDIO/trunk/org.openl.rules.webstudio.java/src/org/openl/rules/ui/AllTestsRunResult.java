/**
 * Created Mar 23, 2007
 */
package org.openl.rules.ui;

import java.util.Arrays;
import java.util.Comparator;

import org.openl.rules.testmethod.TestResult;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class AllTestsRunResult {

    public static class Test {
        TestSuiteMethod method;
        TestResult result;
        String testName;

        public IOpenMethod getMethod() {
            return method;
        }

        public TestResult getResult() {
            return result;
        }

        public String getTestDescription(int i) {
            return method.getTestDescriptions()[i];
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
    }

    private Test[] tests;

    public AllTestsRunResult(IOpenMethod[] methods, String[] names) {

        tests = new Test[methods.length];
        for (int i = 0; i < names.length; i++) {
            tests[i] = new Test();
            tests[i].method = (TestSuiteMethod) methods[i];
            tests[i].testName = names[i];

        }
    }

    private Test findTest(String testName) {
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].testName.equals(testName)) {
                return tests[i];
            }
        }

        throw new RuntimeException("Test " + testName + " not found");
    }

    public Test[] getTests() {

        Comparator<Test> c = new Comparator<Test>() {

            public int compare(Test t1, Test t2) {
                if (t2.result != null && t1.result != null) {
                    int cmp = t2.result.getNumberOfFailures() - t1.result.getNumberOfFailures();
                    if (cmp != 0) {
                        return cmp;
                    }
                }

                return t1.testName.compareTo(t2.testName);
            }

        };
        Arrays.sort(tests, c);

        return tests;
    }

    public int numberOfFailedTests() {
        int cnt = 0;
        for (int i = 0; i < tests.length; i++) {
            cnt += tests[i].result.getNumberOfFailures() > 0 ? 1 : 0;
        }

        return cnt;
    }

    public Object run(String testName, int tid, Object target, IRuntimeEnv env, int ntimes) {
        Test test = findTest(testName);
        return test.run(tid, target, env, ntimes);
    }

    public void setResults(TestResult[] ttr) {

        for (int i = 0; i < ttr.length; i++) {
            tests[i].result = ttr[i];
        }

    }

    public void setTests(Test[] tests) {
        this.tests = tests;
    }

    public int totalNumberOfFailures() {
        int cnt = 0;
        for (int i = 0; i < tests.length; i++) {
            cnt += tests[i].result.getNumberOfFailures();
        }
        return cnt;
    }

    public int totalNumberOfTestUnits() {
        int cnt = 0;
        for (int i = 0; i < tests.length; i++) {
            cnt += tests[i].result.getNumberOfTests();
        }
        return cnt;
    }

}
