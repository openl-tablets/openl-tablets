package org.openl.rules.ruleservice.publish.lazy;

import java.util.List;

import org.openl.CompiledOpenClass;
import org.openl.rules.ruleservice.core.DeploymentDescription;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

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
            throw new IllegalArgumentException("deploymentDescription can't be null!");
        }
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName can't be null");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        Cache cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        Element element = cache.get(key);
        if (element == null){
            return null;
        }
        return (CompiledOpenClass) element.getObjectValue();
    }

    public void putToCache(DeploymentDescription deploymentDescription,
            String dependencyName,
            CompiledOpenClass compiledOpenClass) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription can't be null!");
        }
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName can't be null");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        Element newElement = new Element(key, compiledOpenClass);
        Cache cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.put(newElement);
    }

    /**
     * Removes module from cache.
     * 
     * @param module Module
     */
    public void remove(DeploymentDescription deploymentDescription, String dependencyName) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription can't be null!");
        }
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyNAme can't be null");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        Cache cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.remove(key);
    }

    public void removeAll(DeploymentDescription deploymentDescription) {
        if (deploymentDescription == null){
            throw new IllegalArgumentException("deploymentDescription can't be null!");
        }
        Cache cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        @SuppressWarnings("unchecked")
        List<Key> keys = cache.getKeys();
        for (Key key : keys) {
            DeploymentDescription deployment = key.getDeploymentDescription();
            if (deploymentDescription.getName().equals(deployment.getName()) && deploymentDescription.getVersion()
                .equals(deployment.getVersion())) {
                cache.remove(key);
            }
        }
    }

    public void reset() {
        Cache cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.removeAll();
        cache.clearStatistics();
    }

    private static class Key {
        final String dependencyName;
        final DeploymentDescription deploymentDescription;

        public DeploymentDescription getDeploymentDescription() {
            return deploymentDescription;
        }
        
        public Key(DeploymentDescription deploymentDescription, String dependencyName) {
            if (deploymentDescription == null) {
                throw new IllegalArgumentException("deploymentDescription can't be null!");
            }
            if (dependencyName == null) {
                throw new IllegalArgumentException("dependencyName can't be null");
            }
            this.deploymentDescription = deploymentDescription;
            this.dependencyName = dependencyName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 31;
            result = prime * result + ((deploymentDescription == null) ? 0 : deploymentDescription.hashCode());
            result = prime * result + ((dependencyName == null) ? 0 : dependencyName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (deploymentDescription == null) {
                if (other.deploymentDescription != null)
                    return false;
            } else if (!deploymentDescription.equals(other.deploymentDescription))
                return false;
            if (dependencyName == null) {
                if (other.dependencyName != null)
                    return false;
            } else if (!dependencyName.equals(other.dependencyName))
                return false;
            return true;
        }

    }
}
