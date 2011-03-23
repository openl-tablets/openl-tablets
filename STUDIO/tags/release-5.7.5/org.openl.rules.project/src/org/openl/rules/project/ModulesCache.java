package org.openl.rules.project;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Caches instantiation strategies for Modules.
 * 
 * @author PUdalau
 */
public class ModulesCache {
    @SuppressWarnings("unchecked")
    /**
     * Memory-sensitive cache.
     */
    private Map<Module, RulesInstantiationStrategy> moduleInstantiators = new ReferenceMap(AbstractReferenceMap.HARD,
            AbstractReferenceMap.SOFT);

    public Set<Module> getModules() {
        return moduleInstantiators.keySet();
    }

    /**
     * Gets cached instantiation strategy for the module or creates it in cache.
     * 
     * @param module Module
     * @return Instantiation strategy for the module.
     */
    public RulesInstantiationStrategy getInstantiationStrategy(Module module) {
        RulesInstantiationStrategy strategy = moduleInstantiators.get(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module);
            moduleInstantiators.put(module, strategy);
        }
        return strategy;
    }
    
    public RulesInstantiationStrategy getInstantiationStrategy(Module module, IDependencyManager dependencyManager) {
        RulesInstantiationStrategy strategy = moduleInstantiators.get(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module, dependencyManager);
            moduleInstantiators.put(module, strategy);
        }
        return strategy;
    }
   
    /**
     * Removes cached instantiation strategy for the module.
     * 
     * @param module Module
     */
    public void removeCachedModule(Module module) {
        moduleInstantiators.remove(module);
    }

    /**
     * Removes all cached instantiation strategies for the specified project.
     * 
     * @param project ProjectDescriptor
     */
    public void removeCachedProject(ProjectDescriptor project) {
        for (Module module : project.getModules()) {
            moduleInstantiators.remove(module);
        }
    }

    /**
     * Removes all cached instantiation strategies.
     */
    public void reset() {
        moduleInstantiators.clear();
    }
}
