package io.github.ayongbox.baseon.starter.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 操作审计配置，前缀 {@code baseon.log.operate}。
 */
@ConfigurationProperties(prefix = "baseon.log.operate")
public class BaseonLogProperties {

    /** 总开关：false 时审计切面不装配，{@code @OperateLog} 注解不生效 */
    private boolean enabled = true;

    /**
     * 入参摘要最大长度：{@code Arrays.deepToString} 序列化后超过该长度即截断，
     * 防止大报文撑爆审计存储与日志行。
     */
    private int argMaxLength = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getArgMaxLength() {
        return argMaxLength;
    }

    public void setArgMaxLength(int argMaxLength) {
        this.argMaxLength = argMaxLength;
    }
}
