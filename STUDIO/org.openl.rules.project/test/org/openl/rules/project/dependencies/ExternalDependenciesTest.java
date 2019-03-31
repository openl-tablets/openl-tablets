package org.openl.rules.project.dependencies;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Calendar;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.instantiation.ProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

public class ExternalDependenciesTest {

    @Test
    public void testDependencies1() throws Exception {
        ProjectEngineFactory<?> factory = new SimpleProjectEngineFactoryBuilder()
            .setProject("test-resources/dependencies/test1/module")
            .setExecutionMode(false)
            .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();

        Method method = interfaceClass.getMethod("hello1", new Class[] { int.class });
        Object res = method.invoke(factory.newInstance(), 10);

        assertEquals("Good Morning", res);
    }

    @Test
    public void testDependencies2() throws Exception {
        ProjectEngineFactory<?> factory = new SimpleProjectEngineFactoryBuilder()
            .setProject("test-resources/dependencies/test2/module")
            .setExecutionMode(false)
            .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();
        Object instance = factory.newInstance();

        Method method = interfaceClass.getMethod("hello", new Class[] { int.class });
        Object res = method.invoke(instance, 10);

        assertEquals("Good Morning", res);

        // Get policy profile.
        //
        method = interfaceClass.getMethod("getPolicyProfile4", new Class[] {});
        res = method.invoke(instance, new Object[] {});
        Object policy = ((Object[]) res)[0];

        method = interfaceClass.getMethod("processPolicy", new Class[] { policy.getClass() });
        res = method.invoke(instance, new Object[] { policy });

        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(Double.valueOf(-20),
            Double.valueOf(((DoubleValue) spreadsheetResult.getFieldValue("$Value$Score")).getValue()));
        assertEquals(Double.valueOf(2270),
            Double.valueOf(((DoubleValue) spreadsheetResult.getFieldValue("$Value$Premium")).getValue()));
    }

    @Test
    public void testDependencies3() throws Exception {
        ProjectEngineFactory<?> factory = new SimpleProjectEngineFactoryBuilder()
            .setProject("test-resources/dependencies/test3/module")
            .setExecutionMode(false)
            .setProvideRuntimeContext(true)
            .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();
        Object instance = factory.newInstance();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setLob("dependency2");

        // Get policy profile.
        //
        Method method = interfaceClass.getMethod("getPolicyProfile4", new Class[] { IRulesRuntimeContext.class });
        Object res = method.invoke(instance, new Object[] { context });
        Object policy = ((Object[]) res)[0];

        method = interfaceClass.getMethod("processPolicy",
            new Class[] { IRulesRuntimeContext.class, policy.getClass() });
        res = method.invoke(instance, new Object[] { context, policy });

        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(Double.valueOf(-20),
            Double.valueOf(((DoubleValue) spreadsheetResult.getFieldValue("$Value$Score")).getValue()));
        assertEquals(Double.valueOf(2270),
            Double.valueOf(((DoubleValue) spreadsheetResult.getFieldValue("$Value$Premium")).getValue()));

        context.setLob("main");

        res = method.invoke(instance, new Object[] { context, policy });

        spreadsheetResult = (SpreadsheetResult) res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(Double.valueOf(-20),
            Double.valueOf(((DoubleValue) spreadsheetResult.getFieldValue("$Value$Score")).getValue()));
        assertEquals(Double.valueOf(4970),
            Double.valueOf(((DoubleValue) spreadsheetResult.getFieldValue("$Value$Premium")).getValue()));
    }

    @Test
    public void testDependencies4() throws Exception {
        ProjectEngineFactory<?> factory = new SimpleProjectEngineFactoryBuilder()
            .setProject("test-resources/dependencies/test4/module/main")
            .setWorkspace("test-resources/dependencies/test4/module")
            .setExecutionMode(false)
            .setProvideRuntimeContext(true)
            .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();
        Object instance = factory.newInstance();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        // Get policy profile.
        //
        Method method = interfaceClass.getMethod("getPolicyProfile4", new Class[] { IRulesRuntimeContext.class });
        Object res = method.invoke(instance, new Object[] { context });
        Object policy = ((Object[]) res)[0];

        method = interfaceClass.getMethod("processPolicy",
            new Class[] { IRulesRuntimeContext.class, policy.getClass() });
        res = method.invoke(instance, new Object[] { context, policy });

        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(Double.valueOf(-20),
            Double.valueOf(((DoubleValue) spreadsheetResult.getFieldValue("$Value$Score")).getValue()));
        assertEquals(Double.valueOf(2270),
            Double.valueOf(((DoubleValue) spreadsheetResult.getFieldValue("$Value$Premium")).getValue()));

        // Creating current date value
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);

        // Setting current date in context, which will be used in dispatch
        context.setCurrentDate(calendar.getTime());

        method = interfaceClass.getMethod("getTestCars", new Class[] { IRulesRuntimeContext.class });
        res = method.invoke(instance, new Object[] { context });

        Object car = ((Object[]) res)[1];

        method = interfaceClass.getMethod("getTestAddresses", new Class[] { IRulesRuntimeContext.class });
        res = method.invoke(instance, new Object[] { context });

        Object address = ((Object[]) res)[4];

        method = interfaceClass.getMethod("getPriceForOrder",
            new Class[] { IRulesRuntimeContext.class, car.getClass(), int.class, address.getClass() });
        res = method.invoke(instance, new Object[] { context, car, 4, address });

        assertEquals(Double.valueOf(189050), Double.valueOf(((DoubleValue) res).getValue()));
    }
}
