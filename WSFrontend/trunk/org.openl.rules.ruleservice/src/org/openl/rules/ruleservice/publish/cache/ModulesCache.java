package org.openl.rules.ruleservice.publish.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.openl.rules.project.instantiation.SingleModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Caches instantiation strategies for Modules. Uses EhCache. This is singleton
 * and thread safe class.
 * 
 * @author PUdalau,
 */
final class ModulesCache {

    private static final String CACHE_NAME = "modulesCache";

    private static class ModulesCacheHolder {
        public static final ModulesCache instance = new ModulesCache();
    }

    private ModulesCache() {
    }

    /**
     * Returns singleton ModulesCache
     * 
     * @return
     */
    public static ModulesCache getInstance() {
        return ModulesCacheHolder.instance;
    }

    private Cache cache = CacheManager.create().getCache(CACHE_NAME);

    public SingleModuleInstantiationStrategy getRulesInstantiationStrategyFromCache(Module module) {
        Element element = cache.get(module);
        if (element == null) {
            return null;
        }
        return (SingleModuleInstantiationStrategy) element.getObjectValue();
    }

    public void putToCache(Module module, SingleModuleInstantiationStrategy strategy) {
        if (module == null) {
            throw new IllegalArgumentException("module argument can't be null");
        }

        if (strategy == null) {
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
