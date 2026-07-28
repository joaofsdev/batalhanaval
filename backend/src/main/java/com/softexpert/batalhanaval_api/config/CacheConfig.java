package com.softexpert.batalhanaval_api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String RANKING_CACHE = "ranking";
    public static final String PROFILE_CACHE = "profile";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new CaffeineCache(RANKING_CACHE,
                        Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .maximumSize(50)
                                .build()),
                new CaffeineCache(PROFILE_CACHE,
                        Caffeine.newBuilder()
                                .expireAfterWrite(2, TimeUnit.MINUTES)
                                .maximumSize(200)
                                .build())
        ));
        return cacheManager;
    }
}
