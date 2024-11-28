package org.openl.rules.webstudio.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringCachingConfig {

    @Bean
    public CacheManager cacheManager() {
        return new JCacheCacheManager();
    }

}
