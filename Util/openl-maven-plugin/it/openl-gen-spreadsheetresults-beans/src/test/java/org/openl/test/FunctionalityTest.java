package org.openl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class FunctionalityTest {

    @Test
    public void test() throws Exception {
        var factory = new SimpleProjectEngineFactoryBuilder<Object>()
                .setExecutionMode(false)
                .setProject("src/main/openl").build();

        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        final CompiledOpenClass compiledOpenClass = factory.getCompiledOpenClass();

        assertFalse(compiledOpenClass.hasErrors());

        IOpenClass openClass = compiledOpenClass.getOpenClass();
        Object target = openClass.newInstance(env);

        for (IOpenMethod method : openClass.getDeclaredMethods()) {
            if (method instanceof TestSuiteMethod) {
                TestUnitsResults res = (TestUnitsResults) method.invoke(target, new Object[0], env);
                final int numberOfFailures = res.getNumberOfFailures();
                assertEquals(0, numberOfFailures, "Failed test: " + res.getName());
            }
        }
    }

}
