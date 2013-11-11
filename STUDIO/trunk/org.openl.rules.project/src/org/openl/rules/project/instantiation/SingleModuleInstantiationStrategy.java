package org.openl.rules.project.instantiation;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;

/**
 * Instantiation strategy for single module.
 * 
 * @author PUdalau
 */
public abstract class SingleModuleInstantiationStrategy extends CommonRulesInstantiationStrategy {

    /**
     * Root <code>Module</code> that is used as start point for Openl
     * compilation.
     */
    private Module module;

    public SingleModuleInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager) {
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

    @SuppressWarnings("deprecation")
    protected ClassLoader initClassLoader() {
        ClassLoader parent = getModule().getProject().getClassLoader(false);
        URL[] urls = getModule().getProject().getClassPathUrls();
        SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(parent);
        OpenLClassLoaderHelper.extendClasspath((SimpleBundleClassLoader) classLoader, urls);
        return classLoader;
    }

    @Override
    public Collection<Module> getModules() {
        return Collections.singleton(getModule());
    }
    
    protected Map<String, Object> prepareExternalParameters() {
        Map<String, Object> externalProperties = new HashMap<String, Object>();
        if (getModule().getProperties() != null) {
            externalProperties.putAll(getModule().getProperties());
        }
        if (getExternalParameters() != null) {
            externalProperties.putAll(getExternalParameters());
        }
        return externalProperties;
    }
}
