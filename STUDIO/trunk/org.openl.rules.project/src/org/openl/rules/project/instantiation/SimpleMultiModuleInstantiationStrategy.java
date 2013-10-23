package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.InterfaceClassGeneratorImpl;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.runtime.AOpenLEngineFactory;

/**
 * The simplest way of multimodule instantiation strategy. There will be created
 * virtual module that depends on each predefined module(means virtual module
 * will have dependency for each module).
 * 
 * @author PUdalau
 * 
 */
public class SimpleMultiModuleInstantiationStrategy extends MultiModuleInstantiationStartegy {
    private final Log log = LogFactory.getLog(SimpleMultiModuleInstantiationStrategy.class);

    private RulesEngineFactory<?> engineFactory;

    public SimpleMultiModuleInstantiationStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        super(modules, dependencyManager);
    }

    public SimpleMultiModuleInstantiationStrategy(List<Module> modules) {
        this(modules, null);
    }

    @Override
    public void reset() {
        super.reset();
        engineFactory = null;
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
            return getEngineFactory().newEngineInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @SuppressWarnings("unchecked")
    private RulesEngineFactory<?> getEngineFactory() {
        Class<?> serviceClass = null;
        try {
            serviceClass = getServiceClass();
        } catch (ClassNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to get service class.", e);
            }
            serviceClass = null;
        }
        if (engineFactory == null || (serviceClass != null && !engineFactory.getInterfaceClass().equals(serviceClass))) {
            engineFactory = new RulesEngineFactory<Object>(createVirtualSourceCodeModule(),
                    AOpenLEngineFactory.DEFAULT_USER_HOME, (Class<Object>) serviceClass);// FIXME

            // Information for interface generation, if generation required.
            Collection<String> allIncludes = new HashSet<String>();
            Collection<String> allExcludes = new HashSet<String>();
            for (Module m : getModules()) {
                MethodFilter methodFilter = m.getMethodFilter();
                if (methodFilter != null) {
                    if (methodFilter.getIncludes() != null) {
                        allIncludes.addAll(methodFilter.getIncludes());
                    }
                    if (methodFilter.getExcludes() != null) {
                        allExcludes.addAll(methodFilter.getExcludes());
                    }
                }
            }
            if (!allIncludes.isEmpty() || !allExcludes.isEmpty()) {
                String[] includes = new String[] {};
                String[] excludes = new String[] {};
                includes = allIncludes.toArray(includes);
                excludes = allExcludes.toArray(excludes);
                engineFactory.setInterfaceClassGenerator(new InterfaceClassGeneratorImpl(includes, excludes));
            }
            engineFactory.setDependencyManager(getDependencyManager());
        }

        return engineFactory;
    }
}