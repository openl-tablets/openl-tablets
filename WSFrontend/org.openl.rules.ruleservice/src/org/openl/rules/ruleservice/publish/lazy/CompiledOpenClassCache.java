package org.openl.rules.ruleservice.publish.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.ehcache.Cache;
import org.ehcache.Cache.Entry;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;
import org.openl.CompiledOpenClass;
import org.openl.rules.ruleservice.core.DeploymentDescription;

/**
 * Caches compiled modules. Uses EhCache. This is singleton and thread safe implementation.
 *
 * @author Marat Kamalov
 */
public final class CompiledOpenClassCache {

    private static class CompiledOpenClassHolder {
        public static final CompiledOpenClassCache INSTANCE = new CompiledOpenClassCache();
    }

    private CompiledOpenClassCache() {
        Set<EventType> allEventTypes = new HashSet<>();
        allEventTypes.add(EventType.CREATED);
        allEventTypes.add(EventType.EVICTED);
        allEventTypes.add(EventType.REMOVED);
        allEventTypes.add(EventType.EXPIRED);
        OpenLEhCacheHolder.getInstance()
            .getModulesCache()
            .getRuntimeConfiguration()
            .registerCacheEventListener(event -> {
                synchronized (CompiledOpenClassCache.this.eventsMap) {
                    Collection<Event> events = CompiledOpenClassCache.this.eventsMap.get(event.getKey());
                    if (events != null) {
                        for (Event e : events) {
                            e.onEvent(event);
                        }
                    }
                }
            }, EventOrdering.ORDERED, EventFiring.SYNCHRONOUS, allEventTypes);
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
        Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
        Objects.requireNonNull(dependencyName, "dependencyName cannot be null");
        Key key = new Key(deploymentDescription, dependencyName);
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        return cache.get(key);
    }

    public void putToCache(DeploymentDescription deploymentDescription,
            String dependencyName,
            CompiledOpenClass compiledOpenClass) {
        Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
        Objects.requireNonNull(dependencyName, "dependencyName cannot be null");
        Key key = new Key(deploymentDescription, dependencyName);
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.put(key, compiledOpenClass);
    }

    private Map<Key, Collection<Event>> eventsMap = new HashMap<>();

    public void registerEvent(DeploymentDescription deploymentDescription, String dependencyName, Event event) {
        Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
        Objects.requireNonNull(dependencyName, "dependencyName cannot be null");
        Key key = new Key(deploymentDescription, dependencyName);
        synchronized (eventsMap) {
            Collection<Event> events = eventsMap.computeIfAbsent(key, k -> new ArrayList<>());
            events.add(event);
        }
    }

    public void removeAll(DeploymentDescription deploymentDescription) {
        Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
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
        synchronized (eventsMap) {
            eventsMap.entrySet()
                .removeIf(entry -> deploymentDescription.getName()
                    .equals(entry.getKey().getDeploymentDescription().getName()) && deploymentDescription.getVersion()
                        .equals(entry.getKey().getDeploymentDescription().getVersion()));
        }
    }

    public void reset() {
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.clear();
        synchronized (eventsMap) {
            eventsMap.clear();
        }
    }
}
