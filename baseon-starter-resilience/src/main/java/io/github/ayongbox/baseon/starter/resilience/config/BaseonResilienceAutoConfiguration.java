package io.github.ayongbox.baseon.starter.resilience.config;

import io.github.ayongbox.baseon.starter.resilience.alarm.AlarmHook;
import io.github.ayongbox.baseon.starter.resilience.alarm.LoggingAlarmHook;
import io.github.ayongbox.baseon.starter.resilience.aspect.DegradeAspect;
import io.github.ayongbox.baseon.starter.resilience.support.DegradeResultCache;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 调用韧性装配。
 *
 * <p>熔断/舱壁注解能力（{@code @CircuitBreaker} / {@code @Bulkhead}）由
 * resilience4j-spring-boot3 自身自动配置提供，本 starter 不重封装；
 * 这里只装配自研的降级兜底（{@link DegradeAspect} + {@link DegradeResultCache}）
 * 与熔断告警（{@link AlarmHook} + {@link CircuitBreakerAlarmRegistrar}）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonResilienceProperties.class)
public class BaseonResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DegradeResultCache degradeResultCache(BaseonResilienceProperties properties) {
        return new DegradeResultCache(properties.getDegradeCacheMaxSize(), properties.getDegradeCacheTtlSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    public DegradeAspect degradeAspect(DegradeResultCache degradeResultCache) {
        return new DegradeAspect(degradeResultCache);
    }

    /** 默认日志告警，业务提供同类型 Bean 即可覆盖接入告警渠道 */
    @Bean
    @ConditionalOnMissingBean
    public AlarmHook alarmHook() {
        return new LoggingAlarmHook();
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerAlarmRegistrar circuitBreakerAlarmRegistrar(
            ObjectProvider<CircuitBreakerRegistry> registryProvider, AlarmHook alarmHook) {
        return new CircuitBreakerAlarmRegistrar(registryProvider, alarmHook);
    }
}
