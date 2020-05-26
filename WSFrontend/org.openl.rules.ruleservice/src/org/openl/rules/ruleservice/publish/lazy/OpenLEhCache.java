package org.openl.rules.ruleservice.publish.lazy;

import java.io.IOException;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.Configuration;
import org.ehcache.core.EhcacheManager;
import org.ehcache.xml.XmlConfiguration;
import org.openl.CompiledOpenClass;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public final class OpenLEhCache {

    private static final String CACHE_NAME = "modulesCache";
    private static final String OPENL_EHCACHE_FILE_NAME = "openl-ehcache.xml";

    private volatile Cache<Key, CompiledOpenClass> modulesCache = null;

    private OpenLEhCache() {
    }

    private static class OpenLEhCacheHolder {
        static final OpenLEhCache INSTANCE = new OpenLEhCache();
    }

    /**
     * Returns singleton OpenLEhCacheHolder
     *
     * @return
     */
    public static OpenLEhCache getInstance() {
        return OpenLEhCacheHolder.INSTANCE;
    }

    Cache<Key, CompiledOpenClass> getModulesCache() {
        if (modulesCache == null) {
            synchronized (this) {
                if (modulesCache == null) {
                    try {
                        CacheManager cacheManager = getCacheManager();
                        modulesCache = cacheManager.getCache(CACHE_NAME, Key.class, CompiledOpenClass.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return modulesCache;
    }

    private CacheManager cacheManager = null;

    private synchronized CacheManager getCacheManager() throws IOException {
        if (cacheManager == null) {
            PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = pathMatchingResourcePatternResolver.getResources(ResourceLoader.CLASSPATH_URL_PREFIX + OPENL_EHCACHE_FILE_NAME);
            if (resources.length == 0) {
                resources = pathMatchingResourcePatternResolver
                        .getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + OPENL_EHCACHE_FILE_NAME);
            }
            if (resources.length == 0) {
                throw new IllegalStateException(OPENL_EHCACHE_FILE_NAME + " is not found.");
            } else if (resources.length > 1) {
                throw new IllegalStateException(
                        String.format("Multiple %s exist in classpath.", OPENL_EHCACHE_FILE_NAME));
            }

            Configuration config = new XmlConfiguration(resources[0].getURL());
            cacheManager = new EhcacheManager(config);
            cacheManager.init();
        }
        return cacheManager;
    }

}
