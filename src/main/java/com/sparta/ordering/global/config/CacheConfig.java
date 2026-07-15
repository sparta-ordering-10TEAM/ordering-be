package com.sparta.ordering.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        Map<String, CaffeineCache> cacheMap = new HashMap<>();

        // users cache
        cacheMap.put("users", new CaffeineCache(
                "users",
                Caffeine.newBuilder()
                        .expireAfterWrite(600, TimeUnit.SECONDS)
                        .maximumSize(1000)
                        .build()
        ));

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(new ArrayList<>(cacheMap.values()));

        return cacheManager;
    }
}
