package org.openl.rules.project.instantiation;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.DependencyManager;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

/**
 * Instantiation strategy that combines several modules into single rules
 * module.
 * 
 * Note: it works only in execution mode.
 * 
 * @author PUdalau
 * 
 */
public abstract class MultiModuleInstantiationStartegy extends CommonRulesInstantiationStrategy {
    private Collection<Module> modules;

    private List<InitializingListener> listeners = new ArrayList<InitializingListener>();

    public MultiModuleInstantiationStartegy(Collection<Module> modules, IDependencyManager dependencyManager) {
        this(modules, dependencyManager, null);
    }

    public MultiModuleInstantiationStartegy(Collection<Module> modules,
            IDependencyManager dependencyManager,
            ClassLoader classLoader) {
        // multimodule is only available for execution(execution mode == true)
        super(true, createDependencyManager(modules, dependencyManager), classLoader);
        this.modules = modules;
    }

    private static IDependencyManager createDependencyManager(Collection<Module> modules,
            IDependencyManager dependencyManager) {
        RulesProjectDependencyManager multiModuleDependencyManager = new RulesProjectDependencyManager();
        // multimodule is only available for execution(execution mode == true)
        multiModuleDependencyManager.setExecutionMode(true);
        IDependencyLoader loader = new RulesModuleDependencyLoader(modules);
        List<IDependencyLoader> dependencyLoaders = new ArrayList<IDependencyLoader>();
        if (dependencyManager instanceof DependencyManager) {
            dependencyLoaders.addAll(((DependencyManager) dependencyManager).getDependencyLoaders());
        }
        dependencyLoaders.add(loader);
        multiModuleDependencyManager.setDependencyLoaders(dependencyLoaders);
        return multiModuleDependencyManager;
    }

    protected List<InitializingListener> getInitializingListeners() {
        return listeners;
    }

    public void addInitializingListener(InitializingListener listener) {
        listeners.add(listener);
    }

    public void removeInitializingListener(InitializingListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Collection<Module> getModules() {
        return modules;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected ClassLoader initClassLoader() {
        SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(Thread.currentThread()
            .getContextClassLoader());
        for (Module module : modules) {
            URL[] urls = module.getProject().getClassPathUrls();
            classLoader.addClassLoader(module.getProject().getClassLoader(false));
            OpenLClassLoaderHelper.extendClasspath((SimpleBundleClassLoader) classLoader, urls);
        }
        return classLoader;
    }
    
    @Override
    public void setExternalParameters(Map<String, Object> parameters) {
        super.setExternalParameters(parameters);
        ((RulesProjectDependencyManager)getDependencyManager()).setExternalParameters(parameters);
    }

    /**
     * @return Special empty virtual {@link IOpenSourceCodeModule} with
     *         dependencies on all modules.
     */
    protected IOpenSourceCodeModule createVirtualSourceCodeModule() {
        List<IDependency> dependencies = new ArrayList<IDependency>();

        for (Module module : getModules()) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }

        Map<String, Object> params = new HashMap<String, Object>();
        if (getExternalParameters() != null) {
            params.putAll(getExternalParameters());
        }
        params.put("external-dependencies", dependencies);
        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }

    private IDependency createDependency(Module module) {
        return new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null));
    }
}
