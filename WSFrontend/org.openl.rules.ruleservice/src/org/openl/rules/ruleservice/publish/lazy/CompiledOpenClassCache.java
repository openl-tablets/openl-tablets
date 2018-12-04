package org.openl.rules.ruleservice.publish.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.rules.ruleservice.core.DeploymentDescription;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListenerAdapter;

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

        OpenLEhCacheHolder.getInstance().getModulesCache().getCacheEventNotificationService().registerListener(
            new CacheEventListenerAdapter() {
                @Override
                public void notifyElementEvicted(Ehcache cache, Element element) {
                    process(element.getObjectKey());
                }

                @Override
                public void notifyElementExpired(Ehcache cache, Element element) {
                    process(element.getObjectKey());
                }

                @Override
                public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
                    process(element.getObjectKey());
                }

                @Override
                public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
                    process(element.getObjectKey());
                }

                @Override
                public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
                    process(element.getObjectKey());
                }

                void process(Object key) {
                    synchronized (CompiledOpenClassCache.this.eventsMap) {
                        Collection<Event> events = CompiledOpenClassCache.this.eventsMap.get(key);
                        if (events != null) {
                            for (Event e : events) {
                                e.onEvent();
                            }
                        }
                    }
                }
            });
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
            throw new IllegalArgumentException("dependencyName must not be null!");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        Cache cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        Element element = cache.get(key);
        if (element == null) {
            return null;
        }
        return (CompiledOpenClass) element.getObjectValue();
    }

    public void putToCache(DeploymentDescription deploymentDescription,
            String dependencyName,
            CompiledOpenClass compiledOpenClass) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription must not be null!");
        }
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName must not be null!");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        Element newElement = new Element(key, compiledOpenClass);
        Cache cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.put(newElement);
    }

    private Map<Key, Collection<Event>> eventsMap = new HashMap<>();

    public void registerEvent(DeploymentDescription deploymentDescription, String dependencyName, Event event) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription must not be null!");
        }
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName must not be null!");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        synchronized (eventsMap) {
            Collection<Event> events = eventsMap.get(key);
            if (events == null) {
                events = new ArrayList<>();
                eventsMap.put(key, events);
            }
            events.add(event);
        }
    }

    public void removeAll(DeploymentDescription deploymentDescription) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription must not be null!");
        }
        Cache cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        Iterator<Key> itr = cache.getKeys().iterator();
        while (itr.hasNext()) {
            Key key = itr.next();
            DeploymentDescription deployment = key.getDeploymentDescription();
            if (deploymentDescription.getName().equals(deployment.getName()) && deploymentDescription.getVersion()
                .equals(deployment.getVersion())) {
                cache.remove(key);
            }
        }
        synchronized (eventsMap) {
            Iterator<java.util.Map.Entry<Key, Collection<Event>>> eventsMapIterator = eventsMap.entrySet().iterator();
            while (eventsMapIterator.hasNext()) {
                java.util.Map.Entry<Key, Collection<Event>> entry = eventsMapIterator.next();
                if (deploymentDescription.getName()
                    .equals(entry.getKey().getDeploymentDescription().getName()) && deploymentDescription.getVersion()
                        .equals(entry.getKey().getDeploymentDescription().getVersion())) {
                    eventsMapIterator.remove();
                }
            }
        }
    }

    public void reset() {
        Cache cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.removeAll();
        synchronized (eventsMap) {
            eventsMap.clear();
        }
    }
}
