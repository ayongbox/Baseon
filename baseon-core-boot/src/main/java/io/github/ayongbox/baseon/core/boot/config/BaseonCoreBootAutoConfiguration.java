package io.github.ayongbox.baseon.core.boot.config;

import io.github.ayongbox.baseon.core.boot.SpringContextHolder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * core.boot 自动装配：注册容器持有者，容器启动时由 Aware 回调回填静态引用。
 *
 * <p>{@code baseon.boot.enabled=false} 时不再注册，{@code TransactionUtil} 等依赖容器的工具将不可用。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.boot", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BaseonCoreBootAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }
}
