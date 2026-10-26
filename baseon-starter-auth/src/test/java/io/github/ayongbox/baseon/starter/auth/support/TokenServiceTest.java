package io.github.ayongbox.baseon.starter.auth.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.starter.auth.config.BaseonAuthProperties;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private BaseonAuthProperties properties;
    private JwtTokenProvider provider;
    private RedisUtil redisUtil;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        properties = new BaseonAuthProperties();
        properties.setSecret(SECRET);
        provider = new JwtTokenProvider(properties);
        redisUtil = mock(RedisUtil.class);
        tokenService = new TokenService(provider, redisUtil, properties);
    }

    private static LoginUser demoUser() {
        LoginUser user = new LoginUser();
        user.setUserId(1001L);
        user.setUsername("admin");
        user.setTenantId(2001L);
        user.setPermissions(Set.of("user:create", "user:list"));
        return user;
    }

    private String refreshKey(String sessionId) {
        return properties.getRefreshKeyPrefix() + sessionId;
    }

    private String blacklistKey(String jti) {
        return properties.getBlacklistKeyPrefix() + jti;
    }

    @Test
    void verifyRestoresLoginUserWithPermissions() {
        TokenPair pair = tokenService.issueTokenPair(demoUser());

        LoginUser restored = tokenService.verify(pair.getAccessToken());

        assertThat(restored.getUserId()).isEqualTo(1001L);
        assertThat(restored.getUsername()).isEqualTo("admin");
        assertThat(restored.getTenantId()).isEqualTo(2001L);
        assertThat(restored.getPermissions()).containsExactlyInAnyOrder("user:create", "user:list");
    }

    @Test
    void refreshIssuesNewAccessTokenWhenSessionAlive() {
        TokenPair pair = tokenService.issueTokenPair(demoUser());
        String sessionId = provider.parse(pair.getRefreshToken()).getId();
        when(redisUtil.hasKey(refreshKey(sessionId))).thenReturn(true);

        String newAccessToken = tokenService.refresh(pair.getRefreshToken());

        Claims claims = provider.parse(newAccessToken);
        assertThat(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))
                .isEqualTo(JwtTokenProvider.TYPE_ACCESS);
        assertThat(claims.getSubject()).isEqualTo("1001");
        assertThat(claims.get(JwtTokenProvider.CLAIM_SID, String.class)).isEqualTo(sessionId);
        // 新 access token 必须使用新 jti，否则会被旧 token 的黑名单误伤
        assertThat(claims.getId()).isNotEqualTo(provider.parse(pair.getAccessToken()).getId());
    }

    @Test
    void refreshRejectsRevokedSession() {
        TokenPair pair = tokenService.issueTokenPair(demoUser());
        // RedisUtil mock 默认返回 null：会话 key 不存在 = 已吊销
        assertThatThrownBy(() -> tokenService.refresh(pair.getRefreshToken()))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void invalidatedAccessTokenRejectedByVerify() {
        TokenPair pair = tokenService.issueTokenPair(demoUser());
        tokenService.invalidate(pair.getAccessToken());

        String jti = provider.parse(pair.getAccessToken()).getId();
        verify(redisUtil).set(eq(blacklistKey(jti)), eq("1"), anyLong());

        // 黑名单命中（模拟 Redis 中已存在）后 verify 必须拒绝
        when(redisUtil.hasKey(blacklistKey(jti))).thenReturn(true);
        assertThatThrownBy(() -> tokenService.verify(pair.getAccessToken()))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void invalidateBlacklistsJtiWithRemainingTtlAndDeletesSession() {
        properties.setAccessExpireSeconds(3600);
        TokenPair pair = tokenService.issueTokenPair(demoUser());
        String accessJti = provider.parse(pair.getAccessToken()).getId();
        String sessionId = provider.parse(pair.getRefreshToken()).getId();

        tokenService.invalidate(pair.getAccessToken());

        // 黑名单 TTL 只需覆盖 token 剩余有效期，到期自动清出
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(redisUtil).set(eq(blacklistKey(accessJti)), eq("1"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isBetween(3599L, 3600L);
        // 同一会话的 refresh 凭证一并吊销
        verify(redisUtil).delete(refreshKey(sessionId));
    }
}
