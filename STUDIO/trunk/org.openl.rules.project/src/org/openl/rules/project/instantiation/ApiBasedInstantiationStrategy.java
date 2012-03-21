package org.openl.rules.project.instantiation;

import java.io.File;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.SimpleEngineFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;

/**
 * The simplest {@link RulesInstantiationStrategyFactory} for module that
 * contains only Excel file.
 * 
 * @author PUdalau
 */
public class ApiBasedInstantiationStrategy extends SingleModuleInstantiationStrategy {
  
    /**
     *  Rules engine factory for module that contains only Excel file.
     */
    private SimpleEngineFactory factory;

    public ApiBasedInstantiationStrategy(Module module, boolean executionMode, 
            IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
        initFactory();
    }
    
    public ApiBasedInstantiationStrategy(Module module, boolean executionMode, 
            IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager, classLoader);
        initFactory();
    }

    private SimpleEngineFactory initFactory() {
        if (factory == null) {
            File sourceFile = new File(getModule().getRulesRootPath().getPath());
            IOpenSourceCodeModule source = new FileSourceCodeModule(sourceFile, null);
            source.setParams(getModule().getProperties());
            
            factory = new SimpleEngineFactory(source, getModule().getProject().getProjectFolder().getAbsolutePath());
            factory.setExecutionMode(isExecutionMode());
            factory.setDependencyManager(getDependencyManager());
        }
        return factory;
    }
    
    @Override
    public void reset() {
        super.reset();
        factory.reset(true);
    }
    
    @Override
    public void forcedReset() {
        super.forcedReset();
        factory = null;
        initFactory();
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        // Service class for current implementation will be class, generated at runtime by factory.
        
        // Using project class loader for interface generation.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return factory.getInterfaceClass();
        }catch (Exception e) {
            throw new RulesInstantiationException("Cannot resolve interface.",e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException{
        
        // Ensure that compilation will be done in strategy classLoader
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return factory.getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException {
        
        // Ensure that instantiation will be done in strategy classLoader.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return factory.makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
