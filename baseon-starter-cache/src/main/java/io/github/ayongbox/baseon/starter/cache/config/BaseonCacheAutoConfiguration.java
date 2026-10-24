package io.github.ayongbox.baseon.starter.cache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;
import io.github.ayongbox.baseon.starter.cache.support.CacheHelper;
import io.github.ayongbox.baseon.starter.redis.support.RedisLock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 缓存装配：L1 Caffeine + CacheHelper。
 *
 * <p>{@link CacheKeyPrefixProvider} 为可选 SPI：未引入 starter-tenant 时无前缀，
 * 引入后自动获得租户隔离前缀。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonCacheProperties.class)
public class BaseonCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "baseonL1Cache")
    public Cache<String, Object> baseonL1Cache(BaseonCacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getL1MaxSize())
                .expireAfterWrite(properties.getL1TtlSeconds(), TimeUnit.SECONDS)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheHelper cacheHelper(StringRedisTemplate stringRedisTemplate,
                                   ObjectMapper objectMapper,
                                   Cache<String, Object> baseonL1Cache,
                                   RedisLock redisLock,
                                   BaseonCacheProperties properties,
                                   ObjectProvider<CacheKeyPrefixProvider> prefixProvider) {
        return new CacheHelper(stringRedisTemplate, objectMapper, baseonL1Cache, redisLock,
                properties, prefixProvider.getIfUnique());
    }
}
