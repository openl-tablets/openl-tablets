package org.openl.rules.ruleservice.publish.lazy;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public final class OpenLEhCacheHolder {

    private static final String CACHE_NAME = "modulesCache";
    private static final String OPENL_EHCACHE_FILE_NAME = "openl-ehcache.xml";

    private volatile Cache modulesCache = null;

    private OpenLEhCacheHolder() {
    }

    private static class OpenLEhCacheHolderHolder {
        public static final OpenLEhCacheHolder INSTANCE = new OpenLEhCacheHolder();
    }

    /**
     * Returns singleton OpenLEhCacheHolder
     * 
     * @return
     */
    public static OpenLEhCacheHolder getInstance() {
        return OpenLEhCacheHolderHolder.INSTANCE;
    }

    public Cache getModulesCache() {
        if (modulesCache == null) {
            synchronized (this) {
                if (modulesCache == null) {
                    try {
                        CacheManager cacheManager = getCacheManager();
                        modulesCache = cacheManager.getCache(CACHE_NAME);
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
            PathMatchingResourcePatternResolver prpr = new PathMatchingResourcePatternResolver();
            Resource[] resources = prpr.getResources(ResourceLoader.CLASSPATH_URL_PREFIX + OPENL_EHCACHE_FILE_NAME);
            if (resources == null || resources.length == 0) {
                resources = prpr
                    .getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + OPENL_EHCACHE_FILE_NAME);
            }
            if (resources == null || resources.length == 0) {
                throw new IllegalStateException(OPENL_EHCACHE_FILE_NAME + " hasn't been found!");
            } else if (resources.length > 1) {
                throw new IllegalStateException("Multiple " + OPENL_EHCACHE_FILE_NAME + " exist in classpath!");
            }

            cacheManager = new CacheManager(resources[0].getURL());
        }
        return cacheManager;
    }

}
