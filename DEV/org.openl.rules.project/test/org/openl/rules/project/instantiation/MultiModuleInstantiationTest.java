package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;

public class MultiModuleInstantiationTest {

    @Test
    public void test1() throws Exception {

        var strategy = getInstantiationStrategy("test-resources/multi-module-support/test1");

        RuntimeContextInstantiationStrategyEnhancer enhancer = new RuntimeContextInstantiationStrategyEnhancer(
                strategy);

        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setLob("lob3");
        Method method = serviceClass.getMethod("driverRiskPremium", IRulesRuntimeContext.class, String.class);
        Object result = method.invoke(instance, context, "High Risk Driver");

        assertEquals(400.0, result);
    }

    @Test
    public void test2() throws Exception {

        var strategy = getInstantiationStrategy("test-resources/multi-module-support/test2");

        Class<?> serviceClass = strategy.getInstanceClass();
        Object instance = strategy.instantiate();

        Method method = serviceClass.getMethod("worldHello", int.class);
        Object result = method.invoke(instance, 10);
        assertEquals("World, Good Morning!", result);

        method = serviceClass.getMethod("helloWorld", int.class);
        result = method.invoke(instance, 10);
        assertEquals("Good Morning, World!", result);

        method = serviceClass.getMethod("getData1");
        result = method.invoke(instance);
        assertEquals(2, ((Object[]) result).length);

        method = serviceClass.getMethod("getData2");
        result = method.invoke(instance);
        assertEquals(3, ((Object[]) result).length);
    }

    public interface MultiModuleInterface {
        String worldHello(int hour);

        String helloWorld(int hour);
    }

    @Test
    public void testServiceClass() throws Exception {
        var strategy = getInstantiationStrategy("test-resources/multi-module-support/test2");
        strategy.setServiceClass(MultiModuleInterface.class);
        Object instantiate = strategy.instantiate();
        assertNotNull(instantiate);
        assertInstanceOf(MultiModuleInterface.class, instantiate);
    }

    @Test
    public void test3() throws Exception {

        var strategy = getInstantiationStrategy("test-resources/multi-module-support/test3");

        RuntimeContextInstantiationStrategyEnhancer enhancer = new RuntimeContextInstantiationStrategyEnhancer(
                strategy);

        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

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

    private static SimpleMultiModuleInstantiationStrategy getInstantiationStrategy(String path) throws Exception {
        var modules = new ArrayList<Module>();
        var projects = new ArrayList<ProjectDescriptor>();
        try (var dirs = Files.list(Path.of(path))) {
            dirs.forEach(dir -> {
                        try {
                            var project = ProjectResolver.getInstance().resolve(dir);
                            if (project != null) {
                                projects.add(project);
                                modules.addAll(project.getModules());
                            }
                        } catch (ProjectResolvingException e) {
                            fail("Failed to resolve project in " + dir, e);
                        }
                    }
            );

            var dependencyManager = new SimpleDependencyManager(projects, null, true, null);
            return new SimpleMultiModuleInstantiationStrategy(modules, dependencyManager, true);
        }

    }
}
