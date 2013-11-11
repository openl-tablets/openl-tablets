package org.openl.rules.ruleservice.publish.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.MultiModuleInstantiationStartegy;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.InterfaceClassGeneratorImpl;

/**
 * Prebinds openclass and creates LazyMethod and LazyField that will
 * compile neccessary modules on demand.
 * 
 * @author pudalau, Marat Kamalov
 */
public class LazyInstantiationStrategy extends MultiModuleInstantiationStartegy {

    private final Log log = LogFactory.getLog(LazyInstantiationStrategy.class);

    private LazyEngineFactory<?> engineFactory;

    public LazyInstantiationStrategy(final Module module, IDependencyManager dependencyManager) {
        super(new ArrayList<Module>() {
            private static final long serialVersionUID = 1L;
            {
                add(module);
            }
        }, dependencyManager);
    }

    public LazyInstantiationStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        super(modules, dependencyManager);
    }

    public LazyInstantiationStrategy(Collection<Module> modules,
            IDependencyManager dependencyManager,
            ClassLoader classLoader) {
        super(modules, dependencyManager, classLoader);
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
            throw new RulesInstantiationException("Can't resolve interface", e);
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
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().newInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private LazyEngineFactory<?> getEngineFactory() {
        Class<?> serviceClass = null;
        try {
            serviceClass = getServiceClass();
        } catch (ClassNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to load service class.", e);
            }
            serviceClass = null;
        }
        if (engineFactory == null || (serviceClass != null && !engineFactory.getInterfaceClass().equals(serviceClass))) {
            engineFactory = new LazyEngineFactory(getModules(),
                getDependencyManager(),
                serviceClass,
                getExternalParameters());

            // Information for interface generation, if generation required.
            Collection<String> allIncludes = new HashSet<String>();
            Collection<String> allExcludes = new HashSet<String>();
            for (Module m : getModules()) {
                MethodFilter methodFilter = m.getMethodFilter();
                allIncludes.addAll(methodFilter.getIncludes());
                allExcludes.addAll(methodFilter.getExcludes());
            }
            if (!allIncludes.isEmpty() || !allExcludes.isEmpty()) {
                String[] includes = new String[] {};
                String[] excludes = new String[] {};
                includes = allIncludes.toArray(includes);
                excludes = allExcludes.toArray(excludes);
                engineFactory.setInterfaceClassGenerator(new InterfaceClassGeneratorImpl(includes, excludes));
            }
        }

        return engineFactory;
    }
}