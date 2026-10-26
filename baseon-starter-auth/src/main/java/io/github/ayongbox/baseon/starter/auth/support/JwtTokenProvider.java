package io.github.ayongbox.baseon.starter.auth.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.starter.auth.config.BaseonAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析，HS256 对称签名。
 *
 * <p>claims 约定：{@code sub}=userId、{@code jti}=凭证唯一标识、{@code username}、
 * {@code tenantId}、{@code perms}=权限集合、{@code type}=access|refresh、
 * {@code sid}=会话标识（refresh token 的 jti，access token 携带它以便登出时吊销刷新凭证）。
 *
 * <p>密钥在构造时校验：为空或不足 32 字节直接快速失败，避免弱密钥上线。
 */
public class JwtTokenProvider {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_PERMS = "perms";
    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_SID = "sid";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    /** HS256 要求密钥至少 256 bit，即 32 字节 */
    private static final int MIN_SECRET_BYTES = 32;

    private final BaseonAuthProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(BaseonAuthProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "baseon.auth.secret 未配置或长度不足 " + MIN_SECRET_BYTES + " 字节，"
                            + "HS256 弱密钥等于没有鉴权，请配置足够长的随机密钥后重启");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 access token。jti 随机生成，{@code sid} 指向所属会话（refresh token 的 jti）。
     */
    public String createAccessToken(LoginUser user, String sessionId) {
        return createToken(user, UUID.randomUUID().toString(), sessionId,
                TYPE_ACCESS, properties.getAccessExpireSeconds());
    }

    /**
     * 签发 refresh token。jti 即会话标识，Redis 中按 {@code auth:refresh:{jti}} 登记可吊销。
     */
    public String createRefreshToken(LoginUser user, String sessionId) {
        return createToken(user, sessionId, null, TYPE_REFRESH, properties.getRefreshExpireSeconds());
    }

    private String createToken(LoginUser user, String jti, String sessionId, String type, long expireSeconds) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .id(jti)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key);
        if (sessionId != null) {
            builder.claim(CLAIM_SID, sessionId);
        }
        if (user.getUsername() != null) {
            builder.claim(CLAIM_USERNAME, user.getUsername());
        }
        if (user.getTenantId() != null) {
            builder.claim(CLAIM_TENANT_ID, user.getTenantId());
        }
        if (user.getPermissions() != null) {
            builder.claim(CLAIM_PERMS, new ArrayList<>(user.getPermissions()));
        }
        return builder.compact();
    }

    /**
     * 解析并校验签名与有效期。任何非法/过期凭证统一转为「未认证」业务异常，
     * 由调用方（Filter 或切面）决定如何呈现，本类不感知 HTTP。
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "凭证无效");
        }
    }
}
