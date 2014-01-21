package org.openl.rules.webstudio.dependencies;

import java.util.*;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.instantiation.SingleModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
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
    @SuppressWarnings("unchecked")
    /**
     * Memory-sensitive cache.
     */
    private Map<Module, ModuleInstantiator> moduleInstantiators = new ReferenceMap(AbstractReferenceMap.WEAK,
            AbstractReferenceMap.SOFT);
    /**
     * Memory-sensitive cache.
     */
    @SuppressWarnings("unchecked")
    private Map<ProjectDescriptor, ModuleInstantiator> multiModuleInstantiators = new ReferenceMap(AbstractReferenceMap.WEAK,
            AbstractReferenceMap.SOFT);

    /**
     * Cache of dependent modules usages.
     * Key is dependent module name, value is a set of modules that depend on that module.
     * References to the modules are weak reference objects.
     */
    private Map<String, Set<Module>> dependencyUsages = new HashMap<String, Set<Module>>();

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
        moduleInstantiators.remove(module);
        multiModuleInstantiators.remove(module.getProject());
        removeModuleDependencies(module);
    }

    /**
     * Removes all cached instantiation strategies.
     */
    public void reset() {
        moduleInstantiators.clear();
        multiModuleInstantiators.clear();
        dependencyUsages.clear();
    }

    public boolean isOpenedAsSingleMode(Module module) {
        for (Module m : module.getProject().getModules()) {
            if (moduleInstantiators.containsKey(m)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOpenedAsMultiMode(Module module) {
        return multiModuleInstantiators.containsKey(module.getProject());
    }

    private ModuleInstantiator getFromCache(Module module, boolean singleModuleMode) {
        return singleModuleMode ? moduleInstantiators.get(module) : multiModuleInstantiators.get(module.getProject());
    }

    private void putToCache(Module module, boolean singleModuleMode, ModuleInstantiator instantiator) {
        if (singleModuleMode) {
            moduleInstantiators.put(module, instantiator);
        } else {
            multiModuleInstantiators.put(module.getProject(), instantiator);
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
            List<Module> modules = getModulesInWorkspace(module);
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

    private IDependencyManager getDependencyManager(Module module, boolean singleModuleMode) {
        WebStudioWorkspaceRelatedDependencyManager dependencyManager = dependencyManagerFactory.getDependencyManager(module, singleModuleMode);
        dependencyManager.addListener(new DependenciesCollectingHandler(module));
        return dependencyManager;
    }

    private boolean isSingleModuleModeStrategy(RulesInstantiationStrategy strategy) {
        return strategy instanceof SingleModuleInstantiationStrategy;
    }

    private List<Module> getModulesInWorkspace(Module module) {
        List<ProjectDescriptor> projectDescriptors = dependencyManagerFactory.getDependentProjects(module);

        List<Module> modules = new ArrayList<Module>();
        for (ProjectDescriptor projectDescriptor : projectDescriptors) {
            modules.addAll(projectDescriptor.getModules());
        }

        return modules;
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
