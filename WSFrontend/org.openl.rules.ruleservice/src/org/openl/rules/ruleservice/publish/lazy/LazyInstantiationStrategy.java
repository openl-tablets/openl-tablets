package org.openl.rules.ruleservice.publish.lazy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.openl.classloader.OpenLClassLoader;
import org.openl.rules.project.instantiation.MultiModuleInstantiationStartegy;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.runtime.InterfaceClassGeneratorImpl;

/**
 * Prebinds openclass and creates LazyMethod and LazyField that will compile neccessary modules on demand.
 *
 * @author pudalau, Marat Kamalov
 */
public class LazyInstantiationStrategy extends MultiModuleInstantiationStartegy {

    private LazyEngineFactory<?> engineFactory;
    private final DeploymentDescription deployment;

    public DeploymentDescription getDeployment() {
        return deployment;
    }

    public LazyInstantiationStrategy(DeploymentDescription deployment,
            Collection<Module> modules,
            RuleServiceDependencyManager dependencyManager) {
        super(modules, dependencyManager, true);
        this.deployment = Objects.requireNonNull(deployment, "deployment cannot be null");
    }

    LazyInstantiationStrategy(DeploymentDescription deployment,
            Collection<Module> modules,
            RuleServiceDependencyManager dependencyManager,
            ClassLoader classLoader) {
        super(modules, dependencyManager, classLoader, true);
        this.deployment = Objects.requireNonNull(deployment, "deployment cannot be null");
    }

    @Override
    public void reset() {
        super.reset();
        engineFactory = null;
    }

    private ClassLoader classLoader = null;

    @Override
    protected ClassLoader initClassLoader() {// Required for lazy
        if (classLoader == null) {
            ClassLoader openLClassLoader = new OpenLClassLoader(
                Thread.currentThread().getContextClassLoader());
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(openLClassLoader);
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
    protected RuleServiceDependencyManager getDependencyManager() {
        return (RuleServiceDependencyManager) super.getDependencyManager();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected LazyEngineFactory<?> getEngineFactory() {
        Class<?> serviceClass = getServiceClass();
        if (engineFactory == null) {
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

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        super.setServiceClass(serviceClass);
        if (engineFactory != null) {
            engineFactory.setInterfaceClass((Class) serviceClass);
        }
    }
}