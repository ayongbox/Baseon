package io.github.ayongbox.baseon.starter.sentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sentinel 增强能力配置，前缀 {@code baseon.sentinel}。
 *
 * <p>Sentinel 本体（transport 端口、dashboard、规则数据源等）仍走 SCA 原生
 * {@code spring.cloud.sentinel.*}，本类只承载脚手架增强能力：
 * 总开关与 BlockException 统一异常处理开关。
 */
@ConfigurationProperties(prefix = "baseon.sentinel")
public class BaseonSentinelProperties {

    /** 总开关：false 时本模块所有增强能力均不装配（Sentinel 本体不受影响） */
    private boolean enabled = true;

    /**
     * BlockException 统一异常处理开关：true 时装配 {@code SentinelBlockExceptionHandler}，
     * 把限流/熔断异常收敛为统一响应体（429 + 1005 / 503 + 9003）。
     */
    private boolean blockHandlerEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBlockHandlerEnabled() {
        return blockHandlerEnabled;
    }

    public void setBlockHandlerEnabled(boolean blockHandlerEnabled) {
        this.blockHandlerEnabled = blockHandlerEnabled;
    }
}
