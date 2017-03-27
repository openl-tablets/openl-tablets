package org.openl.rules.project.instantiation;

import java.lang.reflect.Method;

import org.junit.Test;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.springframework.util.Assert;

public class SimpleProjectEngineFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void failureWorkspaceTest() throws Exception {
        @SuppressWarnings("unused")
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>().setProject("test-resources/test1/third")
            .setWorkspace("test-resources/test1/third/third_rules/Third_Hello.xls")
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failureProjectArgumentTest() throws Exception {
        @SuppressWarnings("unused")
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>().setProject(null)
                .setWorkspace("test-resources/test1")
                .build();
    }

    @Test(expected = ProjectResolvingException.class)
    public void failureProjectTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>().setProject("test-resources/project-engine")
                .setWorkspace("test-resources/test1")
                .build();
        simpleProjectEngineFactory.newInstance();
    }

    @Test
    public void dynamicInterfaceTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>().setProject("test-resources/test1/third")
                .setWorkspace("test-resources/test1")
                .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass());
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass().getMethod("sayHello");
        Assert.notNull(sayHelloMethod);
    }

    @Test
    public void dynamicInterfaceWithRuntimeContextTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<Object>().setProject("test-resources/test1/third")
                .setWorkspace("test-resources/test1").setProvideRuntimeContext(true)
                .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass().getMethod("sayHello",
            IRulesRuntimeContext.class);
        Assert.notNull(sayHelloMethod);
    }

    public static interface SayHello {
        public String sayHello();
    }

    public static interface SayHelloWithRuntimeContext {
        public String sayHello(IRulesRuntimeContext context);
    }

    @Test
    public void staticInterfaceTest() throws Exception {
        SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHello>().setProject("test-resources/test1/third")
                .setWorkspace("test-resources/test1").setInterfaceClass(SayHello.class)
                .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass());
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass().equals(SayHello.class));
        Assert.isTrue(instance instanceof SayHello);
    }

    @Test(expected = RulesInstantiationException.class)
    public void staticInterfaceTestWithRuntimeContextFailureTest() throws Exception {
        SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHello>().setProject("test-resources/test1/third")
                .setWorkspace("test-resources/test1").setInterfaceClass(SayHello.class).setProvideRuntimeContext(true)
                .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
    }

    @Test
    public void staticInterfaceTestWithRuntimeContextTest() throws Exception {
        SimpleProjectEngineFactory<SayHelloWithRuntimeContext> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHelloWithRuntimeContext>().setProject("test-resources/test1/third")
                .setWorkspace("test-resources/test1").setInterfaceClass(SayHelloWithRuntimeContext.class).setProvideRuntimeContext(true)
                .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass().equals(SayHelloWithRuntimeContext.class));
        Assert.isTrue(instance instanceof SayHelloWithRuntimeContext);
    }

    @Test(expected = RulesInstantiationException.class)
    public void singleModuleTest() throws Exception {
        SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<SayHello>().setProject("test-resources/test1/third")
                .setWorkspace("test-resources/test1").setInterfaceClass(SayHello.class).setModule("someNotExistedModule")
                .build();
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass());
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass().equals(SayHello.class));
        Assert.isTrue(instance instanceof SayHello);
    }

}
