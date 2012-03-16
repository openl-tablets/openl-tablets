package org.openl.rules.ruleservice.publish.cache;

import java.util.Collection;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.InitializingListener;
import org.openl.rules.project.instantiation.MultiModuleInstantiationStartegy;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.model.Module;

/**
 * Prebinds multimodule openclass and creates LazyMethod and LazyField that will
 * compile neccessary modules on demand.
 * 
 * @author pudalau
 */
public class LazyMultiModuleInstantiationStrategy extends MultiModuleInstantiationStartegy {
    private LazyMultiModuleEngineFactory factory;

    public LazyMultiModuleInstantiationStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        super(modules, dependencyManager);
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
    public CompiledOpenClass compile() throws RulesInstantiationException{
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException{

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(rulesClass.getClassLoader());
        try {
            return getEngineFactory().makeInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private LazyMultiModuleEngineFactory getEngineFactory() {
        if (factory == null) {
            factory = new LazyMultiModuleEngineFactory(getModules());
            for (Module module : getModules()) {
                for (InitializingListener listener : getInitializingListeners()) {
                    listener.afterModuleLoad(module);
                }
            }
            factory.setDependencyManager(getDependencyManager());
        }

        return factory;
    }
}