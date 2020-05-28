package org.openl.rules.ruleservice.publish.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.ehcache.Cache;
import org.ehcache.Cache.Entry;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;
import org.openl.CompiledOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches compiled modules. Uses EhCache. This is singleton and thread safe implementation.
 *
 * @author Marat Kamalov
 */
public final class CompiledOpenClassCache {

    private static final Logger LOG = LoggerFactory.getLogger(CompiledOpenClassCache.class);

    private static class CompiledOpenClassHolder {
        static final CompiledOpenClassCache INSTANCE = new CompiledOpenClassCache();
    }

    private CompiledOpenClassCache() {
        Set<EventType> allEventTypes = new HashSet<>();
        allEventTypes.add(EventType.CREATED);
        allEventTypes.add(EventType.EVICTED);
        allEventTypes.add(EventType.REMOVED);
        allEventTypes.add(EventType.EXPIRED);
        OpenLEhCache.getInstance().getModulesCache().getRuntimeConfiguration().registerCacheEventListener(event -> {
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
        Cache<Key, CompiledOpenClass> cache = OpenLEhCache.getInstance().getModulesCache();
        return cache.get(key);
    }

    static CompiledOpenClass compileToCache(RuleServiceDependencyManager dependencyManager,
                                            String dependencyName,
                                            DeploymentDescription deployment,
                                            Module module,
                                            ClassLoader classLoader) throws OpenLCompilationException {
        Objects.requireNonNull(deployment, "deploymentDescription cannot be null");
        Objects.requireNonNull(dependencyName, "dependencyName cannot be null");
        IPrebindHandler prebindHandler = LazyBinderMethodHandler.getPrebindHandler();
        try {
            LazyBinderMethodHandler.removePrebindHandler();
            RulesInstantiationStrategy rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                .getStrategy(module, true, dependencyManager, classLoader);
            rulesInstantiationStrategy.setServiceClass(EmptyInterface.class);
            Map<String, Object> parameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(
                dependencyManager.getExternalParameters(),
                Collections.singleton(module));
            rulesInstantiationStrategy.setExternalParameters(parameters);
            CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();

            Key key = new Key(deployment, dependencyName);
            Cache<Key, CompiledOpenClass> cache = OpenLEhCache.getInstance().getModulesCache();
            cache.put(key, compiledOpenClass);
            LOG.debug("Compiled lazy dependency (deployment='{}', version='{}', name='{}') is saved in cache.",
                deployment.getName(),
                deployment.getVersion().getVersionName(),
                dependencyName);
            return compiledOpenClass;
        } catch (Exception ex) {
            throw new OpenLCompilationException(String.format("Failed to load dependency '%s'.", dependencyName), ex);
        } finally {
            LazyBinderMethodHandler.setPrebindHandler(prebindHandler);
        }
    }

    private final Map<Key, Collection<Event>> eventsMap = new HashMap<>();

    void registerEvent(DeploymentDescription deploymentDescription, String dependencyName, Event event) {
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
        Cache<Key, CompiledOpenClass> cache = OpenLEhCache.getInstance().getModulesCache();
        for (Entry<Key, CompiledOpenClass> entry : cache) {
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
        Cache<Key, CompiledOpenClass> cache = OpenLEhCache.getInstance().getModulesCache();
        cache.clear();
        synchronized (eventsMap) {
            eventsMap.clear();
        }
    }
}
