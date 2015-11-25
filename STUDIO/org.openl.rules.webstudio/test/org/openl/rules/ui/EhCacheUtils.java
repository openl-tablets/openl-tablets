package org.openl.rules.ui;

import java.io.UnsupportedEncodingException;

import org.openl.util.IOUtils;

import net.sf.ehcache.CacheManager;

public final class EhCacheUtils {
    
    public static final String STUDIO_CACHE_MANAGER = "studioCacheManager";
    
    private EhCacheUtils() {
    }

    public static void createCache() throws UnsupportedEncodingException {
        String config = "<ehcache xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:noNamespaceSchemaLocation=\"http://ehcache.org/ehcache.xsd\" name=\"studioCacheManager\">" +
                "</ehcache>";
        CacheManager.create(IOUtils.toInputStream(config));
    }

    public static void shutdownCache() {
        CacheManager.getCacheManager(STUDIO_CACHE_MANAGER).shutdown();
    }
}
