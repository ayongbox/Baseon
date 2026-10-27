package io.github.ayongbox.baseon.starter.auth.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.starter.auth.config.BaseonAuthProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final BaseonAuthProperties properties = properties();
    private final JwtTokenProvider provider = new JwtTokenProvider(properties);

    private static BaseonAuthProperties properties() {
        BaseonAuthProperties properties = new BaseonAuthProperties();
        properties.setSecret(SECRET);
        return properties;
    }

    private static LoginUser demoUser() {
        LoginUser user = new LoginUser();
        user.setUserId(1001L);
        user.setUsername("admin");
        user.setTenantId(2001L);
        user.setPermissions(Set.of("user:create", "user:list"));
        return user;
    }

    @Test
    void accessTokenRoundTripRestoresAllClaims() {
        String token = provider.createAccessToken(demoUser(), "session-1");

        Claims claims = provider.parse(token);

        assertThat(claims.getSubject()).isEqualTo("1001");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))
                .isEqualTo(JwtTokenProvider.TYPE_ACCESS);
        assertThat(claims.get(JwtTokenProvider.CLAIM_SID, String.class)).isEqualTo("session-1");
        assertThat(claims.get(JwtTokenProvider.CLAIM_USERNAME, String.class)).isEqualTo("admin");
        assertThat(((Number) claims.get(JwtTokenProvider.CLAIM_TENANT_ID)).longValue()).isEqualTo(2001L);
        Object perms = claims.get(JwtTokenProvider.CLAIM_PERMS);
        assertThat(perms).isInstanceOf(List.class);
        assertThat(((List<?>) perms).stream().map(String::valueOf).toList())
                .containsExactlyInAnyOrder("user:create", "user:list");
    }

    @Test
    void tamperedSignatureThrowsUnauthorized() {
        String token = provider.createAccessToken(demoUser(), "session-1");
        // 篡改签名段倒数第二个字符：Base64URL 末字符含 2bit 填充位，
        // 改末字符可能不改变解码后的字节（签名仍合法，测试偶发不失败），
        // 倒数第二个字符的 6bit 全部是有效数据，改动必然破坏签名
        int pos = token.length() - 2;
        char replacement = token.charAt(pos) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, pos) + replacement + token.substring(pos + 1);

        assertThatThrownBy(() -> provider.parse(tampered))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void expiredTokenThrowsUnauthorized() {
        properties.setAccessExpireSeconds(-10);
        String token = provider.createAccessToken(demoUser(), "session-1");

        assertThatThrownBy(() -> provider.parse(token))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shortOrMissingSecretFailsFast() {
        BaseonAuthProperties shortSecret = properties();
        shortSecret.setSecret("too-short");
        assertThatThrownBy(() -> new JwtTokenProvider(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("baseon.auth.secret");

        BaseonAuthProperties emptySecret = properties();
        emptySecret.setSecret("");
        assertThatThrownBy(() -> new JwtTokenProvider(emptySecret))
                .isInstanceOf(IllegalStateException.class);
    }
}
