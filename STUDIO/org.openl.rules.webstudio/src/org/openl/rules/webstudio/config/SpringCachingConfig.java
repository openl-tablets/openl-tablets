package org.openl.rules.webstudio.config;

import java.util.Objects;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

@Configuration
public class SpringCachingConfig {

    @Bean
    public CacheManager cacheManager() {
        return new JCacheCacheManager();
    }

    @Bean
    public ApplicationListener<ContextRefreshedEvent> contextRefreshedEventListener(CacheManager cacheManager) {
        return event -> {
            // Invalidate all caches when Spring context is refreshed
            cacheManager.getCacheNames().stream()
                    .map(cacheManager::getCache)
                    .filter(Objects::nonNull)
                    .forEach(Cache::invalidate);
        };
    }

}
