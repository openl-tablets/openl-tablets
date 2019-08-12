package org.openl.rules.ruleservice.publish.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.MultiModuleInstantiationStartegy;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.runtime.InterfaceClassGeneratorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prebinds openclass and creates LazyMethod and LazyField that will compile neccessary modules on demand.
 *
 * @author pudalau, Marat Kamalov
 */
public class LazyInstantiationStrategy extends MultiModuleInstantiationStartegy {

    private final Logger log = LoggerFactory.getLogger(LazyInstantiationStrategy.class);

    private LazyEngineFactory<?> engineFactory;
    private DeploymentDescription deployment;

    public DeploymentDescription getDeployment() {
        return deployment;
    }

    public LazyInstantiationStrategy(DeploymentDescription deployment,
            final Module module,
            IDependencyManager dependencyManager) {
        super(new ArrayList<Module>() {
            private static final long serialVersionUID = 1L;

            {
                add(module);
            }
        }, dependencyManager, true);
        Objects.requireNonNull(deployment, "deployment must not be null!");
        this.deployment = deployment;
    }

    public LazyInstantiationStrategy(DeploymentDescription deployment,
            Collection<Module> modules,
            IDependencyManager dependencyManager) {
        super(modules, dependencyManager, true);
        Objects.requireNonNull(deployment, "deployment must not be null!");
        this.deployment = deployment;
    }

    public LazyInstantiationStrategy(DeploymentDescription deployment,
            Collection<Module> modules,
            IDependencyManager dependencyManager,
            ClassLoader classLoader) {
        super(modules, dependencyManager, classLoader, true);
        Objects.requireNonNull(deployment, "deployment must not be null!");
        this.deployment = deployment;
    }

    @Override
    public void reset() {
        super.reset();
        engineFactory = null;
    }

    private ClassLoader classLoader = null;

    @Override
    protected ClassLoader initClassLoader() throws RulesInstantiationException {// Required for lazy
        if (classLoader == null) {
            ClassLoader simpleBundleClassLoader = new OpenLBundleClassLoader(Thread.currentThread().getContextClassLoader());
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(simpleBundleClassLoader);
                classLoader = getEngineFactory().getCompiledOpenClass().getClassLoader();
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
        return classLoader;
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        // Using project class loader for interface generation.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        } catch (Exception e) {
            throw new RulesInstantiationException("Failed to resolve interface", e);
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

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected LazyEngineFactory<?> getEngineFactory() {
        Class<?> serviceClass = null;
        try {
            serviceClass = getServiceClass();
        } catch (ClassNotFoundException e) {
            log.debug("Failed to load service class.", e);
            serviceClass = null;
        }
        if (engineFactory == null || (serviceClass != null && !engineFactory.getInterfaceClass()
            .equals(serviceClass))) {
            engineFactory = new LazyEngineFactory(getDeployment(),
                getModules(),
                getDependencyManager(),
                serviceClass,
                getExternalParameters());
            // Information for interface generation, if generation required.
            Collection<String> allIncludes = new HashSet<>();
            Collection<String> allExcludes = new HashSet<>();
            for (Module m : getModules()) {
                MethodFilter methodFilter = m.getMethodFilter();
                if (methodFilter != null) {
                    allIncludes.addAll(methodFilter.getIncludes());
                    allExcludes.addAll(methodFilter.getExcludes());
                }
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