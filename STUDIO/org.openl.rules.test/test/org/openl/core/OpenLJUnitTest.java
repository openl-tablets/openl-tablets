package org.openl.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;

public class OpenLJUnitTest {
    @Test
    public void comparisonTest() throws Exception {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(".//test-resources//junit//Comparison.xlsx");
        engineFactory.setExecutionMode(false);
        IOpenClass openClass = engineFactory.getCompiledOpenClass().getOpenClass();

        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        assertEquals(6, tests.length);

        Object target = openClass.newInstance(new SimpleRulesVM().getRuntimeEnv());
        int cnt = 0;
        for (TestSuiteMethod testSuiteMethod : tests) {
            TestUnitsResults res = (TestUnitsResults) testSuiteMethod
                .invoke(target, new Object[0], new SimpleRulesVM().getRuntimeEnv());
            final int numberOfFailures = res.getNumberOfFailures();
            assertEquals(2, numberOfFailures);

            for (ITestUnit testUnit : res.getTestUnits()) {
                if (!testUnit.getErrors().isEmpty()) {
                    assertEquals(1, testUnit.getErrors().size());
                    assertTrue(testUnit.getErrors()
                        .get(0)
                        .getSummary()
                        .contains("Object '0' is outside of valid domain") || testUnit.getErrors()
                            .get(0)
                            .getSummary()
                            .contains("Object '0.0' is outside of valid domain"));
                    cnt++;
                }
            }
        }
        assertEquals(12, cnt);
    }
}
