package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Test;
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

        assertFalse("There are compilation errors in test", compiledOpenClass.hasErrors());

        IOpenClass openClass = compiledOpenClass.getOpenClass();
        Object target = openClass.newInstance(env);

        IOpenMethod helloTest = openClass.getMethod("HelloTest", new IOpenClass[0]);
        TestUnitsResults res = (TestUnitsResults) helloTest.invoke(target, new Object[0], env);
        List<ITestUnit> testUnits = res.getTestUnits();

        assertEquals("Expected Good Evening", TestStatus.TR_OK, testUnits.get(0).getResultStatus());
        assertEquals("Expected user error 'Incorrect argument'", TestStatus.TR_OK, testUnits.get(1).getResultStatus());
        assertEquals("Expected user error comparison failure", TestStatus.TR_NEQ, testUnits.get(2).getResultStatus());
        assertEquals("Unexpected exception must be thrown. It cannot be compared with user error",
            TestStatus.TR_NEQ,
            testUnits.get(3).getResultStatus());
        assertEquals("Unexpected exception must be thrown. It cannot be compared with user error",
            TestStatus.TR_NEQ,
            testUnits.get(4).getResultStatus());
    }

}
