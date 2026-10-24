package io.github.ayongbox.baseon.starter.mybatisplus.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MyBatis-Plus 全局默认值翻译器：以最低优先级（addLast）注入 MP 原生配置默认值。
 *
 * <p>属性源追加在末尾，用户在 application.yml / Nacos 中显式声明的
 * {@code mybatis-plus.*} 配置始终优先于此处的默认值，默认值只在缺省时生效。
 *
 * <p>默认值语义与 {@code baseon-core} 的 {@code BaseEntity} 约定一一对应：
 * 逻辑删除字段 {@code deleted}（0 未删除 / 1 已删除）、下划线转驼峰。
 */
public class GlobalConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "baseonMybatisPlusGlobalConfig";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("mybatis-plus.global-config.db-config.logic-delete-field", "deleted");
        defaults.put("mybatis-plus.global-config.db-config.logic-delete-value", "1");
        defaults.put("mybatis-plus.global-config.db-config.logic-not-delete-value", "0");
        defaults.put("mybatis-plus.configuration.map-underscore-to-camel-case", "true");
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }
}
