package org.openl.rules.project.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.runtime.IEngineWrapper;

public class DependencyMethodDispatchingTest {

    private static final String AMBIGUOUS_METHOD_MESSAGE = "Ambiguous dispatch for method";

    /**
     * Checks that one module includes another as dependency. Both of them contains the identical methods by signatures,
     * without dimension properties. The expected result: both methods will be wrapped with dispatcher and ambigious
     * method exception will be thrown at runtime.
     */
    @Test
    public void testAmbiguousMethodException() throws Exception {
        // AmbiguousMethodException can be retrieved in only the dispatching
        // mode based on methods selecting in java code

         var factory = new SimpleProjectEngineFactoryBuilder()
                .setProject("test-resources/dependencies/testMethodDispatching")
                .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();
        Method method = interfaceClass.getMethod("hello1", int.class);

        try {
            method.invoke(factory.newInstance(), 10);
            fail("Expected OpenlRuntimeException");
        } catch (Exception e) {
            assertTrue(e.getCause().getMessage().contains(AMBIGUOUS_METHOD_MESSAGE));
        }
    }

    /**
     * Check that main module contains overloaded by property table. Dependency contains the invokable table(start) that
     * calls the table that is overloaded by property. Checks, that on invoke the table from dependency module will
     * work. As it was compiled separately, and know nothing about the overloaded table in main module.
     */
    @Test
    public void testMethodDispatching() throws Exception {

        var factory = new SimpleProjectEngineFactoryBuilder()
                .setProject("test-resources/dependencies/testMethodDispatching1")
                .build();
        factory.getCompiledOpenClass();

        Class<?> interfaceClass = factory.getInterfaceClass();

        Method method = interfaceClass.getMethod("start");

        var context = RulesRuntimeContextFactory.buildRulesRuntimeContext();

        // set the state from dependency module
        //
        context.setUsState(UsStatesEnum.AZ);

        var instance = factory.newInstance();
        ((IEngineWrapper) instance).getRuntimeEnv().setContext(context);
        assertEquals(2, method.invoke(instance));
    }

}
