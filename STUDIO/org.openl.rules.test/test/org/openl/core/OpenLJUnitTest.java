package org.openl.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Objects;

import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.testmethod.result.ComparedResult;
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

    @Test
    public void EPBDS_12729() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(
            "./test-resources/junit/EPBDS-12729_error_code_message.xlsx");
        engineFactory.setExecutionMode(false);
        IOpenClass openClass = engineFactory.getCompiledOpenClass().getOpenClass();

        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        assertEquals(13, tests.length);

        Object target = openClass.newInstance(new SimpleRulesVM().getRuntimeEnv());

        {
            TestUnitsResults res1 = getRestUnitResult(target, tests, "error1_test1");
            assertEquals(3, res1.getNumberOfFailures());
            assertEquals(4, res1.getTestUnits().size());

            ITestUnit testUnit1 = res1.getTestUnits().get(0);
            assertEquals(TestStatus.TR_NEQ, testUnit1.getResultStatus());
            assertEquals(1, testUnit1.getComparisonResults().size());
            assertComparedResult(new ComparedResult(null, "Foo bar", "foo.bar: Foo bar", TestStatus.TR_NEQ),
                testUnit1.getComparisonResults().get(0));

            ITestUnit testUnit2 = res1.getTestUnits().get(1);
            assertEquals(TestStatus.TR_NEQ, testUnit2.getResultStatus());
            assertEquals(1, testUnit2.getComparisonResults().size());
            assertComparedResult(new ComparedResult(null, "foo bar", "foo.bar: Foo bar", TestStatus.TR_NEQ),
                testUnit2.getComparisonResults().get(0));

            ITestUnit testUnit3 = res1.getTestUnits().get(2);
            assertEquals(TestStatus.TR_NEQ, testUnit3.getResultStatus());
            assertEquals(1, testUnit3.getComparisonResults().size());
            assertComparedResult(new ComparedResult(null, null, "foo.bar: Foo bar", TestStatus.TR_NEQ),
                testUnit3.getComparisonResults().get(0));

            ITestUnit testUnit4 = res1.getTestUnits().get(3);
            assertEquals(TestStatus.TR_OK, testUnit4.getResultStatus());
        }

        {
            TestUnitsResults res1 = getRestUnitResult(target, tests, "error3_test2");
            assertEquals(2, res1.getNumberOfFailures());
            assertEquals(3, res1.getTestUnits().size());

            ITestUnit testUnit1 = res1.getTestUnits().get(0);
            assertEquals(TestStatus.TR_OK, testUnit1.getResultStatus());

            ITestUnit testUnit2 = res1.getTestUnits().get(1);
            assertEquals(TestStatus.TR_NEQ, testUnit2.getResultStatus());
            assertEquals(1, testUnit2.getComparisonResults().size());
            assertComparedResult(new ComparedResult("message", null, "Foo bar", TestStatus.TR_NEQ),
                testUnit2.getComparisonResults().get(0));

            ITestUnit testUnit3 = res1.getTestUnits().get(2);
            assertEquals(TestStatus.TR_NEQ, testUnit3.getResultStatus());
            assertEquals(1, testUnit3.getComparisonResults().size());
            assertComparedResult(new ComparedResult("message", "qwerty", "Foo bar", TestStatus.TR_NEQ),
                testUnit3.getComparisonResults().get(0));
        }

        {
            TestUnitsResults res1 = getRestUnitResult(target, tests, "error1_test3");
            assertEquals(4, res1.getNumberOfFailures());
            assertEquals(5, res1.getTestUnits().size());

            ITestUnit testUnit1 = res1.getTestUnits().get(0);
            assertEquals(TestStatus.TR_OK, testUnit1.getResultStatus());

            ITestUnit testUnit2 = res1.getTestUnits().get(1);
            assertEquals(TestStatus.TR_NEQ, testUnit2.getResultStatus());
            assertEquals(2, testUnit2.getComparisonResults().size());
            assertComparedResult(new ComparedResult("message", "foo bar", "Foo bar", TestStatus.TR_NEQ),
                testUnit2.getComparisonResults().get(0));
            assertComparedResult(new ComparedResult("code", "foo.bar", "foo.bar", TestStatus.TR_OK),
                testUnit2.getComparisonResults().get(1));

            ITestUnit testUnit3 = res1.getTestUnits().get(2);
            assertEquals(TestStatus.TR_NEQ, testUnit3.getResultStatus());
            assertEquals(2, testUnit3.getComparisonResults().size());
            assertComparedResult(new ComparedResult("message", "foo bar", "Foo bar", TestStatus.TR_NEQ),
                testUnit3.getComparisonResults().get(0));
            assertComparedResult(new ComparedResult("code", "bar.foo", "foo.bar", TestStatus.TR_NEQ),
                testUnit3.getComparisonResults().get(1));

            ITestUnit testUnit4 = res1.getTestUnits().get(3);
            assertEquals(TestStatus.TR_NEQ, testUnit4.getResultStatus());
            assertEquals(2, testUnit4.getComparisonResults().size());
            assertComparedResult(new ComparedResult("message", "Foo bar", "Foo bar", TestStatus.TR_OK),
                testUnit4.getComparisonResults().get(0));
            assertComparedResult(new ComparedResult("code", "bar.foo", "foo.bar", TestStatus.TR_NEQ),
                testUnit4.getComparisonResults().get(1));

            ITestUnit testUnit5 = res1.getTestUnits().get(4);
            assertEquals(TestStatus.TR_NEQ, testUnit5.getResultStatus());
            assertEquals(2, testUnit5.getComparisonResults().size());
            assertComparedResult(new ComparedResult("message", null, "Foo bar", TestStatus.TR_NEQ),
                testUnit5.getComparisonResults().get(0));
            assertComparedResult(new ComparedResult("code", null, "foo.bar", TestStatus.TR_NEQ),
                testUnit5.getComparisonResults().get(1));
        }

    }

    public static void assertComparedResult(ComparedResult expected, ComparedResult actural) {
        assertEquals(expected.getFieldName(), actural.getFieldName());
        assertEquals(expected.getExpectedValue(), actural.getExpectedValue());
        assertEquals(expected.getStatus(), actural.getStatus());
        assertEquals(expected.getActualValue(), actural.getActualValue());
    }

    public static TestUnitsResults getRestUnitResult(Object target, TestSuiteMethod[] tests, String testTableName) {
        for (TestSuiteMethod testSuiteMethod : tests) {
            if (Objects.equals(testTableName, testSuiteMethod.getName())) {
                return (TestUnitsResults) testSuiteMethod
                    .invoke(target, new Object[0], new SimpleRulesVM().getRuntimeEnv());
            }
        }
        fail(String.format("Can't find '%s' test table,", testTableName));
        return null;
    }
}
