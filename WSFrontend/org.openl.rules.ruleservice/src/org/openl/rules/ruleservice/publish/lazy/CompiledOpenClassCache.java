package org.openl.rules.ruleservice.publish.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.ehcache.Cache;
import org.ehcache.Cache.Entry;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;
import org.openl.CompiledOpenClass;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.types.IOpenMember;

/**
 * Caches compiled modules. Uses EhCache. This is singleton and thread safe
 * implementation.
 * 
 * @author Marat Kamalov
 */
public final class CompiledOpenClassCache {

    private static final Set<EventType> ALL_EVENT_TYPES;

    static {
        Set<EventType> allEventTypes = new HashSet<>();
        allEventTypes.add(EventType.CREATED);
        allEventTypes.add(EventType.EVICTED);
        allEventTypes.add(EventType.EXPIRED);
        allEventTypes.add(EventType.REMOVED);
        allEventTypes.add(EventType.EXPIRED);
        ALL_EVENT_TYPES = Collections.unmodifiableSet(allEventTypes);
    }

    private static class CompiledOpenClassHolder {
        public static final CompiledOpenClassCache INSTANCE = new CompiledOpenClassCache();
    }

    private CompiledOpenClassCache() {
        OpenLEhCacheHolder.getInstance().getModulesCache().getRuntimeConfiguration().registerCacheEventListener(
            new CacheEventListener<Key, CompiledOpenClass>() {
                @Override
                public void onEvent(CacheEvent<? extends Key, ? extends CompiledOpenClass> event) {
                    synchronized (CompiledOpenClassCache.this.lazyMembersMap) {
                        Collection<LazyMember<? extends IOpenMember>> lazyMembers = CompiledOpenClassCache.this.lazyMembersMap.get(event.getKey());  
                        for (LazyMember<? extends IOpenMember> lazyMember : lazyMembers) {
                            lazyMember.onCompiledOpenClassCacheModified();
                        }
                    }
                }
            },
            EventOrdering.ORDERED,
            EventFiring.SYNCHRONOUS,
            ALL_EVENT_TYPES);
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
            throw new IllegalArgumentException("dependencyName must not be null!");
        }
        Key key = new Key(deploymentDescription, dependencyName);
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.put(key, compiledOpenClass);
    }

    private Map<Key, Collection<LazyMember<? extends IOpenMember>>> lazyMembersMap = new HashMap<>();

    public void registerLazyMember(LazyMember<? extends IOpenMember> lazyMember) {
        if (lazyMember == null) {
            throw new IllegalArgumentException("lazyMember must not be null!");
        }
        Key key = new Key(lazyMember.getDeployment(), lazyMember.getModule().getName());
        synchronized (lazyMembersMap) {
            Collection<LazyMember<? extends IOpenMember>> lazyMembers = lazyMembersMap.get(key);
            if (lazyMembers == null) {
                lazyMembers = new ArrayList<>();
                lazyMembersMap.put(key, lazyMembers);
            }
            lazyMembers.add(lazyMember);
        }
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
        synchronized (lazyMembersMap) {
            Iterator<java.util.Map.Entry<Key, Collection<LazyMember<? extends IOpenMember>>>> lazyMembersMapIterator = lazyMembersMap
                .entrySet()
                .iterator();
            while (lazyMembersMapIterator.hasNext()) {
                java.util.Map.Entry<Key, Collection<LazyMember<? extends IOpenMember>>> entry = lazyMembersMapIterator
                    .next();
                if (deploymentDescription.getName()
                    .equals(entry.getKey().getDeploymentDescription().getName()) && deploymentDescription.getVersion()
                        .equals(entry.getKey().getDeploymentDescription().getVersion())) {
                    lazyMembersMapIterator.remove();
                }
            }
        }
    }

    public void reset() {
        Cache<Key, CompiledOpenClass> cache = OpenLEhCacheHolder.getInstance().getModulesCache();
        cache.clear();
        synchronized (lazyMembersMap) {
            lazyMembersMap.clear();
        }
    }
}
