package io.github.ayongbox.baseon.starter.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.starter.auth.config.BaseonAuthProperties;
import io.github.ayongbox.baseon.starter.auth.support.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthTokenFilterTest {

    private TokenService tokenService;
    private AuthTokenFilter filter;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        BaseonAuthProperties properties = new BaseonAuthProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        filter = new AuthTokenFilter(tokenService, properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    void passesThroughWithoutBearerHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/demo/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // 无 Bearer 头直接放行：链被调用、不触碰 TokenService、上下文保持为空
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(tokenService);
        assertThat(SecurityUtil.get()).isNull();
    }

    @Test
    void passesThroughOnIgnoredPathEvenWithBadToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(tokenService);
    }

    @Test
    void writes401JsonWhenTokenInvalid() throws Exception {
        when(tokenService.verify("bad-token"))
                .thenThrow(new BizException(CommonErrorCode.UNAUTHORIZED, "凭证无效"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/demo/hello");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // Filter 异常到不了 @RestControllerAdvice，必须自写 401 JSON，且链中断
        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"code\":1002");
        assertThat(SecurityUtil.get()).isNull();
    }
}
