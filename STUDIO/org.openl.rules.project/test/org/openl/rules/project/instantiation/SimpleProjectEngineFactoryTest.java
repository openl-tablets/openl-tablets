package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;

public class SimpleProjectEngineFactoryTest {

    @Test
    public void failureWorkspaceTest() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            new SimpleProjectEngineFactoryBuilder<>().setProject("test-resources/test1/third")
                .setWorkspace("test-resources/test1/third/third_rules/Third_Hello.xls")
                .build();
        });
    }

    @Test
    public void failureProjectArgumentTest() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            new SimpleProjectEngineFactoryBuilder<>().setProject(null).setWorkspace("test-resources/test1").build();
        });
    }

    @Test
    public void failureProjectTest() throws Exception {
        assertThrows(ProjectResolvingException.class, () -> {
            SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/project-engine")
                .setWorkspace("test-resources/test1")
                .build();
            simpleProjectEngineFactory.newInstance();
        });
    }

    @Test
    public void dynamicInterfaceTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        assertNotNull(simpleProjectEngineFactory.getInterfaceClass());
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass().getMethod("sayHello");
        assertNotNull(sayHelloMethod);
    }

    @Test
    public void dynamicInterfaceWithRuntimeContextTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setProvideRuntimeContext(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass()
            .getMethod("sayHello", IRulesRuntimeContext.class);
        assertNotNull(sayHelloMethod);
    }

    @Test
    public void dynamicInterfaceWithVariationTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setProvideVariations(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass()
            .getMethod("sayHello", VariationsPack.class);
        assertNotNull(sayHelloMethod);
        assertEquals(sayHelloMethod.getReturnType(), VariationsResult.class);
    }

    @Test
    public void dynamicInterfaceWithVariationAndRuntimeContextTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setProvideVariations(true)
            .setProvideRuntimeContext(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass()
            .getMethod("sayHello", IRulesRuntimeContext.class, VariationsPack.class);
        assertNotNull(sayHelloMethod);
        assertEquals(sayHelloMethod.getReturnType(), VariationsResult.class);
    }

    public interface SayHello {
        String sayHello();
    }

    public interface SayHelloWithRuntimeContext {
        String sayHello(IRulesRuntimeContext context);
    }

    public interface SayHelloWithRuntimeContextAndVariation {
        String sayHello(IRulesRuntimeContext context);

        VariationsResult<String> sayHello(IRulesRuntimeContext context, VariationsPack variationsPack);
    }

    public interface SayHelloWithVariation {
        String sayHello();

        VariationsResult<String> sayHello(VariationsPack variationsPack);
    }

    @Test
    public void staticInterfaceTest() throws Exception {
        SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHello>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setInterfaceClass(SayHello.class)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        assertNotNull(simpleProjectEngineFactory.getInterfaceClass());
        assertEquals(simpleProjectEngineFactory.getInterfaceClass(), SayHello.class);
    }

    @Test
    public void staticInterfaceTestWithRuntimeContextFailureTest() throws Exception {
        assertThrows(RulesInstantiationException.class, () -> {
            SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHello>()
                .setProject("test-resources/test1/third")
                .setWorkspace("test-resources/test1")
                .setInterfaceClass(SayHello.class)
                .setProvideRuntimeContext(true)
                .build();
            Object instance = simpleProjectEngineFactory.newInstance();
            assertNotNull(instance);
        });
    }

    @Test
    public void staticInterfaceTestWithRuntimeContextTest() throws Exception {
        SimpleProjectEngineFactory<SayHelloWithRuntimeContext> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHelloWithRuntimeContext>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setInterfaceClass(SayHelloWithRuntimeContext.class)
            .setProvideRuntimeContext(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        assertEquals(simpleProjectEngineFactory.getInterfaceClass(), SayHelloWithRuntimeContext.class);
    }

    @Test
    public void staticInterfaceTestWithRuntimeContextAndVariationTest() throws Exception {
        SimpleProjectEngineFactory<SayHelloWithRuntimeContextAndVariation> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHelloWithRuntimeContextAndVariation>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setInterfaceClass(SayHelloWithRuntimeContextAndVariation.class)
            .setProvideRuntimeContext(true)
            .setProvideVariations(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        assertEquals(simpleProjectEngineFactory.getInterfaceClass(),
                SayHelloWithRuntimeContextAndVariation.class);
    }

    @Test
    public void staticInterfaceTestWithVariationTest() throws Exception {
        SimpleProjectEngineFactory<SayHelloWithVariation> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHelloWithVariation>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setInterfaceClass(SayHelloWithVariation.class)
            .setProvideVariations(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        assertEquals(simpleProjectEngineFactory.getInterfaceClass(), SayHelloWithVariation.class);
    }

    @Test
    public void dynamicInterfaceTest2() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/test1/third")
                .setProjectDependencies("test-resources/test1/first", "test-resources/test1/second")
                .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        assertNotNull(simpleProjectEngineFactory.getInterfaceClass());
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass().getMethod("sayHello");
        assertNotNull(sayHelloMethod);
    }

    @Test
    public void wrongProjectDependency() throws Exception {
        assertThrows(RulesInstantiationException.class, () -> {
            SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/test1/third")
                .setProjectDependencies("test-resources/test1")
                .build();
            simpleProjectEngineFactory.newInstance();
        });
    }

    @Test
    public void wrongProjectDependency2() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/test1/third")
                .setProjectDependencies("test-resources/test1/unknown")
                .build();
        });
    }

}
