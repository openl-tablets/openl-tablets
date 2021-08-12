package org.openl.rules.project.instantiation;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openl.dependency.IDependencyManager;
import org.openl.meta.DoubleValue;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;

public class MultiModuleInstantiationTest {

    @Test
    public void test1() throws Exception {

        File root = new File("test-resources/multi-module-support/test1");
        ProjectResolver projectResolver = ProjectResolver.getInstance();
        List<ProjectDescriptor> projects = projectResolver.resolve(root.listFiles());

        List<Module> modules = new ArrayList<>();
        for (ProjectDescriptor project : projects) {
            modules.addAll(project.getModules());
        }

        IDependencyManager dependencyManager = new SimpleDependencyManager(projects, null, true, null);

        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(modules,
            dependencyManager,
            true);

        RuntimeContextInstantiationStrategyEnhancer enhancer = new RuntimeContextInstantiationStrategyEnhancer(
            strategy);

        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setLob("lob3");
        Method method = serviceClass.getMethod("driverRiskPremium", IRulesRuntimeContext.class, String.class);
        Object result = method.invoke(instance, context, "High Risk Driver");

        assertEquals(new DoubleValue(400), result);
    }

    private List<ProjectDescriptor> listProjectsInFolder(File root) {
        ProjectResolver projectResolver = ProjectResolver.getInstance();
        return projectResolver.resolve(root.listFiles());
    }

    private List<Module> listModules(List<ProjectDescriptor> projects) {
        List<Module> modules = new ArrayList<>();
        for (ProjectDescriptor project : projects) {
            modules.addAll(project.getModules());
        }
        return modules;
    }

    @Test
    public void test2() throws Exception {

        File root = new File("test-resources/multi-module-support/test2");
        List<ProjectDescriptor> projects = listProjectsInFolder(root);
        IDependencyManager dependencyManager = new SimpleDependencyManager(projects, null, true, null);

        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(
            listModules(projects),
            dependencyManager,
            true);

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
        File root = new File("test-resources/multi-module-support/test2");
        List<ProjectDescriptor> projects = listProjectsInFolder(root);
        IDependencyManager dependencyManager = new SimpleDependencyManager(projects, null, true, null);
        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(
            listModules(projects),
            dependencyManager,
            true);
        strategy.setServiceClass(MultiModuleInterface.class);
        Object instantiate = strategy.instantiate();
        assertNotNull(instantiate);
        assertTrue(instantiate instanceof MultiModuleInterface);
    }

    @Test
    public void test3() throws Exception {

        File root = new File("test-resources/multi-module-support/test3");
        ProjectResolver projectResolver = ProjectResolver.getInstance();
        List<ProjectDescriptor> projects = projectResolver.resolve(root.listFiles());

        List<Module> modules = new ArrayList<>();
        for (ProjectDescriptor project : projects) {
            modules.addAll(project.getModules());
        }
        IDependencyManager dependencyManager = new SimpleDependencyManager(projects, null, true, null);

        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(modules,
            dependencyManager,
            true);

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

}
