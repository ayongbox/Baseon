package io.github.ayongbox.baseon.starter.debug.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 调试端点配置，前缀 {@code baseon.debug}。
 *
 * <p>设计意图：debug 端点暴露登录上下文与运行配置，在生产环境属于信息泄露面，
 * 因此生产失效必须「硬」——由 profile 交集判定强制不装配，而不是仅靠
 * {@code enabled} 这个可被误开启的配置开关。
 */
@ConfigurationProperties(prefix = "baseon.debug")
public class BaseonDebugProperties {

    /**
     * 总开关：false 时端点 Bean 不注册。默认 true。
     * 注意：即使为 true，命中 {@link #prodProfiles} 时端点仍被强制移除。
     */
    private boolean enabled = true;

    /**
     * 生产环境 profile 名单：activeProfiles 与之存在交集即判定为生产环境，
     * 端点硬失效（不装配 + 运行期二次拦截）。默认 {@code ["prod"]}。
     */
    private List<String> prodProfiles = new ArrayList<>(List.of("prod"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getProdProfiles() {
        return prodProfiles;
    }

    public void setProdProfiles(List<String> prodProfiles) {
        this.prodProfiles = prodProfiles;
    }
}
