package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.rules.project.resolving.ProjectResolvingException;

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

    public interface SayHello {
        String sayHello();
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
        assertEquals(SayHello.class, simpleProjectEngineFactory.getInterfaceClass());
        assertInstanceOf(SayHello.class, instance);
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
        assertThrows(OpenlNotCheckedException.class, () -> {
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

    @Test
    public void invokeSimpleTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/multi-module-support/test2/project2")
                .setWorkspace("test-resources/multi-module-support/test2")
                .build();
        var instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        var interfaceClass = simpleProjectEngineFactory.getInterfaceClass();
        assertNotNull(interfaceClass);
        assertInstanceOf(interfaceClass, instance);
        assertEquals("Good Morning, World!", interfaceClass.getMethod("helloWorld", int.class).invoke(instance, 10));
        assertEquals(2, ((Object[]) interfaceClass.getMethod("getData1").invoke(instance)).length);
    }

    @Test
    public void invokeEmptyContextTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/multi-module-support/test2/project3")
                .setWorkspace("test-resources/multi-module-support/test2")
                .build();
        var instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        var interfaceClass = simpleProjectEngineFactory.getInterfaceClass();
        assertNotNull(interfaceClass);
        assertInstanceOf(interfaceClass, instance);
        assertEquals("World, Good Morning!", interfaceClass.getMethod("worldHello", IRulesRuntimeContext.class, int.class).invoke(instance, null, 10));
        assertEquals(3, ((Object[]) interfaceClass.getMethod("getData2", IRulesRuntimeContext.class).invoke(instance, new DefaultRulesRuntimeContext())).length);
    }

    @Test
    public void invokeContextTest() throws Exception {
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/multi-module-support/test3/project2")
                .setWorkspace("test-resources/multi-module-support/test3")
                .build();
        var instance = simpleProjectEngineFactory.newInstance();
        assertNotNull(instance);
        Class<?> serviceClass = instance.getClass();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setLob("lob2");

        Method method = serviceClass.getMethod("hello", IRulesRuntimeContext.class, int.class);
        Object result = method.invoke(instance, context, 10);
        assertEquals("Good Morning, World!", result);

        method = serviceClass.getMethod("getData1", IRulesRuntimeContext.class);
        result = method.invoke(instance, context);
        assertEquals(2, ((Object[]) result).length);

        method = serviceClass.getMethod("getData2", IRulesRuntimeContext.class);
        result = method.invoke(instance, context);
        assertEquals(3, ((Object[]) result).length);

        context.setLob("lob3");

        method = serviceClass.getMethod("hello", IRulesRuntimeContext.class, int.class);
        result = method.invoke(instance, context, 10);
        assertEquals("World, Good Morning!", result);

        method = serviceClass.getMethod("getData1", IRulesRuntimeContext.class);
        result = method.invoke(instance, context);
        assertEquals(2, ((Object[]) result).length);

        method = serviceClass.getMethod("getData2", IRulesRuntimeContext.class);
        result = method.invoke(instance, context);
        assertEquals(3, ((Object[]) result).length);

        context.setLob("lob1");

        method = serviceClass.getMethod("hello", IRulesRuntimeContext.class, int.class);
        result = method.invoke(instance, context, 10);
        assertEquals("Good Morning", result);

        method = serviceClass.getMethod("getData1", IRulesRuntimeContext.class);
        result = method.invoke(instance, context);
        assertEquals(2, ((Object[]) result).length);

        method = serviceClass.getMethod("getData2", IRulesRuntimeContext.class);
        result = method.invoke(instance, context);
        assertEquals(3, ((Object[]) result).length);
    }

}
