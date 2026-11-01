package io.github.ayongbox.baseon.core.launch;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动扩展配置：横幅开关与启动前置校验清单。
 *
 * <p>前置校验通过 SPI 扩展（业务注册 {@link LaunchCheck} Bean），
 * 配置项只控制框架内置行为，不内置业务规则（机制/策略分层）。
 */
@ConfigurationProperties(prefix = "baseon.launch")
public class LaunchProperties {

    /**
     * 是否打印启动横幅，默认 true；prod 环境建议保持开启，成本可忽略。
     */
    private boolean bannerEnabled = true;

    /**
     * 启动前置校验：任一检查失败是否终止启动。默认 true（快速失败）；
     * 置 false 时降级为 WARN 日志，仅用于本地联调，生产禁止关闭。
     */
    private boolean failFast = true;

    /**
     * 必选环境变量清单：任一缺失即视为校验失败。
     * 用于强制声明「本服务依赖的外部配置」，避免启动后才暴露。
     */
    private List<String> requiredEnvironmentVariables = new ArrayList<>();

    /**
     * 必选配置项清单：任一缺失（无对应属性）即视为校验失败。
     * 用点号路径，例如 {@code spring.datasource.url}。
     */
    private List<String> requiredProperties = new ArrayList<>();

    public boolean isBannerEnabled() {
        return bannerEnabled;
    }

    public void setBannerEnabled(boolean bannerEnabled) {
        this.bannerEnabled = bannerEnabled;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public List<String> getRequiredEnvironmentVariables() {
        return requiredEnvironmentVariables;
    }

    public void setRequiredEnvironmentVariables(List<String> requiredEnvironmentVariables) {
        this.requiredEnvironmentVariables = requiredEnvironmentVariables;
    }

    public List<String> getRequiredProperties() {
        return requiredProperties;
    }

    public void setRequiredProperties(List<String> requiredProperties) {
        this.requiredProperties = requiredProperties;
    }
}
