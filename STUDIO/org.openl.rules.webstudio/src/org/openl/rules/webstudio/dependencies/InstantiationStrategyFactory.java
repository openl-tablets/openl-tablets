package org.openl.rules.webstudio.dependencies;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.instantiation.SingleModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;


/**
 * Caches instantiation strategies for Modules.
 * 
 * @author PUdalau
 */
public class InstantiationStrategyFactory {
    public static final String STUDIO_CACHE_MANAGER = "studioCacheManager";
    public static final String INSTANTIATION_STRATEGIES_CACHE = "instantiationStrategiesCache";

    private Cache cache = CacheManager.getCacheManager(STUDIO_CACHE_MANAGER).getCache(INSTANTIATION_STRATEGIES_CACHE);

    /**
     * Cache of dependent modules usages.
     * Key is dependent module name, value is a set of modules that depend on that module.
     * References to the modules are weak reference objects.
     */
    private Map<String, Set<Module>> dependencyUsages = new HashMap<String, Set<Module>>();
    /**
     * Cache of dependent project usages.
     * Key is dependent project name, value is a set of project that depend on that project.
     * References to the projects are weak reference objects.
     */
    private Map<String, Set<ProjectDescriptor>> projectDependencyUsages = new HashMap<String, Set<ProjectDescriptor>>();

    private WebStudio studio;
    private WebStudioDependencyManagerFactory dependencyManagerFactory;

    public InstantiationStrategyFactory(WebStudio studio) {
        this.studio = studio;
        this.dependencyManagerFactory = new WebStudioDependencyManagerFactory(studio);
    }

    /**
     * Gets cached instantiation strategy for the module or creates it in cache.
     * 
     * @param module Module
     * @param singleModuleMode if true - function will return single module instantiation strategy and multi module otherwise
     * @return Instantiation strategy for the module.
     */
    public ModuleInstantiator getInstantiationStrategy(Module module, boolean singleModuleMode) {
        ModuleInstantiator instantiator = getFromCache(module, singleModuleMode);

        if (instantiator != null && singleModuleMode != isSingleModuleModeStrategy(instantiator.getInstantiationStrategy())) {
            // Changed single/multi module mode
            removeCachedModule(module);
            instantiator = null;
        }

        if (!singleModuleMode && isOpenedAsSingleMode(module)) {
            // Changed from single to multi module mode - remove old cached modules
            for (Module m : module.getProject().getModules()) {
                removeCachedModule(m);
            }
            instantiator = null;
        }

        if (instantiator == null) {
            instantiator = createModuleInstantiator(module, singleModuleMode);
            putToCache(module, singleModuleMode, instantiator);
        }

        return instantiator;
    }

    /**
     * Removes cached instantiation strategy for the module.
     *
     * @param module Module
     */
    public void removeCachedModule(Module module) {
        cache.remove(new Key(this, module));
        cache.remove(new Key(this, module.getProject()));
        removeModuleDependencies(module);
        removeProjectDependencies(module.getProject());
    }

    /**
     * Removes all cached instantiation strategies.
     */
    public void reset() {
        if (cache.getStatus() == Status.STATUS_ALIVE) {
            for (Object key : cache.getKeys()) {
                Key k = (Key) key;
                InstantiationStrategyFactory factory = k.getFactory();
                if (factory == this || factory == null) {
                    // As far as cache.getKeys() returns the copy of key list, we can remove from cache while iterating the list
                    cache.remove(key);
                }
            }
        }
        dependencyUsages.clear();
        projectDependencyUsages.clear();
    }

    public boolean isOpenedAsSingleMode(Module module) {
        for (Module m : module.getProject().getModules()) {
            if (getFromCache(m, true) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isOpenedAsMultiMode(Module module) {
        return getFromCache(module, false) != null;
    }

    private ModuleInstantiator getFromCache(Module module, boolean singleModuleMode) {
        Key key = singleModuleMode ? new Key(this, module) : new Key(this, module.getProject());
        Element element = cache.get(key);

        if (element == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        ModuleInstantiator instantiator = ((SoftReference<ModuleInstantiator>) element.getObjectValue()).get();
        if (instantiator == null) {
            cache.remove(key);
            removeModuleDependencies(module);
        }

        return instantiator;
    }

    private void putToCache(Module module, boolean singleModuleMode, ModuleInstantiator instantiator) {
        Key key;
        ProjectDescriptor project = module.getProject();
        if (singleModuleMode) {
            key = new Key(this, module);
        } else {
            key = new Key(this, project);
        }
        cache.put(new Element(key, new SoftReference<ModuleInstantiator>(instantiator)));

        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor dependencyDescriptor : project.getDependencies()) {
                Set<ProjectDescriptor> projects = projectDependencyUsages.get(dependencyDescriptor.getName());
                if (projects == null) {
                    projects = Collections.newSetFromMap(new WeakHashMap<ProjectDescriptor, Boolean>());
                    projectDependencyUsages.put(dependencyDescriptor.getName(), projects);
                }
                projects.add(project);
            }
        }
    }

    private ModuleInstantiator createModuleInstantiator(Module module, boolean singleModuleMode) {
        IDependencyManager dependencyManager = getDependencyManager(module, singleModuleMode);

        Map<String, Object> externalParameters;
        RulesInstantiationStrategy strategy;

        if (singleModuleMode) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module, dependencyManager);
            externalParameters = studio.getSystemConfigManager().getProperties();
        } else {
            List<Module> modules = module.getProject().getModules();
            strategy = new SimpleMultiModuleInstantiationStrategy(modules, dependencyManager);

            externalParameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(
                    studio.getSystemConfigManager().getProperties(),
                    modules
            );

        }
        strategy.setExternalParameters(externalParameters);
        strategy.setServiceClass(WebStudioDependencyLoader.EmptyInterface.class);

        return new ModuleInstantiator(strategy, dependencyManager);
    }

