package io.github.ayongbox.baseon.starter.auth.support;

/**
 * 登录签发的双 Token 结果。
 */
public class TokenPair {

    /** 访问凭证，随请求头 {@code Authorization: Bearer} 携带 */
    private String accessToken;

    /** 刷新凭证，仅在换取新 access token 时使用 */
    private String refreshToken;

    /** access token 有效期（秒），供前端安排静默刷新节奏 */
    private long expiresIn;

    public TokenPair() {
    }

    public TokenPair(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
