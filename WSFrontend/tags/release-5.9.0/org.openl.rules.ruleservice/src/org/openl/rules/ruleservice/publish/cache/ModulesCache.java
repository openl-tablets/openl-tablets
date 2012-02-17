package org.openl.rules.ruleservice.publish.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Caches instantiation strategies for Modules. Uses EhCache. This is singleton and thread safe class.
 * 
 * @author PUdalau, MKamalov
 */
class ModulesCache {

    private static final String CACHE_NAME = "modulesCache";
    
    private static class ModulesCacheHolder{
        public static final ModulesCache instance = new ModulesCache(); 
    }

    private ModulesCache(){
    }
    
    /**
     * Returns singleton ModulesCache
     * @return
     */
    public static ModulesCache getInstance(){
        return ModulesCacheHolder.instance; 
    }
    
    private Cache cache = CacheManager.create().getCache(CACHE_NAME);

    private RulesInstantiationStrategy getRulesInstantiationStrategyFromCache(Module module) {
        Element element = cache.get(module);
        if (element == null) return null;
        return (RulesInstantiationStrategy) element.getObjectValue();
    }

    private void putToCache(Module module, RulesInstantiationStrategy strategy) {
        if (module == null){
            throw new IllegalArgumentException("module argument can't be null");
        }
        
        if (strategy == null){
            throw new IllegalArgumentException("strategy argument can't be null");
        }

        Element newElement = new Element(module, strategy);
        cache.put(newElement);
    }

    @SuppressWarnings("unchecked")
    public Set<Module> getModules() {
        List<Module> keys = cache.getKeys();
        return new HashSet<Module>(keys);
    }

    /**
     * Gets cached instantiation strategy for the module or creates it in cache.
     * 
     * @param module Module
     * @return Instantiation strategy for the module.
     */
    public RulesInstantiationStrategy getInstantiationStrategy(Module module) {
        RulesInstantiationStrategy strategy = getRulesInstantiationStrategyFromCache(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module);
            putToCache(module, strategy);
        }
        return strategy;
    }

    public RulesInstantiationStrategy getInstantiationStrategy(Module module, IDependencyManager dependencyManager) {
        RulesInstantiationStrategy strategy = getRulesInstantiationStrategyFromCache(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module, dependencyManager);
            putToCache(module, strategy);
        }
        return strategy;
    }

    public RulesInstantiationStrategy getInstantiationStrategy(Module module, boolean executionMode,
            IDependencyManager dependencyManager) {
        RulesInstantiationStrategy strategy = getRulesInstantiationStrategyFromCache(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module, executionMode, dependencyManager);
            putToCache(module, strategy);
        }
        return strategy;
    }

    public RulesInstantiationStrategy getInstantiationStrategy(Module module, boolean executionMode,
            IDependencyManager dependencyManager, ClassLoader classLoader) {
        RulesInstantiationStrategy strategy = getRulesInstantiationStrategyFromCache(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module, executionMode, dependencyManager, classLoader);
            putToCache(module, strategy);
        }
        return strategy;
    }

    /**
     * Removes cached instantiation strategy for the module.
     * 
     * @param module Module
     */
    public void removeCachedModule(Module module) {
        cache.remove(module);
    }

    /**
     * Removes all cached instantiation strategies for the specified project.
     * 
     * @param project ProjectDescriptor
     */
    public void removeCachedProject(ProjectDescriptor project) {
        for (Module module : project.getModules()) {
            cache.remove(module);
        }
    }

    /**
     * Removes all cached instantiation strategies.
     */
    public void reset() {
        cache.removeAll();
        cache.clearStatistics();
    }
   
}
