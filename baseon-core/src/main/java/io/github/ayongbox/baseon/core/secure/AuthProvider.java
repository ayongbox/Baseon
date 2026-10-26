package io.github.ayongbox.baseon.core.secure;

import io.github.ayongbox.baseon.core.model.LoginUser;

/**
 * 认证提供者契约：签发、校验、刷新、注销访问凭证。
 *
 * <p>内核只定义契约，默认实现由 starter-auth 提供（JWT 双 Token + Redis 黑名单）。
 * 业务接入企业统一认证（OAuth2 / CAS / 内部 SSO）时实现本接口即可替换，
 * 上层 {@code @RequiresPermission} 与登录上下文写入逻辑不变。
 */
public interface AuthProvider {

    /**
     * 为指定登录用户签发访问凭证（默认实现返回 access token，刷新凭证由 {@link #refresh} 链路管理）。
     *
     * @param user 已验证身份的登录用户（含权限集合）
     * @return 访问凭证字符串
     */
    String issueToken(LoginUser user);

    /**
     * 校验凭证并还原登录用户。凭证非法、过期或已被吊销时抛出业务异常（未认证）。
     *
     * @param token 访问凭证
     * @return 凭证携带的登录用户（含权限集合）
     */
    LoginUser verify(String token);

    /**
     * 用刷新凭证换取新的访问凭证。刷新凭证非法或已吊销时抛出业务异常（未认证）。
     *
     * @param refreshToken 刷新凭证
     * @return 新的访问凭证
     */
    String refresh(String refreshToken);

    /**
     * 注销指定凭证（默认实现把 access token 加入黑名单直至其自然过期）。
     *
     * @param token 访问凭证
     */
    void invalidate(String token);
}
