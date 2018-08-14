package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.*;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class RoundExpectedResultTest {

    private static final String FILE_NAME = "test/rules/testmethod/RoundExpectedResult.xls";

    private Collection<String> successfulTests = Arrays.asList("MyM2Test",
            "MyM3Test",
            "MyM3Test2",
            "MyM3Test3",
            "MyM4Test",
            "MyM4Test2",
            "MyM4Test3",
            "MyM5Test",
            "MyM6Test",
            "MyM6_60Test",
            "MyM6_61Test",
            "MyM6_62Test",
            "MyM7Test",
            "MyM7Test3",
            "MyM8Test",
            "MyM8Test2",
            "MyM8Test3");

    private Collection<String> failedTests = Arrays.asList("MyM2Test2",
            "MyM2Test3",
            "MyM5Test2",
            "MyM5Test3",
            "MyM6_60Test2",
            "MyM6_60Test3",
            "MyM6_61Test2",
            "MyM6_61Test3",
            "MyM7Test2");

    private Map<String, Collection<Integer>> partialSuccessTests = new HashMap<String, Collection<Integer>>() {{
        put("MyM4_2Test", Collections.singletonList(1));
        put("MyM7_2Test", Arrays.asList(1, 3));
    }};

    @Test
    public void testUserExceptionSupport1() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(FILE_NAME);
        engineFactory.setExecutionMode(false);
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        final CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();

        assertFalse("There are compilation errors in test", compiledOpenClass.hasErrors());

        IOpenClass openClass = compiledOpenClass.getOpenClass();
        Object target = openClass.newInstance(env);

        for (IOpenMethod method : openClass.getDeclaredMethods()) {
            if (method instanceof TestSuiteMethod) {
                String name = method.getName();

                @SuppressWarnings("unchecked")
                TestUnitsResults res = (TestUnitsResults) method.invoke(target, new Object[0], env);
                if (successfulTests.contains(name)) {
                    assertEquals("Test '" + name + "' must be successful", 0, res.getNumberOfFailures());
                } else if (failedTests.contains(name)) {
                    assertEquals("Test '" + name + "' must fail completely", res.getNumberOfTestUnits(), res.getNumberOfFailures());
                } else {
                    Collection<Integer> successfulRows = partialSuccessTests.get(name);
                    assertNotNull("Expectation of test '" + name + "' not described.", successfulRows);
                    ArrayList<ITestUnit> testUnits = res.getTestUnits();
                    for (int i = 0; i < testUnits.size(); i++) {
                        if (successfulRows.contains(i)) {
                            assertEquals(TestStatus.TR_OK, testUnits.get(i).getResultStatus());
                        } else {
                            assertEquals(TestStatus.TR_NEQ, testUnits.get(i).getResultStatus());
                        }
                    }
                }
            }
        }
    }

}
