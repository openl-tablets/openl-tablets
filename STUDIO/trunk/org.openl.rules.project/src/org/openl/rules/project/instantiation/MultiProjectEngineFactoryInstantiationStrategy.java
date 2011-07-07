package org.openl.rules.project.instantiation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;

/** 
 * TODO: Rename to MultiProjectInstantiationStrategy. Too large name. 
 */
public class MultiProjectEngineFactoryInstantiationStrategy extends RulesInstantiationStrategy {
    private static final Log LOG = LogFactory.getLog(MultiProjectEngineFactoryInstantiationStrategy.class);

    private File root;
    private MultiProjectEngineFactory factory;
    private ClassLoader classLoader;
    
    private RulesProjectDependencyManager dependencyManager;
    private Collection<Module> modules;
    private List<InitializingListener> listeners = new ArrayList<InitializingListener>();

    public MultiProjectEngineFactoryInstantiationStrategy(File root) {
        super(null, true, null);        
        
        this.root = root;
    }
    
    public void addInitializingListener(InitializingListener listener) {
        listeners.add(listener);
    }
    
    public void removeInitializingListener(InitializingListener listener) {
        listeners.remove(listener);
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
    
    private void init() {
        modules = listModules(root);
        dependencyManager = new RulesProjectDependencyManager();
        
        dependencyManager.setExecutionMode(true);
        IDependencyLoader loader = new RulesModuleDependencyLoader(modules);
        dependencyManager.setDependencyLoaders(Arrays.asList(loader));
    }

    private MultiProjectEngineFactory getEngineFactory() {
        if (factory == null) {
            init();
            factory = new MultiProjectEngineFactory(modules);
            factory.setDependencyManager(dependencyManager);
        }
        
        return factory;
    }
    
    /**
     * Load modules from root folder.
     * 
     * @param root folder for all modules.
     * @return list of resolved modules.
     */
    private List<Module> listModules(File root) {

        List<Module> modules = new ArrayList<Module>();
        
        RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        projectResolver.setWorkspace(root.getAbsolutePath());
        List<ProjectDescriptor> projects = projectResolver.listOpenLProjects();

        for (ProjectDescriptor project : projects) {
            for (Module module : project.getModules()) {
                for(InitializingListener listener : listeners) {
                    listener.afterModuleLoad(module);
                }
                
                modules.add(module);
            }
        }
        
        return modules;
    }

}