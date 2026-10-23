package io.github.ayongbox.baseon.starter.cloud.feign.config;

import feign.RequestInterceptor;
import feign.Retryer;
import io.github.ayongbox.baseon.starter.cloud.feign.interceptor.ContextPropagateRequestInterceptor;
import io.github.ayongbox.baseon.starter.trace.config.BaseonTraceProperties;
import io.github.ayongbox.baseon.starter.web.config.BaseonWebProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Feign 调用治理自动装配。
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@EnableConfigurationProperties(BaseonCloudFeignProperties.class)
public class BaseonCloudFeignAutoConfiguration {

    /**
     * 上下文透传拦截器。不使用 {@code @ConditionalOnMissingBean(RequestInterceptor.class)}：
     * Feign 会收集容器中所有拦截器，业务可以再加自己的；按具体类型去重以便业务替换本实现。
     */
    @Bean
    @ConditionalOnMissingBean(ContextPropagateRequestInterceptor.class)
    @ConditionalOnProperty(prefix = "baseon.cloud.feign", name = "propagate-enabled",
            havingValue = "true", matchIfMissing = true)
    public ContextPropagateRequestInterceptor contextPropagateRequestInterceptor(
            BaseonWebProperties webProperties, BaseonTraceProperties traceProperties) {
        return new ContextPropagateRequestInterceptor(webProperties, traceProperties);
    }

    /**
     * 全局默认重试器：默认不重试（NEVER_RETRY），开启后按指数退避。
     * 业务声明任意 {@link Retryer} Bean 即整体替换。
     */
    @Bean
    @ConditionalOnMissingBean
    public Retryer feignRetryer(BaseonCloudFeignProperties properties) {
        BaseonCloudFeignProperties.Retry retry = properties.getRetry();
        if (!retry.isEnabled()) {
            return Retryer.NEVER_RETRY;
        }
        return new Retryer.Default(retry.getPeriod(), retry.getMaxPeriod(), retry.getMaxAttempts());
    }
}
