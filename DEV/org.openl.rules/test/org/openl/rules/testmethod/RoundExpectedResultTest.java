package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class RoundExpectedResultTest {

    private static final String FILE_NAME = "test/rules/testmethod/RoundExpectedResult.xls";

    private final Collection<String> successfulTests = Arrays.asList("MyM2Test",
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

    private final Collection<String> failedTests = Arrays.asList("MyM2Test2",
            "MyM2Test3",
            "MyM5Test2",
            "MyM5Test3",
            "MyM6_60Test2",
            "MyM6_60Test3",
            "MyM6_61Test2",
            "MyM6_61Test3",
            "MyM7Test2");

    private final Map<String, Collection<Integer>> partialSuccessTests = new HashMap<String, Collection<Integer>>() {
        {
            put("MyM4_2Test", Collections.singletonList(1));
            put("MyM7_2Test", Arrays.asList(1, 3));
        }
    };

    @Test
    public void testUserExceptionSupport1() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(FILE_NAME);
        engineFactory.setExecutionMode(false);
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        final CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();

        assertFalse(compiledOpenClass.hasErrors(), "There are compilation errors in test");

        IOpenClass openClass = compiledOpenClass.getOpenClass();
        Object target = openClass.newInstance(env);

        for (IOpenMethod method : openClass.getDeclaredMethods()) {
            if (method instanceof TestSuiteMethod) {
                String name = method.getName();

                @SuppressWarnings("unchecked")
                TestUnitsResults res = (TestUnitsResults) method.invoke(target, new Object[0], env);
                if (successfulTests.contains(name)) {
                    assertEquals(0, res.getNumberOfFailures(), "Test '" + name + "' must be successful");
                } else if (failedTests.contains(name)) {
                    assertEquals(res.getNumberOfTestUnits(),
                            res.getNumberOfFailures(),
                            "Test '" + name + "' must fail completely");
                } else {
                    Collection<Integer> successfulRows = partialSuccessTests.get(name);
                    assertNotNull(successfulRows, "Expectation of test '" + name + "' not described.");
                    List<ITestUnit> testUnits = res.getTestUnits();
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
