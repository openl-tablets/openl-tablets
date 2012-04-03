package org.openl.rules.project.instantiation;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.io.File;
import java.util.ArrayList;

import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

public class ApiInstantiationTest {
//    @Test
//    public void testClassLoaders(){
//        ResolvingStrategy resolvingStrategy = new SimpleXlsResolvingStrategy();
//        File projectFolder = new File("test/resources/excel/");
//        assertTrue(resolvingStrategy.isRulesProject(projectFolder));
//        ProjectDescriptor descriptor = resolvingStrategy.resolveProject(projectFolder);
//        RulesInstantiationStrategy instantiationStrategyFirst = RulesInstantiationStrategyFactory.getStrategy(descriptor.getModules().get(0));
//        RulesInstantiationStrategy instantiationStrategySecond = RulesInstantiationStrategyFactory.getStrategy(descriptor.getModules().get(1));
//        assertTrue(instantiationStrategyFirst.getClassLoader().getParent() == instantiationStrategySecond.getClassLoader().getParent());
//        assertFalse(instantiationStrategyFirst.getClassLoader() == instantiationStrategySecond.getClassLoader());
//        //reload parent class loader
//        instantiationStrategyFirst.forcedReset();
//        // parent class loader now also will be used in the second class loader
//        assertTrue(instantiationStrategyFirst.getClassLoader().getParent() == instantiationStrategySecond.getClassLoader().getParent());
//        assertFalse(instantiationStrategyFirst.getClassLoader() == instantiationStrategySecond.getClassLoader());
//    }

    @Test
    public void testXlsWithErrors() throws ClassNotFoundException{
        ProjectDescriptor project = new ProjectDescriptor();
        project.setClasspath(new ArrayList<PathEntry>());
        project.setProjectFolder(new File("test/resources/excel/"));
        Module module = new Module();
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("test/resources/excel/Rules2.xls"));

        ApiBasedInstantiationStrategy strategy = new ApiBasedInstantiationStrategy(module, false, null);
        assertNull(strategy.getServiceClass());
        try {
            assertNotNull(strategy.compile());
        } catch (Exception e) {
//            e.printStackTrace();
            assertFalse(true);
        }
    }
}
