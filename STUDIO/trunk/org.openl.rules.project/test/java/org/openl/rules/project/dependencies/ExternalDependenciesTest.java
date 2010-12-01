package org.openl.rules.project.dependencies;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.ApiBasedEngineFactoryInstantiationStrategy;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.WrapperAdjustingInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.EclipseBasedResolvingStrategy;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.project.resolving.SimpleXlsResolvingStrategy;
import org.openl.rules.runtime.RulesFileDependencyLoader;

public class ExternalDependenciesTest {

    @Test
    public void testDependencies1() throws Exception {
        ResolvingStrategy strategy = new SimpleXlsResolvingStrategy();
        ProjectDescriptor descr = strategy.resolveProject(new File("test/resources/dependencies/module"));

        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        
        RulesFileDependencyLoader loader1 = new RulesFileDependencyLoader();
        RulesProjectDependencyLoader loader2 = new RulesProjectDependencyLoader("test/resources/dependencies/module");
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1, loader2));
        ApiBasedEngineFactoryInstantiationStrategy s = new ApiBasedEngineFactoryInstantiationStrategy(descr.getModules().get(0), false, dependencyManager);
        
        Class<?> interfaceClass = s.getServiceClass();
        Method method = interfaceClass.getMethod("hello", new Class[]{int.class});
        Object res = method.invoke(s.instantiate(ReloadType.NO), 10);
        
        System.out.println(res);
    }
}
