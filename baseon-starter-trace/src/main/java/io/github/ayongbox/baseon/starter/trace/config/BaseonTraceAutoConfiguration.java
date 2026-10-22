package io.github.ayongbox.baseon.starter.trace.config;

import io.github.ayongbox.baseon.starter.trace.async.MdcTaskDecorator;
import io.github.ayongbox.baseon.starter.trace.async.ScheduledTraceAspect;
import io.github.ayongbox.baseon.starter.trace.filter.TraceIdFilter;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 链路追踪自动装配。
 *
 * <p>通过 {@link BeanPostProcessor} 给所有 {@link ThreadPoolTaskExecutor} 注入
 * {@link MdcTaskDecorator}，同时提供 {@link AsyncConfigurer} 让 @Async 也走装饰器。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(BaseonTraceProperties.class)
public class BaseonTraceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.trace", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public TraceIdFilter traceIdFilter(BaseonTraceProperties properties) {
        return new TraceIdFilter(properties);
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
