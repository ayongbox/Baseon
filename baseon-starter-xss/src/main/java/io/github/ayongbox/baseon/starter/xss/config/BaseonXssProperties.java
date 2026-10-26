package io.github.ayongbox.baseon.starter.xss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS 防护配置，前缀 {@code baseon.xss}。
 */
@ConfigurationProperties(prefix = "baseon.xss")
public class BaseonXssProperties {

    /**
     * 总开关：false 时过滤器原样放行所有请求。默认 true。
     */
    private boolean enabled = true;

    /**
     * 排除路径（Ant 风格）：命中的请求不做转义，
     * 用于富文本提交等由业务自行处理 XSS 的接口。默认空。
     */
    private List<String> excludePaths = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}
