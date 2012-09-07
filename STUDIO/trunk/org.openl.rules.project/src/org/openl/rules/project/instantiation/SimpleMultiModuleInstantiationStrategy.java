package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.SimpleEngineFactory;
import org.openl.runtime.AOpenLEngineFactory;

/**
 * The simplest way of multimodule instantiation strategy. There will be created
 * virtual module that depends on each predefined module(means virtual module
 * will have dependency for each module).
 * 
 * @author PUdalau, Marat Kamalov
 * 
 */
public class SimpleMultiModuleInstantiationStrategy extends MultiModuleInstantiationStartegy {
    private final Log log = LogFactory.getLog(SimpleMultiModuleInstantiationStrategy.class);

    private SimpleEngineFactory factory;

    public SimpleMultiModuleInstantiationStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        super(modules, dependencyManager);
    }

    public SimpleMultiModuleInstantiationStrategy(List<Module> modules) {
        this(modules, null);
    }

    @Override
    public void reset() {
        super.reset();
        factory = null;
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        // Using project class loader for interface generation.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        } catch (Exception e) {
            throw new RulesInstantiationException("Cannot resolve interface", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException {

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(rulesClass.getClassLoader());
        try {
            return getEngineFactory().makeInstance();
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
            factory = new SimpleEngineFactory(createVirtualSourceCodeModule(), AOpenLEngineFactory.DEFAULT_USER_HOME);// FIXME

            factory.setDependencyManager(getDependencyManager());
            factory.setInterfaceClass(serviceClass);
        }

        return factory;
    }

}