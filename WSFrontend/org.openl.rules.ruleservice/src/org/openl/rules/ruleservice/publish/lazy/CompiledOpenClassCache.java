package org.openl.rules.ruleservice.publish.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.spi.CachingProvider;

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

    private static final String CACHE_NAME = "modulesCache";

    private volatile Cache<Key, CompiledOpenClass> modulesCache = null;

    private final Map<Key, Collection<LazyMember>> eventsMap = new HashMap<>();

    public void reset() {
        getModulesCache().removeAll();
    }

    private static class CompiledOpenClassHolder {
        static final CompiledOpenClassCache INSTANCE = new CompiledOpenClassCache();
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
        Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
        Objects.requireNonNull(dependencyName, "dependencyName cannot be null");
        Key key = new Key(deploymentDescription, dependencyName);
        Cache<Key, CompiledOpenClass> cache = getModulesCache();
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
            Cache<Key, CompiledOpenClass> cache = getInstance().getModulesCache();
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

    void registerEvent(DeploymentDescription deploymentDescription, String dependencyName, LazyMember event) {
        Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
        Objects.requireNonNull(dependencyName, "dependencyName cannot be null");
        Key key = new Key(deploymentDescription, dependencyName);
        synchronized (eventsMap) {
            Collection<LazyMember> events = eventsMap.computeIfAbsent(key, k -> new ArrayList<>());
            events.add(event);
        }
    }

    void removeAll(DeploymentDescription deploymentDescription) {
        Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
        Cache<Key, CompiledOpenClass> cache = getModulesCache();
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

    void clean(Key key) {
        synchronized (CompiledOpenClassCache.this.eventsMap) {
            Collection<LazyMember> events = CompiledOpenClassCache.this.eventsMap.remove(key);
            if (events != null) {
                for (LazyMember e : events) {
                    e.clearCachedMember();
                }
            }
        }
    }

    private Cache<Key, CompiledOpenClass> getModulesCache() {
        if (modulesCache == null) {
            synchronized (this) {
                if (modulesCache == null) {
                    ClassLoader classLoader = CompiledOpenClassCache.class.getClassLoader();
                    CachingProvider cachingProvider = Caching.getCachingProvider(classLoader);

                    CacheManager cacheManager = cachingProvider.getCacheManager();
                    modulesCache = cacheManager.getCache(CACHE_NAME, Key.class, CompiledOpenClass.class);
                    if (modulesCache == null) {
                        modulesCache = cacheManager.createCache(CACHE_NAME,
                            new MutableConfiguration<Key, CompiledOpenClass>()
                                .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                                .setStoreByValue(false)
                                .setTypes(Key.class, CompiledOpenClass.class));
                    }
                    modulesCache.registerCacheEntryListener(new MutableCacheEntryListenerConfiguration<>(FactoryBuilder
                        .factoryOf(CleanUpListener.class), null, false, true));
                }
            }
        }
        return modulesCache;
    }
}