    /**
     * Remove instantiation strategies, depending on specified module, from the cache
     *
     * @param module depending module
     */
    private void removeModuleDependencies(Module module) {
        Collection<Module> modulesUsingTheModule = dependencyUsages.get(module.getName());
        if (modulesUsingTheModule != null) {
            dependencyUsages.remove(module.getName());
            for (Module parentModule : new HashSet<Module>(modulesUsingTheModule)) {
                removeCachedModule(parentModule);
            }
        }
        for (Collection<Module> modules : dependencyUsages.values()) {
            modules.remove(module);
        }
    }

    /**
     * Remove instantiation strategies, depending on specified project, from the cache
     *
     * @param project depending project
     */
    private void removeProjectDependencies(ProjectDescriptor project) {
        Collection<ProjectDescriptor> projectsUsingTheProject = projectDependencyUsages.get(project.getName());
        if (projectsUsingTheProject != null) {
            projectDependencyUsages.remove(project.getName());
            for (ProjectDescriptor parentProject : new HashSet<ProjectDescriptor>(projectsUsingTheProject)) {
                cache.remove(new Key(this, parentProject));
            }
        }
        for (Collection<ProjectDescriptor> projects : projectDependencyUsages.values()) {
            projects.remove(project);
        }
    }

    private IDependencyManager getDependencyManager(Module module, boolean singleModuleMode) {
        WebStudioWorkspaceRelatedDependencyManager dependencyManager = dependencyManagerFactory.getDependencyManager(module, singleModuleMode);
        dependencyManager.addListener(new DependenciesCollectingHandler(module));
        return dependencyManager;
    }

    private boolean isSingleModuleModeStrategy(RulesInstantiationStrategy strategy) {
        return strategy instanceof SingleModuleInstantiationStrategy;
    }

    private class DependenciesCollectingHandler extends DefaultDependencyManagerListener {
        private final Module mainModule;

        public DependenciesCollectingHandler(Module mainModule) {
            this.mainModule = mainModule;
        }

        @Override
        public void onLoadDependency(IDependency dependency) {
            if (dependency.getType() == DependencyType.MODULE && !mainModule.getName().equals(dependency.getNode().getIdentifier())) {
                Set<Module> modules = dependencyUsages.get(dependency.getNode().getIdentifier());
                if (modules == null) {
                    modules = Collections.newSetFromMap(new WeakHashMap<Module, Boolean>());
                    dependencyUsages.put(dependency.getNode().getIdentifier(), modules);
                }
                modules.add(mainModule);
            }
        }
    }

    private static final class Key {
        private final WeakReference<InstantiationStrategyFactory> referenceToFactory;
        private final WeakReference<Module> referenceToModule;
        private final WeakReference<ProjectDescriptor> referenceToProject;
        private final int hash;

        private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();

        public Key(InstantiationStrategyFactory factory, Module module) {
            this.referenceToFactory = new WeakReference<InstantiationStrategyFactory>(factory, referenceQueue);
            this.referenceToModule = new WeakReference<Module>(module, referenceQueue);
            this.referenceToProject = new WeakReference<ProjectDescriptor>(null);

            hash = hash(factory, module, null);
        }

        public Key(InstantiationStrategyFactory factory, ProjectDescriptor project) {
            this.referenceToFactory = new WeakReference<InstantiationStrategyFactory>(factory);
            this.referenceToModule = new WeakReference<Module>(null);
            this.referenceToProject = new WeakReference<ProjectDescriptor>(project, referenceQueue);
            hash = hash(factory, null, project);
        }

        public InstantiationStrategyFactory getFactory() {
            return referenceToFactory.get();
        }

        private void purge() {
            Reference<?> reference = referenceQueue.poll();
            if (reference != null) {
                // This key not needed anymore - clear the memory
                referenceToFactory.clear();
                referenceToModule.clear();
                referenceToProject.clear();

                while (reference != null) {
                    reference = referenceQueue.poll();
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            purge();

            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            InstantiationStrategyFactory factory = referenceToFactory.get();
            InstantiationStrategyFactory keyFactory = key.referenceToFactory.get();

            Module module = referenceToModule.get();
            Module keyModule = key.referenceToModule.get();

            ProjectDescriptor project = referenceToProject.get();
            ProjectDescriptor keyProject = key.referenceToProject.get();

            if (factory == null && module == null && project == null) {
                // Content of this object GC-ed, it will be removed from cache.
                return false;
            }

            if (factory != null ? !factory.equals(keyFactory) : keyFactory != null) return false;
            if (module != null ? !module.equals(keyModule) : keyModule != null) return false;
            if (project != null ? !project.equals(keyProject) : keyProject != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            purge();

            return hash;
        }

        private int hash(InstantiationStrategyFactory factory, Module module, ProjectDescriptor project) {
            int result = factory != null ? factory.hashCode() : 0;
            result = 31 * result + (module != null ? module.hashCode() : 0);
            result = 31 * result + (project != null ? project.hashCode() : 0);
            return result;
        }
    }

    public static class ModuleInstantiator {
        private final RulesInstantiationStrategy instantiationStrategy;
        private final IDependencyManager dependencyManager;

        private ModuleInstantiator(RulesInstantiationStrategy instantiationStrategy, IDependencyManager dependencyManager) {
            this.instantiationStrategy = instantiationStrategy;
            this.dependencyManager = dependencyManager;
        }

        public RulesInstantiationStrategy getInstantiationStrategy() {
            return instantiationStrategy;
        }

        public IDependencyManager getDependencyManager() {
            return dependencyManager;
        }
    }
}
