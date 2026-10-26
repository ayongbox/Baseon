package io.github.ayongbox.baseon.starter.sentinel.config;

import com.alibaba.csp.sentinel.SphU;
import io.github.ayongbox.baseon.starter.sentinel.handler.SentinelBlockExceptionHandler;
import io.github.ayongbox.baseon.starter.sentinel.support.TenantHotParamGuard;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Sentinel 增强能力自动装配入口。
 *
 * <p>Sentinel 本体（核心拦截、dashboard 通信、规则数据源）由 SCA 的
 * {@code SentinelAutoConfiguration} 装配，规则 Nacos 持久化走 SCA 原生
 * {@code spring.cloud.sentinel.datasource.*} 配置，本模块不重复造轮子，
 * 只装配脚手架增强能力：BlockException 统一异常处理与租户维度热点限流门面。
 *
 * <p>所有 Bean 都带 {@link ConditionalOnMissingBean}，业务声明同类型 Bean 即覆盖默认实现。
 */
@AutoConfiguration
@ConditionalOnClass(SphU.class)
@ConditionalOnProperty(prefix = "baseon.sentinel", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonSentinelProperties.class)
public class BaseonSentinelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.sentinel", name = "block-handler-enabled",
            havingValue = "true", matchIfMissing = true)
    public SentinelBlockExceptionHandler sentinelBlockExceptionHandler() {
        return new SentinelBlockExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantHotParamGuard tenantHotParamGuard() {
        return new TenantHotParamGuard();
    }
}
