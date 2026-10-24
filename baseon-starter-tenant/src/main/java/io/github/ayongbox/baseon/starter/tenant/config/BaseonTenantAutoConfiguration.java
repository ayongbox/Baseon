package io.github.ayongbox.baseon.starter.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;
import io.github.ayongbox.baseon.starter.tenant.aspect.TenantCacheIgnoreAspect;
import io.github.ayongbox.baseon.starter.tenant.aspect.TenantSqlIgnoreAspect;
import io.github.ayongbox.baseon.starter.tenant.support.BaseonTenantLineHandler;
import io.github.ayongbox.baseon.starter.tenant.support.TenantCacheKeyPrefixProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 租户装配。
 *
 * <p>{@link BaseonTenantLineHandler} 作为 {@link TenantLineHandler} 注册后，
 * starter-mybatis-plus 的 {@code MybatisPlusInterceptor} 会自动挂载租户插件；
 * 未引入本 starter 时租户插件不装配，两级能力解耦。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonTenantProperties.class)
public class BaseonTenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantLineHandler tenantLineHandler(BaseonTenantProperties properties) {
        return new BaseonTenantLineHandler(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheKeyPrefixProvider cacheKeyPrefixProvider() {
        return new TenantCacheKeyPrefixProvider();
    }

    @Bean
    public TenantSqlIgnoreAspect tenantSqlIgnoreAspect() {
        return new TenantSqlIgnoreAspect();
    }

    @Bean
    public TenantCacheIgnoreAspect tenantCacheIgnoreAspect() {
        return new TenantCacheIgnoreAspect();
    }
}
