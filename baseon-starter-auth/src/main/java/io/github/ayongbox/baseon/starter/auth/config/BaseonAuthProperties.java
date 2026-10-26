package io.github.ayongbox.baseon.starter.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT 鉴权配置，前缀 {@code baseon.auth}。
 */
@ConfigurationProperties(prefix = "baseon.auth")
public class BaseonAuthProperties {

    /** 总开关：false 时 AuthTokenFilter 直接放行，不做任何 Token 校验 */
    private boolean enabled = true;

    /**
     * HS256 签名密钥，必填且至少 32 字节（UTF-8 编码后）。
     * 缺省或过短时启动快速失败——弱密钥签发出来的 Token 等于没有鉴权。
     */
    private String secret = "";

    /** access token 有效期（秒），默认 2 小时 */
    private long accessExpireSeconds = 7200;

    /** refresh token 有效期（秒），默认 7 天 */
    private long refreshExpireSeconds = 604800;

    /** 不校验 Token 的放行路径（Ant 风格），登录与刷新接口必须在内 */
    private List<String> ignorePaths = new ArrayList<>(List.of("/auth/login", "/auth/refresh"));

    /** access token 黑名单 Redis key 前缀，登出后 jti 写入该前缀下直至自然过期 */
    private String blacklistKeyPrefix = "auth:blacklist:";

    /** refresh token 会话 Redis key 前缀，签发时写入、吊销时删除 */
    private String refreshKeyPrefix = "auth:refresh:";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public void setAccessExpireSeconds(long accessExpireSeconds) {
        this.accessExpireSeconds = accessExpireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }

    public void setRefreshExpireSeconds(long refreshExpireSeconds) {
        this.refreshExpireSeconds = refreshExpireSeconds;
    }

    public List<String> getIgnorePaths() {
        return ignorePaths;
    }

    public void setIgnorePaths(List<String> ignorePaths) {
        this.ignorePaths = ignorePaths;
    }

    public String getBlacklistKeyPrefix() {
        return blacklistKeyPrefix;
    }

    public void setBlacklistKeyPrefix(String blacklistKeyPrefix) {
        this.blacklistKeyPrefix = blacklistKeyPrefix;
    }

    public String getRefreshKeyPrefix() {
        return refreshKeyPrefix;
    }

    public void setRefreshKeyPrefix(String refreshKeyPrefix) {
        this.refreshKeyPrefix = refreshKeyPrefix;
    }
}
