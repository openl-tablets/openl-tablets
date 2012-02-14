package org.openl.rules.project.instantiation;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;

/**
 * The simplest {@link RulesInstantiationStrategyFactory} for module that
 * contains only Excel file.
 * 
 * @author PUdalau
 * TODO: rename to ApiBasedInstantiationStrategy as current name is too long.
 */
public class ApiBasedEngineFactoryInstantiationStrategy extends RulesInstantiationStrategy {

    private static final Log LOG = LogFactory.getLog(ApiBasedEngineFactoryInstantiationStrategy.class);
    
    /**
     *  Rules engine factory for module that contains only Excel file.
     */
    private ApiBasedRulesEngineFactory factory;

    public ApiBasedEngineFactoryInstantiationStrategy(Module module, boolean executionMode, 
            IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
        initFactory();
    }
    
    public ApiBasedEngineFactoryInstantiationStrategy(Module module, boolean executionMode, 
            IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager, classLoader);
        initFactory();
    }

    private ApiBasedRulesEngineFactory initFactory() {
        if (factory == null) {
            File sourceFile = new File(getModule().getRulesRootPath().getPath());
            IOpenSourceCodeModule source = new FileSourceCodeModule(sourceFile, null);
            source.setParams(getModule().getProperties());
            
            factory = new ApiBasedRulesEngineFactory(source);
            factory.setExecutionMode(isExecutionMode());
            factory.setDependencyManager(getDependencyManager());
        }
        return factory;
    }

    @Override
    protected void forcedReset() {
        super.forcedReset();
        factory.reset(true);
    }

    @Override
    public Class<?> getServiceClass() {
        // Service class for current implementation will be class, generated at runtime by factory.
        
        // Using project class loader for interface generation.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return factory.getInterfaceClass();
        }catch (Exception e) {
            LOG.error("Cannot resolve interface.", e);
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    protected CompiledOpenClass compile(boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        
        // Ensure that compilation will be done in strategy classLoader
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            if (!useExisting) {
                factory.reset(false);
            }
            return factory.getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    protected Object instantiate(Class<?> rulesClass, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        
        // Ensure that instantiation will be done in strategy classLoader.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            if (!useExisting) {
                factory.reset(false);
            }
            return factory.makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
