package io.github.ayongbox.baseon.starter.cloud.nacos.config;

import com.alibaba.cloud.nacos.registry.NacosRegistration;
import io.github.ayongbox.baseon.starter.cloud.nacos.ConfigHolder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Nacos 增强能力自动装配。
 *
 * <p>注册发现与配置中心的本体装配由 SCA 完成（依赖引入即生效），
 * 这里只装配脚手架增强能力：三档注册策略（见
 * {@link RegisterStrategyEnvironmentPostProcessor}）与 {@link ConfigHolder}。
 */
@AutoConfiguration
@ConditionalOnClass(NacosRegistration.class)
@EnableConfigurationProperties(BaseonCloudNacosProperties.class)
public class BaseonCloudNacosAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConfigHolder configHolder(Environment environment) {
        return new ConfigHolder(environment);
    }
}
