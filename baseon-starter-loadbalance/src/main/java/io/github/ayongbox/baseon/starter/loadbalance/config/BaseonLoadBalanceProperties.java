package io.github.ayongbox.baseon.starter.loadbalance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 负载均衡治理配置，前缀 {@code baseon.cloud.loadbalance}。
 */
@ConfigurationProperties(prefix = "baseon.cloud.loadbalance")
public class BaseonLoadBalanceProperties {

    /**
     * 灰度路由开关。关闭后负载均衡退化为普通轮询（不解析标签、不过滤实例），
     * 但 GrayContext 仍会被入口过滤器捕获并透传给下游，便于日志排查。
     */
    private boolean enabled = true;

    /**
     * 是否允许把请求路由到 {@code baseon.local=true} 标记的本地调试实例。
     * 默认不允许：本地实例只服务开发者自己，不承接集群流量。
     */
    private boolean allowLocal = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowLocal() {
        return allowLocal;
    }

    public void setAllowLocal(boolean allowLocal) {
        this.allowLocal = allowLocal;
    }
}
