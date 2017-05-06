package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.Log;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

/**
 * 
 * @author snshor Created May 2, 2010
 * 
 * This class should be used to run all the tests from command-line interface.
 * It may be also used in JUnit test integration
 *
 * @deprecated Use openl-maven-plugin for tests
 */
@Deprecated
public class TestAll {

    public class TestStatistics {
        /**
         * Time in milliseconds
         */
        private long start;
        /**
         * Time in milliseconds
         */
        private long end;

        private long loadTimeMs = -1;

        private ArrayList<TestUnitsResults> results = new ArrayList<TestUnitsResults>();
        String name;

        public TestStatistics(String name, long loadTimeMs) {
            this.name = name;
            this.loadTimeMs = loadTimeMs;
        }

        public void addTestResult(TestUnitsResults res) {
            results.add(res);
        }

        public int getNumberOfFailedTests() {
            int sum = 0;
            for (TestUnitsResults tres : results) {
                if (tres.getNumberOfFailures() > 0)
                    sum++;
            }
            return sum;
        }

        @SuppressWarnings("unchecked")
        public List<TestUnitsResults> getFilteredResults(boolean includePassedTests, boolean orderFailedFirst) {
            List<TestUnitsResults> list = (List<TestUnitsResults>) results.clone();

            return list;
        }

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        public List<TestUnitsResults> getResults() {
            return results;
        }

        @Override
        public String toString() {
            return "Loaded " + name + " in " + loadTimeMs + "ms\n" + "Executed/failed tests: " + results.size() + "/"
                    + getNumberOfFailedTests() + " in " + (end - start) + " ms";
        }
        
        public String printFailedResults()
        {
            StringBuilder sb = new StringBuilder(1000);
            sb.append(toString());
            
            for (TestUnitsResults test : results) {
                if (test.getNumberOfFailures() > 0)
                    sb.append('\n').append(test);
            }
            
            return sb.toString();
        }

        public long getLoadTimeMs() {
            return loadTimeMs;
        }

        public void setLoadTimeMs(long loadTimeMs) {
            this.loadTimeMs = loadTimeMs;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    /**
     * Runs all the tests found in OpenL module (ioc)
     * 
     * @param ioc
     * @return TestStatistics object containing
     */
    public TestStatistics testAll(IOpenClass ioc) {
        TestStatistics stat = new TestStatistics(ioc.getName(), -1);
        return runAllTests(ioc, stat);
    }

    private TestStatistics runAllTests(IOpenClass ioc, TestStatistics stat) {
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        stat.setStart(System.currentTimeMillis());
        for (IOpenMethod m : ioc.getMethods()) {
            if (m.getType().getInstanceClass() == TestUnitsResults.class) {
                runTestMethod(m, ioc.newInstance(env), env, stat);
            }
        }
        stat.setEnd(System.currentTimeMillis());

        return stat;

    }

    protected void runTestMethod(IOpenMethod testMethod, Object engine, IRuntimeEnv env, TestStatistics stat) {
        TestUnitsResults res = (TestUnitsResults) testMethod.invoke(engine, new Object[0], env);
        
        if (!(testMethod instanceof TestSuiteMethod))
            return;
        if (!((TestSuiteMethod)testMethod).isRunmethodTestable())
            return;
        Log.info("Testing " + testMethod.getName() + ", tests: " + res.getNumberOfTestUnits() + ", failures:"
                + res.getNumberOfFailures());

        stat.addTestResult(res);

    }
}
