package io.github.ayongbox.baseon.starter.cloud.feign.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Feign 默认值翻译器：把 {@code baseon.cloud.feign} 的超时配置推导为
 * Feign 原生 {@code feign.client.config.default.*} 默认值。
 *
 * <p>最低优先级（addLast）注入：业务在 application.yml 中显式声明的
 * {@code feign.client.config.*} 或对某个 client 的单独配置始终优先。
 */
public class FeignDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(FeignDefaultsEnvironmentPostProcessor.class);

    static final String PROPERTY_SOURCE_NAME = "baseonCloudFeignDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        BaseonCloudFeignProperties properties = Binder.get(environment)
                .bind("baseon.cloud.feign", BaseonCloudFeignProperties.class)
                .orElse(new BaseonCloudFeignProperties());

        Map<String, Object> derived = new HashMap<>();
        derived.put("feign.client.config.default.connect-timeout", String.valueOf(properties.getConnectTimeout()));
        derived.put("feign.client.config.default.read-timeout", String.valueOf(properties.getReadTimeout()));

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, derived));
        log.debug("[baseon] Feign 默认超时：connect={}ms, read={}ms（可被 feign.client.config.* 覆盖）",
                properties.getConnectTimeout(), properties.getReadTimeout());
    }
}
