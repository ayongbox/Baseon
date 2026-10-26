package io.github.ayongbox.baseon.starter.idempotent.config;

import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;
import io.github.ayongbox.baseon.starter.idempotent.aspect.IdempotentAspect;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 幂等控制装配。
 *
 * <p>占位走 starter-redis 的 {@link RedisUtil#setIfAbsent}（全局前缀 + key 校验在其内部完成）；
 * {@link CacheKeyPrefixProvider} 用 {@link ObjectProvider} 可选注入——引入 starter-tenant 时
 * 幂等 key 自动追加租户前缀，未引入则无前缀，两级能力解耦。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonIdempotentProperties.class)
public class BaseonIdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(RedisUtil redisUtil, BaseonIdempotentProperties properties,
                                             ObjectProvider<CacheKeyPrefixProvider> prefixProvider) {
        return new IdempotentAspect(redisUtil, properties, prefixProvider);
    }
}
