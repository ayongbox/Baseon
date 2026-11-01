package io.github.ayongbox.baseon.starter.seata.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 把 {@code baseon.seata.*} 映射为 Seata 原生 {@code seata.*} 属性。
 *
 * <p>只映射显式配置的 baseon 键，未配置的 {@code seata.*} 保持业务自身的取值；
 * 映射结果以最高优先级属性源注入，因此同名的 {@code seata.*} 配置会被 baseon 键覆盖。
 *
 * <p>顺序取 {@link Ordered#LOWEST_PRECEDENCE}，保证在配置文件加载（ConfigData）之后执行。
 */
public class BaseonSeataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * 映射属性源名，用于幂等判断。
     */
    static final String PROPERTY_SOURCE_NAME = "baseonSeataProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        Map<String, Object> mapped = new HashMap<>();
        mapEnabled(environment, mapped);
        mapMode(environment, mapped);
        mapIfPresent(environment, mapped, "baseon.seata.tx-service-group", "seata.tx-service-group");
        mapIfPresent(environment, mapped, "baseon.seata.application-id", "seata.application-id");
        if (!mapped.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, mapped));
        }
    }

    private void mapEnabled(ConfigurableEnvironment environment, Map<String, Object> mapped) {
        String value = environment.getProperty("baseon.seata.enabled");
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!"true".equals(normalized) && !"false".equals(normalized)) {
            throw new IllegalStateException("baseon.seata.enabled 只接受 true/false，当前值：" + value);
        }
        mapped.put("seata.enabled", Boolean.parseBoolean(normalized));
    }

    /**
     * AT 模式必须打开数据源自动代理，TCC 模式必须关闭——两种模式共用一套代理开关时
     * 会出现「TCC 也被代理了数据源」的隐性错配，这里由模式统一推导。
     */
    private void mapMode(ConfigurableEnvironment environment, Map<String, Object> mapped) {
        String value = environment.getProperty("baseon.seata.mode");
        if (value == null || value.isBlank()) {
            return;
        }
        BaseonSeataProperties.Mode mode;
        try {
            mode = BaseonSeataProperties.Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("baseon.seata.mode 只接受 at/tcc，当前值：" + value, e);
        }
        mapped.put("seata.enable-auto-data-source-proxy", mode == BaseonSeataProperties.Mode.AT);
        if (mode == BaseonSeataProperties.Mode.AT) {
            mapped.put("seata.data-source-proxy-mode", "AT");
        }
    }

    private void mapIfPresent(ConfigurableEnvironment environment, Map<String, Object> mapped,
                              String sourceKey, String targetKey) {
        String value = environment.getProperty(sourceKey);
        if (value != null && !value.isBlank()) {
            mapped.put(targetKey, value.trim());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
