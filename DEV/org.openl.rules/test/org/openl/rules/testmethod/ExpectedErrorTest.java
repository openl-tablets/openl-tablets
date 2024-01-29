package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class ExpectedErrorTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testUserExceptionSupport1() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>("test/rules/testmethod/ExpectedErrorTest.xls");
        engineFactory.setExecutionMode(false);
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        final CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();

        assertFalse(compiledOpenClass.hasErrors(), "There are compilation errors in test");

        IOpenClass openClass = compiledOpenClass.getOpenClass();
        Object target = openClass.newInstance(env);

        IOpenMethod helloTest = openClass.getMethod("HelloTest", IOpenClass.EMPTY);
        TestUnitsResults res = (TestUnitsResults) helloTest.invoke(target, new Object[0], env);
        List<ITestUnit> testUnits = res.getTestUnits();

        assertEquals(TestStatus.TR_OK, testUnits.get(0).getResultStatus(), "Expected Good Evening");
        assertEquals(TestStatus.TR_OK, testUnits.get(1).getResultStatus(), "Expected user error 'Incorrect argument'");
        assertEquals(TestStatus.TR_NEQ, testUnits.get(2).getResultStatus(), "Expected user error comparison failure");
        assertEquals(TestStatus.TR_NEQ,
            testUnits.get(3).getResultStatus(),
            "Unexpected exception must be thrown. It cannot be compared with user error");
        assertEquals(TestStatus.TR_NEQ,
            testUnits.get(4).getResultStatus(),
            "Unexpected exception must be thrown. It cannot be compared with user error");
    }

}
