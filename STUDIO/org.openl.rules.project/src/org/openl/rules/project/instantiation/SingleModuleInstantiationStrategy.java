package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Instantiation strategy for single module.
 * 
 * @author PUdalau
 */
public abstract class SingleModuleInstantiationStrategy extends CommonRulesInstantiationStrategy {

    /**
     * Root <code>Module</code> that is used as start point for Openl compilation.
     */
    private Module module;

    public SingleModuleInstantiationStrategy(Module module,
            boolean executionMode,
            IDependencyManager dependencyManager) {
        this(module, executionMode, dependencyManager, null);
    }

    public SingleModuleInstantiationStrategy(Module module,
            boolean executionMode,
            IDependencyManager dependencyManager,
            ClassLoader classLoader) {
        super(executionMode, dependencyManager, classLoader);
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    // Single module strategy doesn't compile dependencies. Exception not required.
    @Override
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = initClassLoader();
        }
        return classLoader;
    }

    @Override
    protected ClassLoader initClassLoader() {
        ProjectDescriptor project = getModule().getProject();
        return new SimpleBundleClassLoader(project.getClassPathUrls(), Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Collection<Module> getModules() {
        return Collections.singleton(getModule());
    }

    protected Map<String, Object> prepareExternalParameters() {
        Map<String, Object> externalProperties = new HashMap<>();
        if (getModule().getProperties() != null) {
            externalProperties.putAll(getModule().getProperties());
        }
        if (getExternalParameters() != null) {
            externalProperties.putAll(getExternalParameters());
        }
        return externalProperties;
    }
}
