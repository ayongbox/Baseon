package io.github.ayongbox.baseon.starter.auth.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.secure.AuthProvider;
import io.github.ayongbox.baseon.starter.auth.config.BaseonAuthProperties;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import io.jsonwebtoken.Claims;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * {@link AuthProvider} 的默认实现：JWT 双 Token + Redis 吊销/黑名单。
 *
 * <p>会话模型：一次登录 = 一个会话（sessionId），refresh token 的 jti 即 sessionId，
 * 签发时写入 {@code auth:refresh:{jti}}（TTL=refresh 有效期）；access token 的
 * {@code sid} claim 指向所属会话。登出 = access jti 入黑名单（TTL=剩余有效期）
 * + 删除会话对应的 refresh key，整条登录链路一次性失效。
 */
public class TokenService implements AuthProvider {

    private static final String PRESENT = "1";

    private final JwtTokenProvider tokenProvider;
    private final RedisUtil redisUtil;
    private final BaseonAuthProperties properties;

    public TokenService(JwtTokenProvider tokenProvider, RedisUtil redisUtil,
                        BaseonAuthProperties properties) {
        this.tokenProvider = tokenProvider;
        this.redisUtil = redisUtil;
        this.properties = properties;
    }

    /**
     * 登录链路入口：签发双 Token，并把 refresh jti 登记到 Redis（可被登出吊销）。
     */
    public TokenPair issueTokenPair(LoginUser user) {
        String sessionId = UUID.randomUUID().toString();
        String accessToken = tokenProvider.createAccessToken(user, sessionId);
        String refreshToken = tokenProvider.createRefreshToken(user, sessionId);
        redisUtil.set(refreshKey(sessionId), PRESENT, properties.getRefreshExpireSeconds());
        return new TokenPair(accessToken, refreshToken, properties.getAccessExpireSeconds());
    }

    @Override
    public String issueToken(LoginUser user) {
        return issueTokenPair(user).getAccessToken();
    }

    @Override
    public LoginUser verify(String token) {
        Claims claims = tokenProvider.parse(token);
        requireType(claims, JwtTokenProvider.TYPE_ACCESS);
        if (Boolean.TRUE.equals(redisUtil.hasKey(blacklistKey(claims.getId())))) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "凭证已吊销，请重新登录");
        }
        return toLoginUser(claims);
    }

    @Override
    public String refresh(String refreshToken) {
        Claims claims = tokenProvider.parse(refreshToken);
        requireType(claims, JwtTokenProvider.TYPE_REFRESH);
        String sessionId = claims.getId();
        if (!Boolean.TRUE.equals(redisUtil.hasKey(refreshKey(sessionId)))) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "刷新凭证已失效，请重新登录");
        }
        // 会话仍然有效，签发绑定同一会话的新 access token（新 jti，不受旧黑名单影响）
        return tokenProvider.createAccessToken(toLoginUser(claims), sessionId);
    }

    @Override
    public void invalidate(String token) {
        Claims claims;
        try {
            claims = tokenProvider.parse(token);
        } catch (BizException e) {
            // 已过期/非法的凭证无法解析会话信息，也不需再拉黑——登出保持幂等
            return;
        }
        if (!JwtTokenProvider.TYPE_ACCESS.equals(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
            return;
        }
        // 黑名单只覆盖 token 剩余有效期，到期自动清出，避免 Redis 无限膨胀
        long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainingMillis > 0) {
            long ttlSeconds = (remainingMillis + 999) / 1000;
            redisUtil.set(blacklistKey(claims.getId()), PRESENT, ttlSeconds);
        }
        String sessionId = claims.get(JwtTokenProvider.CLAIM_SID, String.class);
        if (sessionId != null) {
            redisUtil.delete(refreshKey(sessionId));
        }
    }

    private void requireType(Claims claims, String expectedType) {
        if (!expectedType.equals(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "凭证类型不匹配");
        }
    }

    private LoginUser toLoginUser(Claims claims) {
        LoginUser user = new LoginUser();
        user.setUserId(Long.valueOf(claims.getSubject()));
        user.setUsername(claims.get(JwtTokenProvider.CLAIM_USERNAME, String.class));
        Object tenantId = claims.get(JwtTokenProvider.CLAIM_TENANT_ID);
        if (tenantId instanceof Number number) {
            // JSON 数值反序列化可能落到 Integer/Long，统一按 long 还原
            user.setTenantId(number.longValue());
        }
        Object perms = claims.get(JwtTokenProvider.CLAIM_PERMS);
        if (perms instanceof List<?> list) {
            user.setPermissions(new LinkedHashSet<>(list.stream().map(String::valueOf).toList()));
        }
        return user;
    }

    private String blacklistKey(String jti) {
        return properties.getBlacklistKeyPrefix() + jti;
    }

    private String refreshKey(String sessionId) {
        return properties.getRefreshKeyPrefix() + sessionId;
    }
}
