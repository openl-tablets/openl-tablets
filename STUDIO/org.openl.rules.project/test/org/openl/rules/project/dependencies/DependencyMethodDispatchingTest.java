package org.openl.rules.project.dependencies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.junit.Test;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.project.instantiation.ProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

public class DependencyMethodDispatchingTest {

    private static final String AMBIGUOUS_METHOD_MESSAGE = "Ambiguous dispatch for method";

    /**
     * Checks that one module includes another as dependency. Both of them contains the identical methods by signatures,
     * without dimension properties. The expected result: both methods will be wrapped with dispatcher and ambigious
     * method exception will be thrown at runtime.
     */
    @Test
    public void testAmbigiousMethodException() throws Exception {
        // AmbigiousMethodException can be retrieved in only the dispatching
        // mode based on methods selecting in java code

        ProjectEngineFactory<?> factory = new SimpleProjectEngineFactoryBuilder()
            .setProject("test-resources/dependencies/testMethodDispatching")
            .build();
        factory.getCompiledOpenClass();
        Class<?> interfaceClass = factory.getInterfaceClass();
        Method method = null;
        try {
            method = interfaceClass.getMethod("hello1", int.class);
        } catch (Throwable e1) {
            fail("Method should exist.");
        }

        try {
            method.invoke(factory.newInstance(), 10);
            fail("We are waiting for OpenlRuntimeException");
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
    public void testMethodDispatching() {

        ProjectEngineFactory<?> factory = new SimpleProjectEngineFactoryBuilder().setProvideRuntimeContext(true)
            .setProject("test-resources/dependencies/testMethodDispatching1")
            .build();

        Class<?> interfaceClass = null;
        try {
            factory.getCompiledOpenClass();
            interfaceClass = factory.getInterfaceClass();
        } catch (Exception e2) {
            fail("Should instantiate");
        }
        Method method = null;
        try {
            // get the method from dependency module for invoke
            //
            method = interfaceClass.getMethod("start", IRulesRuntimeContext.class);
        } catch (Throwable e1) {
            fail("Method should exist.");
        }

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();

        // set the state from dependency module
        //
        context.setUsState(UsStatesEnum.AZ);

        try {
            // check that method from dependency will be invoked
            //
            assertEquals(2, method.invoke(factory.newInstance(), context));
        } catch (Exception e) {
            fail("We should get the right result");
        }
    }

}
