package io.github.ayongbox.baseon.starter.idempotent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 幂等控制配置，前缀 {@code baseon.idempotent}。
 */
@ConfigurationProperties(prefix = "baseon.idempotent")
public class BaseonIdempotentProperties {

    /** 总开关：false 时幂等切面不装配，{@code @Idempotent} 注解不生效 */
    private boolean enabled = true;

    /**
     * 默认占位窗口（秒）：注解未显式指定 ttlSeconds 时使用。
     * 占位 key 到期自动过期即清理，无后台任务。
     */
    private long defaultTtlSeconds = 10;

    /** 注解未配置 key 表达式时，从该请求头读取幂等令牌 */
    private String tokenHeader = "X-Idempotent-Token";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public void setDefaultTtlSeconds(long defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public String getTokenHeader() {
        return tokenHeader;
    }

    public void setTokenHeader(String tokenHeader) {
        this.tokenHeader = tokenHeader;
    }
}
