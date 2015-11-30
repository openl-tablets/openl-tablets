package org.openl.rules.indexer;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.rules.vm.SimpleRulesVM;

public class IndexTest {

    @Test
    public void testOk() throws NoSuchMethodException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("./test/rules/index/test1.xlsx");
        engineFactory.setExecutionMode(false);
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Assert.assertFalse(engineFactory.getCompiledOpenClass().hasErrors());
        IOpenClass openClass = engineFactory.getCompiledOpenClass().getOpenClass();
        Object target = openClass.newInstance(env);
        Collection<IOpenMethod> tests = new ArrayList<IOpenMethod>();
        for (IOpenMethod method : openClass.getDeclaredMethods()) {
            if (method instanceof TestSuiteMethod) {
                tests.add(method);
            }
        }
        for (IOpenMethod testMethod : tests) {
            TestUnitsResults res = (TestUnitsResults) testMethod.invoke(target, new Object[0], env);
            assertEquals("Failed test " + testMethod.getDisplayName(0), 0, res.getNumberOfFailures());
        }
    }

    @Test
    @Ignore
    public void testFail1() throws NoSuchMethodException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("./test/rules/index/test2.xlsx");
        engineFactory.setExecutionMode(false);
        Assert.assertTrue(engineFactory.getCompiledOpenClass().hasErrors());
    }

    @Test
    public void testFail2() throws NoSuchMethodException {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("./test/rules/index/test3.xlsx");
        engineFactory.setExecutionMode(false);
        Assert.assertTrue(engineFactory.getCompiledOpenClass().hasErrors());
    }
}
