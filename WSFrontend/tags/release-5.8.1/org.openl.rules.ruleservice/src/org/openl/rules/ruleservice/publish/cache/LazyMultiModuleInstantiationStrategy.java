package org.openl.rules.ruleservice.publish.cache;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.DependencyManager;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.instantiation.InitializingListener;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.cache.ModuleInfoGatheringDependencyLoader.ModuleStatistics;

/**
 * Prebinds multimodule openclass and creates LazyMethod and LazyField that will
 * compile neccessary modules on demand.
 * 
 * @author pudalau
 */
public class LazyMultiModuleInstantiationStrategy extends RulesInstantiationStrategy {
    private static final Log LOG = LogFactory.getLog(LazyMultiModuleInstantiationStrategy.class);

    private LazyMultiModuleEngineFactory factory;
    private ClassLoader classLoader;
    private ModuleStatistics moduleStatistic;
    private Collection<Module> modules;
    private List<InitializingListener> listeners = new ArrayList<InitializingListener>();

    public LazyMultiModuleInstantiationStrategy(Collection<Module> modules, boolean executionMode,
            IDependencyManager dependencyManager) {
        super(null, executionMode, createDependencyManager(modules, executionMode, dependencyManager));
        this.modules = modules;
        moduleStatistic = ((ModuleInfoGatheringDependencyLoader) ((RulesProjectDependencyManager) getDependencyManager())
                .getDependencyLoaders().get(0)).getModuleStatistic();
    }

    public void addInitializingListener(InitializingListener listener) {
        listeners.add(listener);
    }

    public void removeInitializingListener(InitializingListener listener) {
        listeners.remove(listener);
    }

    private static IDependencyManager createDependencyManager(Collection<Module> modules, boolean executionMode,
            IDependencyManager dependencyManager) {
        RulesProjectDependencyManager multiModuleDependencyManager = new RulesProjectDependencyManager();
        multiModuleDependencyManager.setExecutionMode(executionMode);
        IDependencyLoader loader = new ModuleInfoGatheringDependencyLoader(modules);
        List<IDependencyLoader> dependencyLoaders = new ArrayList<IDependencyLoader>();
        dependencyLoaders.add(loader);
        if (dependencyManager instanceof DependencyManager) {
            dependencyLoaders.addAll(((DependencyManager) dependencyManager).getDependencyLoaders());
        }
        multiModuleDependencyManager.setDependencyLoaders(dependencyLoaders);
        return multiModuleDependencyManager;
    }

    @Override
    protected void forcedReset() {
    }

    @SuppressWarnings("deprecation")
    @Override
    protected ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = new SimpleBundleClassLoader(Thread.currentThread().getContextClassLoader());
            for (Module module : modules) {
                URL[] urls = module.getProject().getClassPathUrls();
                ((SimpleBundleClassLoader) classLoader).addClassLoader(module.getProject().getClassLoader(false));
                OpenLClassLoaderHelper.extendClasspath((SimpleBundleClassLoader) classLoader, urls);
            }
        }

        return classLoader;
    }

    @Override
    public Class<?> getServiceClass() {
        // Using project class loader for interface generation.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        } catch (Exception e) {
            LOG.error("Cannot resolve interface", e);
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    protected CompiledOpenClass compile(boolean useExisting) throws InstantiationException, IllegalAccessException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    protected Object instantiate(Class<?> rulesClass, boolean useExisting) throws InstantiationException,
            IllegalAccessException {

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
            factory = new LazyMultiModuleEngineFactory(modules);
            for (Module module : modules) {
                for (InitializingListener listener : listeners) {
                    listener.afterModuleLoad(module);
                }
            }
            factory.setDependencyManager(getDependencyManager());
            factory.setModuleStatistic(moduleStatistic);
        }

        return factory;
    }
}