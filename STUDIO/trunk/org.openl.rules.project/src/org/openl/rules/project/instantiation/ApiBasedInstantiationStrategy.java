package org.openl.rules.project.instantiation;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.BaseRulesFactory;
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
    private final Log log = LogFactory.getLog(ApiBasedInstantiationStrategy.class);
  
    /**
     *  Rules engine factory for module that contains only Excel file.
     */
    private SimpleEngineFactory factory;

    public ApiBasedInstantiationStrategy(Module module, boolean executionMode, 
            IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
    }
    
    public ApiBasedInstantiationStrategy(Module module, boolean executionMode, 
            IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager, classLoader);
    }

    
    @Override
    public void reset() {
        super.reset();
        if (factory != null) {
            getEngineFactory().reset(true);
        }
    }
    
    @Override
    public void forcedReset() {
        super.forcedReset();
        factory = null;
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        // Service class for current implementation will be class, generated at runtime by factory.
        
        // Using project class loader for interface generation.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
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
            return getEngineFactory().getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private SimpleEngineFactory getEngineFactory() {
        Class<?> serviceClass = null;
        try {
            serviceClass = getServiceClass();
        } catch (ClassNotFoundException e) {
            log.debug("Failed to get service class.", e);
            serviceClass = null;
        }
        if (factory == null || (serviceClass != null && !factory.getInterfaceClass().equals(serviceClass))) {
            File sourceFile = new File(getModule().getRulesRootPath().getPath());
            IOpenSourceCodeModule source = new FileSourceCodeModule(sourceFile, null);
            source.setParams(prepareExternalParameters());

            factory = new SimpleEngineFactory(source);
            
            //Information for interface generation, if generation required.
            Module m = getModule();
            MethodFilter methodFilter = m.getMethodFilter();
            if (methodFilter != null && (!methodFilter.getExcludes().isEmpty() || !methodFilter.getIncludes().isEmpty())) {
                String[] includes = new String[]{};
                String[] excludes = new String[]{};
                includes = methodFilter.getIncludes().toArray(includes);
                excludes = methodFilter.getExcludes().toArray(excludes);
                factory.setRulesFactory(new BaseRulesFactory(includes, excludes));
            }       
            
            factory.setExecutionMode(isExecutionMode());
            factory.setDependencyManager(getDependencyManager());
            factory.setInterfaceClass(serviceClass);
        }
        return factory;
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException {
        
        // Ensure that instantiation will be done in strategy classLoader.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
