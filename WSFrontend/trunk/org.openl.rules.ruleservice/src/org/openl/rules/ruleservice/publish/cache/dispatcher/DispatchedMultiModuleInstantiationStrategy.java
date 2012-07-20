package org.openl.rules.ruleservice.publish.cache.dispatcher;

import java.util.Collection;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.InitializingListener;
import org.openl.rules.project.instantiation.MultiModuleInstantiationStartegy;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.model.Module;

/**
 * Multimodule with dispatching. Dispatching is defined by
 * {@link DispatchedData} and {@link DispatchedMethod} annotations.
 * 
 * @author PUdalau
 */
public class DispatchedMultiModuleInstantiationStrategy extends MultiModuleInstantiationStartegy {
    private DispatchedMultiModuleEngineFactory factory;

    public DispatchedMultiModuleInstantiationStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        super(modules, dependencyManager);
    }

    @Override
    public void reset() {
        super.reset();
        factory = null;
    }

    @Override
    public Class<?> getGeneratedRulesClass()throws RulesInstantiationException {
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
        } catch (ClassNotFoundException e) {
            throw new RulesInstantiationException("Faield to compile multimodule with dispatcher.",e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException{

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(rulesClass.getClassLoader());
        try {
            try {
                return getEngineFactory().makeInstance();
            } catch (ClassNotFoundException e) {
                throw new RulesInstantiationException("Faield to compile multimodule with dispatcher.",e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private DispatchedMultiModuleEngineFactory getEngineFactory() throws ClassNotFoundException {
        if (factory == null) {
            factory = new DispatchedMultiModuleEngineFactory(getModules(), getServiceClass());
            for (Module module : getModules()) {
                for (InitializingListener listener : getInitializingListeners()) {
                    listener.afterModuleLoad(module);
                }
            }
            factory.setDependencyManager(getDependencyManager());
            factory.setExternalParameters(getExternalParameters());
        }

        return factory;
    }
}