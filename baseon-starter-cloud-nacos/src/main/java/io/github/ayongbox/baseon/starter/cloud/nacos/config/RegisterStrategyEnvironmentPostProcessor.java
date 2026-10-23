package io.github.ayongbox.baseon.starter.cloud.nacos.config;

import io.github.ayongbox.baseon.core.cloud.CloudHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 三档注册策略翻译器：把 {@code baseon.cloud.nacos.register-strategy}
 * 翻译成 SCA 原生 {@code spring.cloud.nacos.discovery.*} 配置。
 *
 * <p>以最低优先级（addLast）注入属性源：用户在 application.yml 中显式声明的
 * {@code spring.cloud.nacos.discovery.*} 始终优先于策略推导值，策略只是默认值。
 */
public class RegisterStrategyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(RegisterStrategyEnvironmentPostProcessor.class);

    static final String PROPERTY_SOURCE_NAME = "baseonCloudNacosRegisterStrategy";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        BaseonCloudNacosProperties properties = Binder.get(environment)
                .bind("baseon.cloud.nacos", BaseonCloudNacosProperties.class)
                .orElse(new BaseonCloudNacosProperties());

        Map<String, Object> derived = new HashMap<>();
        switch (properties.getRegisterStrategy()) {
            case LOCAL_NO_REGISTER -> {
                derived.put("spring.cloud.nacos.discovery.register-enabled", "false");
                log.info("[baseon] Nacos 注册策略=LOCAL_NO_REGISTER：本实例不会注册到注册中心");
            }
            case LOCAL_REGISTER_PRIVATE -> {
                derived.put("spring.cloud.nacos.discovery.register-enabled", "true");
                derived.put("spring.cloud.nacos.discovery.metadata[" + CloudHeaders.LOCAL_METADATA + "]", "true");
                log.info("[baseon] Nacos 注册策略=LOCAL_REGISTER_PRIVATE：注册但标记 {}=true，"
                        + "其他服务默认不会路由到本实例", CloudHeaders.LOCAL_METADATA);
            }
            case FULL_REGISTER -> {
                // 生产档位：SCA 默认行为，不注入任何派生配置
            }
        }

        if (!derived.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, derived));
        }
    }
}
