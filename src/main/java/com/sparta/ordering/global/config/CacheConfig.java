package com.sparta.ordering.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USERS = "users";
    public static final String CATEGORIES = "categories";
    public static final String REGIONS = "regions";

    @Bean
    public CacheManager cacheManager() {
        Map<String, CaffeineCache> cacheMap = new HashMap<>();

        cacheMap.put(USERS, new CaffeineCache(
                USERS,
                Caffeine.newBuilder()
                        .expireAfterWrite(600, TimeUnit.SECONDS)
                        .maximumSize(1000)
                        .build()
        ));

        cacheMap.put(CATEGORIES, new CaffeineCache(
                CATEGORIES,
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(100)
                        .build()
        ));
        cacheMap.put(REGIONS, new CaffeineCache(
                REGIONS,
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(500)
                        .build()
        ));

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(new ArrayList<>(cacheMap.values()));
        return cacheManager;
    }
}
