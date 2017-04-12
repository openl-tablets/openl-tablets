package org.openl.rules.ruleservice.publish.lazy;

import java.util.Iterator;

import org.ehcache.Cache;
import org.ehcache.Cache.Entry;
import org.openl.CompiledOpenClass;
import org.openl.rules.ruleservice.core.DeploymentDescription;

/**
 * Caches compiled modules. Uses EhCache. This is singleton and thread safe
 * implementation.
 * 
 * @author Marat Kamalov
 */
public final class CompiledOpenClassCache {

    private static class CompiledOpenClassHolder {
        public static final CompiledOpenClassCache INSTANCE = new CompiledOpenClassCache();
    }

    private CompiledOpenClassCache() {
    }

    /**
     * Returns singleton CompiledOpenClassCache
     * 
     * @return
     */
    public static CompiledOpenClassCache getInstance() {
        return CompiledOpenClassHolder.INSTANCE;
    }

    public CompiledOpenClass get(DeploymentDescription deploymentDescription, String dependencyName) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription must not be null!");
        }
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName must not be null");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        return cache.get(key);
    }

    public void putToCache(DeploymentDescription deploymentDescription,
            String dependencyName,
            CompiledOpenClass compiledOpenClass) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription must not be null!");
        }
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName must not be null");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.put(key, compiledOpenClass);
    }

    /**
     * Removes module from cache.
     * 
     * @param module Module
     */
    public void remove(DeploymentDescription deploymentDescription, String dependencyName) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription must not be null!");
        }
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyNAme must not be null!");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.remove(key);
    }

    public void removeAll(DeploymentDescription deploymentDescription) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription must not be null!");
        }
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        Iterator<Entry<Key, CompiledOpenClass>> itr = cache.iterator();
        while (itr.hasNext()) {
            Entry<Key, CompiledOpenClass> entry = itr.next();
            Key key = entry.getKey();
            DeploymentDescription deployment = key.getDeploymentDescription();
            if (deploymentDescription.getName().equals(deployment.getName()) && deploymentDescription.getVersion()
                .equals(deployment.getVersion())) {
                cache.remove(key);
            }
        }
    }

    public void reset() {
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.clear();
    }
}
