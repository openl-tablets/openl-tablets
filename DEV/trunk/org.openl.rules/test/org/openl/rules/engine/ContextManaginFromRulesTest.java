package org.openl.rules.engine;

import static org.junit.Assert.*;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class ContextManaginFromRulesTest extends BaseOpenlBuilderHelper {

    private static String __src = "test/rules/engine/TestContextManaginFromRules.xlsx";

    public ContextManaginFromRulesTest() {
        super(__src);
    }

    @Test
    public void testContextModifying() {
        IOpenMethod testMethod = getJavaWrapper().getOpenClass().getMethod("modifyContextTest", new IOpenClass[] {});
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object instance = getJavaWrapper().getOpenClass().newInstance(env);
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setUsState(UsStatesEnum.DC);
        context.setLob("home");
        env.setContext(context);
        assertEquals(true, testMethod.invoke(instance, new Object[] {}, env));
    }

    @Test
    public void testContextSetter() {
        IOpenMethod testMethod = getJavaWrapper().getOpenClass().getMethod("setContextTest", new IOpenClass[] {});
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object instance = getJavaWrapper().getOpenClass().newInstance(env);
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setUsState(UsStatesEnum.DC);
        context.setLob("home");
        env.setContext(context);
        assertEquals(true, testMethod.invoke(instance, new Object[] {}, env));
    }

    @Test
    public void testCurrentContextGetter() {
        IOpenMethod testMethod = getJavaWrapper().getOpenClass().getMethod("getContextPropery",
            new IOpenClass[] { JavaOpenClass.STRING });
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object instance = getJavaWrapper().getOpenClass().newInstance(env);
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setUsState(UsStatesEnum.MO);
        env.setContext(context);
        assertEquals(UsStatesEnum.MO, testMethod.invoke(instance, new Object[] { "usState" }, env));
    }

    @Test
    public void testEmptyContextGetter() throws Exception {
        IOpenMethod testMethod = getJavaWrapper().getOpenClass().getMethod("emptyContextTest", new IOpenClass[] {});
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object instance = getJavaWrapper().getOpenClass().newInstance(env);
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
        IOpenMethod testMethod = getJavaWrapper().getOpenClass().getMethod("restoreContextTest", new IOpenClass[] {});
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        Object instance = getJavaWrapper().getOpenClass().newInstance(env);
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setUsState(UsStatesEnum.DC);
        context.setLob("home");
        env.setContext(context);
        assertEquals(true, testMethod.invoke(instance, new Object[] {}, env));
    }
}
