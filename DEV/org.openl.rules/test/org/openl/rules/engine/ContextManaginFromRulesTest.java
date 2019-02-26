package org.openl.rules.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ContextManaginFromRulesTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/engine/TestContextManaginFromRules.xlsx";

    private static  final IOpenClass[] EMPTY_PARAMS = {};

    public ContextManaginFromRulesTest() {
        super(SRC);
    }

    @Test
    public void testContextModifying() {
        IOpenMethod testMethod = getCompiledOpenClass().getOpenClass().getMethod("modifyContextTest", EMPTY_PARAMS);
        Object instance = newInstance();
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setUsState(UsStatesEnum.DC);
        context.setLob("home");
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        env.setContext(context);
        Object res = testMethod.invoke(instance, new Object[] {}, env);
        assertNotNull(res);
        assertTrue((Boolean) res);
    }

    @Test
    public void testContextSetter() {
        IOpenMethod testMethod = getCompiledOpenClass().getOpenClass().getMethod("setContextTest", EMPTY_PARAMS);
        Object instance = newInstance();
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setUsState(UsStatesEnum.DC);
        context.setLob("home");
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        env.setContext(context);
        Object res = testMethod.invoke(instance, new Object[] {}, env);
        assertNotNull(res);
        assertTrue((Boolean) res);
    }

    @Test
    public void testCurrentContextGetter() {
        IOpenMethod testMethod = getCompiledOpenClass().getOpenClass().getMethod("getContextPropery",
                new IOpenClass[] { JavaOpenClass.STRING });
        Object instance = newInstance();
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setUsState(UsStatesEnum.MO);
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        env.setContext(context);
        assertEquals(UsStatesEnum.MO, testMethod.invoke(instance, new Object[] { "usState" }, env));
    }

    @Test
    public void testEmptyContextGetter() throws Exception {
        IOpenMethod testMethod = getCompiledOpenClass().getOpenClass().getMethod("emptyContextTest", EMPTY_PARAMS);
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        Object instance = newInstance();
        Object result = testMethod.invoke(instance, new Object[] {}, env);
        assertTrue(result instanceof IRulesRuntimeContext);
        BeanInfo info = Introspector.getBeanInfo(IRulesRuntimeContext.class);
        PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
        for (PropertyDescriptor descriptor : propertyDescriptors) {
            assertNull(descriptor.getReadMethod().invoke(result));
        }
    }

    @Test
    public void testContextRestoring() {
        IOpenMethod testMethod = getCompiledOpenClass().getOpenClass().getMethod("restoreContextTest", EMPTY_PARAMS);
        Object instance = newInstance();
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setUsState(UsStatesEnum.DC);
        context.setLob("home");
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        env.setContext(context);
        Object res = testMethod.invoke(instance, new Object[] {}, env);
        assertNotNull(res);
        assertTrue((Boolean) res);
    }

    @Test
    public void testTBasicContext() {
        IOpenMethod testMethod = getCompiledOpenClass().getOpenClass().getMethod("tbasicCaller", EMPTY_PARAMS);
        Object instance = newInstance();
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setUsState(UsStatesEnum.DC);
        context.setLob("home");
        IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        env.setContext(context);
        Object res = testMethod.invoke(instance, new Object[] {}, env);
        assertNotNull(res);
        assertTrue((Boolean) res);
    }
}
