package org.openl.rules.project.instantiation;

import java.io.File;
//import java.net.URL;
//import java.net.URLClassLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;

/**
 * The simplest {@link RulesInstantiationStrategyFactory} for module that
 * contains only Excel file.
 * 
 * @author PUdalau
 */
public class ApiBasedEngineFactoryInstantiationStrategy extends RulesInstantiationStrategy {
    private static final Log LOG = LogFactory.getLog(ApiBasedEngineFactoryInstantiationStrategy.class);
    private ApiBasedRulesEngineFactory factory;
    //private ClassLoader classLoader;

    public ApiBasedEngineFactoryInstantiationStrategy(Module module, boolean executionMode) {
        super(module, executionMode);
        getEngineFactory();
    }

    private ApiBasedRulesEngineFactory getEngineFactory() {
        if (factory == null) {
            File sourceFile = new File(getModule().getRulesRootPath().getPath());
            IOpenSourceCodeModule source = new FileSourceCodeModule(sourceFile, null);
            source.setParams(getModule().getProperties());
            
            factory = new ApiBasedRulesEngineFactory(source);
            factory.setExecutionMode(isExecutionMode());
        }
        return factory;
    }

    @Override
    protected void forcedReset() {
        super.forcedReset();
        factory.reset(true);
    }

//    @Override
//    protected ClassLoader getClassLoader() {
//        ClassLoader projectClassloader = getProjectClassLoader();
//        if (classLoader == null || projectClassloader != classLoader.getParent()) {
//            // For all of modules resolved as API we will use different class
//            // loaders with common project class loader
//            classLoader = new URLClassLoader(new URL[] {}, projectClassloader);
//        }
//        return classLoader;
//    }

//    private ClassLoader getProjectClassLoader() {
//        return super.getClassLoader();
//    }

    @Override
    public Class<?> getServiceClass() {
        // Using project class loader for interface generation.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        }catch (Exception e) {
            LOG.warn("Cannot resolve interface", e);
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    protected CompiledOpenClass compile(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
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
    protected Object instantiate(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
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
