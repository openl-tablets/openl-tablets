package org.openl.rules.ui;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.openl.rules.webstudio.dependencies.InstantiationStrategyFactory;
import org.openl.util.IOUtils;

import java.io.UnsupportedEncodingException;

public final class EhCacheUtils {
    private EhCacheUtils() {
    }

    public static void createCache() throws UnsupportedEncodingException {
        String config = "<ehcache xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:noNamespaceSchemaLocation=\"http://ehcache.org/ehcache.xsd\" name=\"studioCacheManager\">" +
                "</ehcache>";
        CacheManager cacheManager = CacheManager.create(IOUtils.toInputStream(config));
        cacheManager.addCache(new Cache(InstantiationStrategyFactory.INSTANTIATION_STRATEGIES_CACHE,
                5, MemoryStoreEvictionPolicy.LRU,
                false, null, false, 0, 300,
                false, 120, null,
                null, 10000000, 0));
    }

    public static void shutdownCache() {
        CacheManager.getCacheManager(InstantiationStrategyFactory.STUDIO_CACHE_MANAGER).shutdown();
    }
}
