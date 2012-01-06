package org.openl.rules.project.dependencies;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.project.instantiation.ApiBasedEngineFactoryInstantiationStrategy;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.SimpleXlsResolvingStrategy;
import org.openl.rules.runtime.RulesFileDependencyLoader;

public class DependencyMethodDispatchingTest {
	
	private static final String MODULES_FOLDER = "test/resources/dependencies/testMethodDispatching";
        
    /**
     * Checks that one module includes another as dependency.
     * Both of them contains the identical methods by signatures, without dimension properties.
     * The expected result: both methods will be wrapped with dispatcher and ambigious method exception 
     * will be thrown at runtime. 
     */
    @Test
    public void testAmbigiousMethodException() {
        // AmbigiousMethodException can be retrieved in only the dispatching
        // mode based on methods selecting in java code
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY,
            OpenLSystemProperties.DISPATCHING_MODE_JAVA);
        ResolvingStrategy strategy = new SimpleXlsResolvingStrategy();
        ProjectDescriptor descr = strategy.resolveProject(new File(MODULES_FOLDER));

        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        
        RulesFileDependencyLoader loader1 = new RulesFileDependencyLoader();
        ResolvingRulesProjectDependencyLoader loader2 = new ResolvingRulesProjectDependencyLoader(MODULES_FOLDER);
        
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1, loader2));
        boolean executionMode = false;
        dependencyManager.setExecutionMode(executionMode);
        
        ApiBasedEngineFactoryInstantiationStrategy s = 
            new ApiBasedEngineFactoryInstantiationStrategy(descr.getModules().get(0), executionMode, dependencyManager);
        
        Class<?> interfaceClass = s.getServiceClass();
        Method method = null;
        try {
            method = interfaceClass.getMethod("hello1", new Class[]{int.class});
        } catch (Throwable e1) {
            fail("Method should exist.");
        }
            
        try {
            method.invoke(s.instantiate(ReloadType.NO), 10);
            fail("We are waiting for OpenlRuntimeException");
        } catch (Exception e) {        	
            assertTrue(e.getCause().getMessage().contains("Ambiguous method dispatch"));
        }
    }

}
