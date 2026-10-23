package io.github.ayongbox.baseon.starter.cloud.nacos;

import org.springframework.core.env.Environment;

/**
 * 配置读取入口：每次调用直读 {@link Environment}，Nacos 配置刷新后
 * （Spring Cloud 的 Environment 会被刷新事件更新）无需重启即可读到最新值，
 * 天然规避了 {@code @Value} 注入不刷新、必须配合 {@code @RefreshScope} 的坑。
 */
public class ConfigHolder {

    private final Environment environment;

    public ConfigHolder(Environment environment) {
        this.environment = environment;
    }

    public String get(String key) {
        return environment.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    public <T> T get(String key, Class<T> targetType, T defaultValue) {
        return environment.getProperty(key, targetType, defaultValue);
    }
}
