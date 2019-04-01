package org.openl.rules.project.instantiation;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;

import org.junit.Test;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.runtime.IEngineWrapper;

public class ApiInstantiationTest {
    // @Test
    // public void testClassLoaders(){
    // ResolvingStrategy resolvingStrategy = new SimpleXlsResolvingStrategy();
    // File projectFolder = new File("test-resources/excel/");
    // assertTrue(resolvingStrategy.isRulesProject(projectFolder));
    // ProjectDescriptor descriptor = resolvingStrategy.resolveProject(projectFolder);
    // RulesInstantiationStrategy instantiationStrategyFirst =
    // RulesInstantiationStrategyFactory.getStrategy(descriptor.getModules().get(0));
    // RulesInstantiationStrategy instantiationStrategySecond =
    // RulesInstantiationStrategyFactory.getStrategy(descriptor.getModules().get(1));
    // assertTrue(instantiationStrategyFirst.getClassLoader().getParent() ==
    // instantiationStrategySecond.getClassLoader().getParent());
    // assertFalse(instantiationStrategyFirst.getClassLoader() == instantiationStrategySecond.getClassLoader());
    // //reload parent class loader
    // instantiationStrategyFirst.forcedReset();
    // // parent class loader now also will be used in the second class loader
    // assertTrue(instantiationStrategyFirst.getClassLoader().getParent() ==
    // instantiationStrategySecond.getClassLoader().getParent());
    // assertFalse(instantiationStrategyFirst.getClassLoader() == instantiationStrategySecond.getClassLoader());
    // }

    @Test
    public void testXlsWithErrors() throws ClassNotFoundException {
        ProjectDescriptor project = new ProjectDescriptor();
        project.setClasspath(new ArrayList<PathEntry>());
        project.setProjectFolder(new File("test-resources/excel/"));
        Module module = new Module();
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("test-resources/excel/Rules2.xls"));

        ApiBasedInstantiationStrategy strategy = new ApiBasedInstantiationStrategy(module, false, null);
        assertNull(strategy.getServiceClass());
        try {
            assertNotNull(strategy.compile());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testInterface() throws Exception {
        ProjectDescriptor project = new ProjectDescriptor();
        project.setClasspath(new ArrayList<PathEntry>());
        project.setProjectFolder(new File("test-resources/excel/"));
        Module module = new Module();
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("test-resources/excel/Rules.xls"));

        ApiBasedInstantiationStrategy strategy = new ApiBasedInstantiationStrategy(module, false, null);
        strategy.setServiceClass(ServiceClass.class);
        Object instance = strategy.instantiate();
        assertNotNull(instance);
        assertTrue(instance instanceof ServiceClass);
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        ((IEngineWrapper) instance).getRuntimeEnv().setContext(context);
        assertEquals("Good Evening, World!", ((ServiceClass) instance).hello1(19));
    }

    public interface ServiceClass {
        String hello1(int hour);
    }
}
