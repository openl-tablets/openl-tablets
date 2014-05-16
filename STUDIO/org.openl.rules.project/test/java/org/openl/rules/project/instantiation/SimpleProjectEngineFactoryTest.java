package org.openl.rules.project.instantiation;

import java.lang.reflect.Method;

import org.junit.Test;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.springframework.util.Assert;

public class SimpleProjectEngineFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void failureWorkspaceTest() throws Exception {
        @SuppressWarnings("unused")
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactory<Object>("test/resources/project-engine/test1/third",
            "test/resources/project-engine/test1/third/third_rules/Third_Hello.xls");
    }

    @Test(expected = IllegalArgumentException.class)
    public void failureProjectArgumentTest() throws Exception {
        @SuppressWarnings("unused")
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactory<Object>(null,
            "test/resources/project-engine/test1");
    }

    @Test(expected = ProjectResolvingException.class)
    public void failureProjectTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactory<Object>("test/resources/project-engine",
            "test/resources/project-engine/test1");
        simpleProjectEngineFactory.newInstance();
    }

    @Test
    public void dynamicInterfaceTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactory<Object>("test/resources/project-engine/test1/third",
            "test/resources/project-engine/test1");
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass());
        Method sayHelloMethod = simpleProjectEngineFactory.getInterfaceClass().getMethod("sayHello");
        Assert.notNull(sayHelloMethod);
    }

    @Test
    public void dynamicInterfaceWithRuntimeContextTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactory<Object>("test/resources/project-engine/test1/third",
            "test/resources/project-engine/test1");
        simpleProjectEngineFactory.setProvideRuntimeContext(true);
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
        SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactory<SayHello>("test/resources/project-engine/test1/third",
            "test/resources/project-engine/test1");
        simpleProjectEngineFactory.setInterfaceClass(SayHello.class);
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass());
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass().equals(SayHello.class));
        Assert.isTrue(instance instanceof SayHello);
    }

    @Test(expected = RulesInstantiationException.class)
    public void staticInterfaceTestWithRuntimeContextFailureTest() throws Exception {
        SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactory<SayHello>("test/resources/project-engine/test1/third",
            "test/resources/project-engine/test1");
        simpleProjectEngineFactory.setInterfaceClass(SayHello.class);
        simpleProjectEngineFactory.setProvideRuntimeContext(true);
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
    }

    @Test
    public void staticInterfaceTestWithRuntimeContextTest() throws Exception {
        SimpleProjectEngineFactory<SayHelloWithRuntimeContext> simpleProjectEngineFactory = new SimpleProjectEngineFactory<SayHelloWithRuntimeContext>("test/resources/project-engine/test1/third",
            "test/resources/project-engine/test1");
        simpleProjectEngineFactory.setInterfaceClass(SayHelloWithRuntimeContext.class);
        simpleProjectEngineFactory.setProvideRuntimeContext(true);
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass().equals(SayHelloWithRuntimeContext.class));
        Assert.isTrue(instance instanceof SayHelloWithRuntimeContext);
    }

    @Test(expected = RulesInstantiationException.class)
    public void singleModuleTest() throws Exception {
        SimpleProjectEngineFactory<SayHello> simpleProjectEngineFactory = new SimpleProjectEngineFactory<SayHello>("test/resources/project-engine/test1/third",
            "test/resources/project-engine/test1");
        simpleProjectEngineFactory.setInterfaceClass(SayHello.class);
        simpleProjectEngineFactory.setSingleModuleMode(true);
        Object instance = simpleProjectEngineFactory.newInstance();
        Assert.notNull(instance);
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass());
        Assert.notNull(simpleProjectEngineFactory.getInterfaceClass().equals(SayHello.class));
        Assert.isTrue(instance instanceof SayHello);
    }

}
