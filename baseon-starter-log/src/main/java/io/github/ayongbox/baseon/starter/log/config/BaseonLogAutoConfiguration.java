package io.github.ayongbox.baseon.starter.log.config;

import io.github.ayongbox.baseon.starter.log.aspect.OperateLogAspect;
import io.github.ayongbox.baseon.starter.log.support.LoggingOperateLogHandler;
import io.github.ayongbox.baseon.starter.log.support.OperateLogHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 操作审计装配。
 *
 * <p>{@link OperateLogHandler} 以 {@link ConditionalOnMissingBean} 注册默认日志实现，
 * 业务声明自己的实现（写库 / MQ）即整体替换；切面 Bean 无条件注册，
 * 整个模块仅受总开关 {@code baseon.log.operate.enabled} 控制。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.log.operate", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonLogProperties.class)
public class BaseonLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OperateLogHandler operateLogHandler() {
        return new LoggingOperateLogHandler();
    }

    @Bean
    public OperateLogAspect operateLogAspect(OperateLogHandler operateLogHandler,
                                             BaseonLogProperties properties) {
        return new OperateLogAspect(operateLogHandler, properties);
    }
}
