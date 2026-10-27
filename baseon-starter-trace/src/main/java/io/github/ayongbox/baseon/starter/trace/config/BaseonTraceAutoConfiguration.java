package io.github.ayongbox.baseon.starter.trace.config;

import io.github.ayongbox.baseon.starter.trace.async.MdcTaskDecorator;
import io.github.ayongbox.baseon.starter.trace.async.ScheduledTraceAspect;
import io.github.ayongbox.baseon.starter.trace.filter.TraceIdFilter;
import io.github.ayongbox.baseon.starter.trace.filter.TracerAwareTraceIdFilter;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 链路追踪自动装配。
 *
 * <p>TraceId 过滤器按 classpath 二选一：无 micrometer-tracing 时装配 {@link TraceIdFilter}
 * （自有 ID 生成/继承）；有 micrometer-tracing（典型为引入 baseon-starter-tracing-otel）时
 * 装配 {@link TracerAwareTraceIdFilter}（复用全链路 traceId）。拆成两个过滤器类是为了让
 * 无相关依赖的工程中本模块所有类都可加载，不在 TraceIdFilter 签名上引用 Tracer。
 *
 * <p>通过 {@link BeanPostProcessor} 给所有 {@link ThreadPoolTaskExecutor} 注入
 * {@link MdcTaskDecorator}，同时提供 {@link AsyncConfigurer} 让 @Async 也走装饰器。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(BaseonTraceProperties.class)
public class BaseonTraceAutoConfiguration {

    /**
     * 无 micrometer-tracing 时的过滤器装配（默认路径）。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("io.micrometer.tracing.Tracer")
    static class PlainTraceIdFilterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "baseon.trace", name = "enabled",
                havingValue = "true", matchIfMissing = true)
        public TraceIdFilter traceIdFilter(BaseonTraceProperties properties) {
            return new TraceIdFilter(properties);
        }
    }

    /**
     * 有 micrometer-tracing 时的过滤器装配。{@code management.tracing.enabled=false} 导致
     * 容器中无 Tracer Bean 时注入 {@link Tracer#NOOP}，由过滤器内部回退自有 ID 逻辑。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
    static class TracerAwareTraceIdFilterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "baseon.trace", name = "enabled",
                havingValue = "true", matchIfMissing = true)
        public TracerAwareTraceIdFilter tracerAwareTraceIdFilter(BaseonTraceProperties properties,
                                                                 ObjectProvider<Tracer> tracerProvider) {
            Tracer tracer = tracerProvider.getIfAvailable();
            return new TracerAwareTraceIdFilter(properties, tracer == null ? Tracer.NOOP : tracer);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.trace.async", name = "annotation-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsyncConfigurer mdcAsyncConfigurer() {
        return new AsyncConfigurer() {
            @Override
            public Executor getAsyncExecutor() {
                return null; // 使用默认 executor，由 BeanPostProcessor 装饰
            }

            @Override
            public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return null;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.trace.async", name = "executor-enabled",
            havingValue = "true", matchIfMissing = true)
    public static BeanPostProcessor threadPoolTaskExecutorMdcDecorator() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof ThreadPoolTaskExecutor executor) {
                    executor.setTaskDecorator(new MdcTaskDecorator());
                }
                return bean;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ScheduledTraceAspect.class)
    @ConditionalOnProperty(prefix = "baseon.trace.async", name = "scheduled-enabled",
            havingValue = "true", matchIfMissing = true)
    public ScheduledTraceAspect scheduledTraceAspect() {
        return new ScheduledTraceAspect();
    }
}
