package org.openl.rules.project.instantiation;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.variation.VariationsPack;
import org.openl.rules.variation.VariationsResult;

public class SimpleProjectEngineFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void failureWorkspaceTest() throws Exception {
        @SuppressWarnings("unused")
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1/third/third_rules/Third_Hello.xls")
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failureProjectArgumentTest() throws Exception {
        @SuppressWarnings("unused")
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>()
            .setProject(null)
            .setWorkspace("test-resources/test1")
            .build();
    }

    @Test(expected = ProjectResolvingException.class)
    public void failureProjectTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>()
            .setProject("test-resources/project-engine")
            .setWorkspace("test-resources/test1")
            .build();
        simpleProjectEngineFactory.newInstance();
    }

    @Test
    public void dynamicInterfaceTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.assertNotNull(instance);
        Assert.assertNotNull(simpleProjectEngineFactory.getInterfaceClass());
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass().getMethod("sayHello");
        Assert.assertNotNull(sayHelloMethod);
    }

    @Test
    public void dynamicInterfaceWithRuntimeContextTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setProvideRuntimeContext(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.assertNotNull(instance);
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass()
            .getMethod("sayHello", IRulesRuntimeContext.class);
        Assert.assertNotNull(sayHelloMethod);
    }

    @Test
    public void dynamicInterfaceWithVariationTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setProvideVariations(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.assertNotNull(instance);
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass()
            .getMethod("sayHello", VariationsPack.class);
        Assert.assertNotNull(sayHelloMethod);
        Assert.assertTrue(sayHelloMethod.getReturnType().equals(VariationsResult.class));
    }

    @Test
    public void dynamicInterfaceWithVariationAndRuntimeContextTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setProvideVariations(true)
            .setProvideRuntimeContext(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.assertNotNull(instance);
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass()
            .getMethod("sayHello", IRulesRuntimeContext.class, VariationsPack.class);
        Assert.assertNotNull(sayHelloMethod);
        Assert.assertTrue(sayHelloMethod.getReturnType().equals(VariationsResult.class));
    }

    public static interface SayHello {
        String sayHello();
    }

    public static interface SayHelloWithRuntimeContext {
        String sayHello(IRulesRuntimeContext context);
    }

    public static interface SayHelloWithRuntimeContextAndVariation {
        String sayHello(IRulesRuntimeContext context);

        VariationsResult<String> sayHello(IRulesRuntimeContext context, VariationsPack variationsPack);
    }

    public static interface SayHelloWithVariation {
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
        Assert.assertNotNull(instance);
        Assert.assertNotNull(simpleProjectEngineFactory.getInterfaceClass());
        Assert.assertTrue(simpleProjectEngineFactory.getInterfaceClass().equals(SayHello.class));
        Assert.assertTrue(instance instanceof SayHello);
    }

    @Test(expected = RulesInstantiationException.class)
    public void staticInterfaceTestWithRuntimeContextFailureTest() throws Exception {
        SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHello>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setInterfaceClass(SayHello.class)
            .setProvideRuntimeContext(true)
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.assertNotNull(instance);
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
        Assert.assertNotNull(instance);
        Assert.assertTrue(simpleProjectEngineFactory.getInterfaceClass().equals(SayHelloWithRuntimeContext.class));
        Assert.assertTrue(instance instanceof SayHelloWithRuntimeContext);
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
        Assert.assertNotNull(instance);
        Assert.assertTrue(
            simpleProjectEngineFactory.getInterfaceClass().equals(SayHelloWithRuntimeContextAndVariation.class));
        Assert.assertTrue(instance instanceof SayHelloWithRuntimeContextAndVariation);
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
        Assert.assertNotNull(instance);
        Assert.assertTrue(simpleProjectEngineFactory.getInterfaceClass().equals(SayHelloWithVariation.class));
        Assert.assertTrue(instance instanceof SayHelloWithVariation);
    }

    @Test(expected = RulesInstantiationException.class)
    public void singleModuleTest() throws Exception {
        SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHello>()
            .setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1")
            .setInterfaceClass(SayHello.class)
            .setModule("someNotExistedModule")
            .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.assertNotNull(instance);
        Assert.assertNotNull(simpleProjectEngineFactory.getInterfaceClass());
        Assert.assertTrue(simpleProjectEngineFactory.getInterfaceClass().equals(SayHello.class));
        Assert.assertTrue(instance instanceof SayHello);
    }

}
