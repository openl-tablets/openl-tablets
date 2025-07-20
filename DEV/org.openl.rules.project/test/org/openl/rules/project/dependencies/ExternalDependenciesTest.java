package org.openl.rules.project.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Calendar;

import org.junit.jupiter.api.Test;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.runtime.IEngineWrapper;

public class ExternalDependenciesTest {

    @Test
    public void testDependencies1() throws Exception {
        var factory = new SimpleProjectEngineFactoryBuilder()
                .setProject("test-resources/dependencies/test1/module")
                .setExecutionMode(false)
                .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();

        Method method = interfaceClass.getMethod("hello1", int.class);
        Object res = method.invoke(factory.newInstance(), 10);

        assertEquals("Good Morning", res);
    }

    @Test
    public void testDependencies2() throws Exception {
        var factory = new SimpleProjectEngineFactoryBuilder()
                .setProject("test-resources/dependencies/test2/module")
                .setExecutionMode(false)
                .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();
        Object instance = factory.newInstance();

        Method method = interfaceClass.getMethod("hello", int.class);
        Object res = method.invoke(instance, 10);

        assertEquals("Good Morning", res);

        // Get policy profile.
        //
        method = interfaceClass.getMethod("getPolicyProfile4");
        res = method.invoke(instance);
        Object policy = ((Object[]) res)[0];

        method = interfaceClass.getMethod("processPolicy", policy.getClass());
        res = method.invoke(instance, policy);

        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(-20.0, spreadsheetResult.getFieldValue("$Value$Score"));
        assertEquals(2270.0, spreadsheetResult.getFieldValue("$Value$Premium"));
    }

    @Test
    public void testDependencies3() throws Exception {
        var factory = new SimpleProjectEngineFactoryBuilder()
                .setProject("test-resources/dependencies/test3/module")
                .setExecutionMode(false)
                .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();
        Object instance = factory.newInstance();

        var context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        ((IEngineWrapper) instance).getRuntimeEnv().setContext(context);
        context.setLob("dependency2");

        // Get policy profile.
        //
        Method method = interfaceClass.getMethod("getPolicyProfile4");
        Object res = method.invoke(instance);
        Object policy = ((Object[]) res)[0];

        method = interfaceClass.getMethod("processPolicy", policy.getClass());
        res = method.invoke(instance, policy);

        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(-20.0, spreadsheetResult.getFieldValue("$Value$Score"));
        assertEquals(2270.0, spreadsheetResult.getFieldValue("$Value$Premium"));

        context.setLob("main");

        res = method.invoke(instance, policy);

        spreadsheetResult = (SpreadsheetResult) res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(-20.0, spreadsheetResult.getFieldValue("$Value$Score"));
        assertEquals(4970.0, spreadsheetResult.getFieldValue("$Value$Premium"));
    }

    @Test
    public void testDependencies4() throws Exception {
        var factory = new SimpleProjectEngineFactoryBuilder()
                .setProject("test-resources/dependencies/test4/module/main")
                .setWorkspace("test-resources/dependencies/test4/module")
                .setExecutionMode(false)
                .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();
        Object instance = factory.newInstance();

        var context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        ((IEngineWrapper) instance).getRuntimeEnv().setContext(context);
        // Get policy profile.
        //
        Method method = interfaceClass.getMethod("getPolicyProfile4");
        Object res = method.invoke(instance);
        Object policy = ((Object[]) res)[0];

        method = interfaceClass.getMethod("processPolicy", policy.getClass());
        res = method.invoke(instance, policy);

        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(-20.0, spreadsheetResult.getFieldValue("$Value$Score"));
        assertEquals(2270.0, spreadsheetResult.getFieldValue("$Value$Premium"));

        // Creating current date value
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);

        // Setting current date in context, which will be used in dispatch
        context.setCurrentDate(calendar.getTime());

        method = interfaceClass.getMethod("getTestCars");
        res = method.invoke(instance);

        Object car = ((Object[]) res)[1];

        method = interfaceClass.getMethod("getTestAddresses");
        res = method.invoke(instance);

        Object address = ((Object[]) res)[4];

        method = interfaceClass.getMethod("getPriceForOrder",
                car.getClass(),
                int.class,
                address.getClass());
        res = method.invoke(instance, car, 4, address);

        assertEquals(189050.0, res);
    }
}
