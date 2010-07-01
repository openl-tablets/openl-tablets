package org.openl.rules.project.instantiation;

import static junit.framework.Assert.*;

import java.io.File;

import org.junit.Test;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.SimpleXlsResolvingStrategy;

public class ApiClassloadersTest {
    @Test
    public void test(){
        ResolvingStrategy resolvingStrategy = new SimpleXlsResolvingStrategy();
        File projectFolder = new File("test/resources/excel/");
        assertTrue(resolvingStrategy.isRulesProject(projectFolder));
        ProjectDescriptor descriptor = resolvingStrategy.resolveProject(projectFolder);
        RulesInstantiationStrategy instantiationStrategyFirst = RulesInstantiationStrategyFactory.getStrategy(descriptor.getModules().get(0));
        RulesInstantiationStrategy instantiationStrategySecond = RulesInstantiationStrategyFactory.getStrategy(descriptor.getModules().get(1));
        assertTrue(instantiationStrategyFirst.getClassLoader().getParent() == instantiationStrategySecond.getClassLoader().getParent());
        assertFalse(instantiationStrategyFirst.getClassLoader() == instantiationStrategySecond.getClassLoader());
        //reload parent class loader
        instantiationStrategyFirst.forcedReset();
        // parent class loader now also will be used in the second class loader
        assertTrue(instantiationStrategyFirst.getClassLoader().getParent() == instantiationStrategySecond.getClassLoader().getParent());
        assertFalse(instantiationStrategyFirst.getClassLoader() == instantiationStrategySecond.getClassLoader());
    }
}
