package com.cryptotrading.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {


    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .disableCachingNullValues();

        // Typed serializer for methods returning JsonNode
        RedisCacheConfiguration jsonNodeConfig = base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(JsonNode.class)));

        // Generic serializer for getCoinList (returns List)
        RedisCacheConfiguration listConfig = base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("coinList",      listConfig.entryTtl(Duration.ofSeconds(20)));
        configs.put("coinDetails",   jsonNodeConfig.entryTtl(Duration.ofSeconds(20)));
        configs.put("trendingCoins", jsonNodeConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("marketChart",   jsonNodeConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(listConfig)
                .withInitialCacheConfigurations(configs)
                .build();
    }
}
