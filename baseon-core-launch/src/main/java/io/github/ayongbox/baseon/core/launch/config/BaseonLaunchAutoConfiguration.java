package io.github.ayongbox.baseon.core.launch.config;

import io.github.ayongbox.baseon.core.launch.BaseonBannerPrinter;
import io.github.ayongbox.baseon.core.launch.GracefulShutdownListener;
import io.github.ayongbox.baseon.core.launch.LaunchCheck;
import io.github.ayongbox.baseon.core.launch.LaunchCheckExecutor;
import io.github.ayongbox.baseon.core.launch.LaunchProperties;
import io.github.ayongbox.baseon.core.launch.RequiredConfigurationCheck;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * core.launch 自动装配：启动横幅、前置校验执行器、优雅停机监听器。
 *
 * <p>{@code baseon.launch.enabled=false} 时整体不再注册；
 * 每个默认 Bean 均可被业务同名/同类型 Bean 覆盖（{@link ConditionalOnMissingBean}）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.launch", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LaunchProperties.class)
public class BaseonLaunchAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "baseon.launch", name = "banner-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public BaseonBannerPrinter baseonBannerPrinter(Environment environment) {
        return new BaseonBannerPrinter(environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public GracefulShutdownListener gracefulShutdownListener() {
        return new GracefulShutdownListener();
    }

    /**
     * 内置必选配置检查：清单为空时检查直接通过，保持编排简单。
     */
    @Bean
    @ConditionalOnMissingBean(name = "requiredConfigurationCheck")
    public LaunchCheck requiredConfigurationCheck(ConfigurableEnvironment environment, LaunchProperties properties) {
        return RequiredConfigurationCheck.of(environment, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LaunchCheckExecutor launchCheckExecutor(List<LaunchCheck> checks,
                                                   LaunchProperties properties,
                                                   Environment environment) {
        return new LaunchCheckExecutor(checks, properties.isFailFast(), environment);
    }
}
