package org.openl.rules.project.instantiation;

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
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.model.Module;

/** 
 * TODO: Rename to MultiProjectInstantiationStrategy. Too large name. 
 */
public class MultiModuleInstantiationStrategy extends RulesInstantiationStrategy {
    private static final Log LOG = LogFactory.getLog(MultiModuleInstantiationStrategy.class);

    private MultiProjectEngineFactory factory;
    private ClassLoader classLoader;
    
    private Collection<Module> modules;
    private List<InitializingListener> listeners = new ArrayList<InitializingListener>();

    public MultiModuleInstantiationStrategy(Collection<Module> modules, boolean executionMode, IDependencyManager dependencyManager) {
        super(null, executionMode, createDependencyManager(modules, executionMode, dependencyManager));
        this.modules = modules;
    }
    
    public void addInitializingListener(InitializingListener listener) {
        listeners.add(listener);
    }
    
    public void removeInitializingListener(InitializingListener listener) {
        listeners.remove(listener);
    }

    private static IDependencyManager createDependencyManager(Collection<Module> modules, boolean executionMode, IDependencyManager dependencyManager){
        RulesProjectDependencyManager multiModuleDependencyManager = new RulesProjectDependencyManager();
        multiModuleDependencyManager.setExecutionMode(executionMode);
        IDependencyLoader loader = new RulesModuleDependencyLoader(modules);
        List<IDependencyLoader> dependencyLoaders = new ArrayList<IDependencyLoader>();
        if (dependencyManager instanceof DependencyManager) {
            dependencyLoaders.addAll(((DependencyManager) dependencyManager).getDependencyLoaders());
        }
        dependencyLoaders.add(loader);
        multiModuleDependencyManager.setDependencyLoaders(dependencyLoaders);
        return multiModuleDependencyManager;
    }
    
    @Override
    protected void forcedReset() {
    }

    @Override
    protected ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = new SimpleBundleClassLoader(Thread.currentThread().getContextClassLoader());            
            for (Module module : modules) {
                URL[] urls = module.getProject().getClassPathUrls();
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
        }catch (Exception e) {
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
    
    private MultiProjectEngineFactory getEngineFactory() {
        if (factory == null) {
            factory = new MultiProjectEngineFactory(modules, getRulesClass());
            for (Module module : modules) {
                for (InitializingListener listener : listeners) {
                    listener.afterModuleLoad(module);
                }
            }
            factory.setDependencyManager(getDependencyManager());
        }
        
        return factory;
    }
}