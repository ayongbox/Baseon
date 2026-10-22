package io.github.ayongbox.baseon.starter.web.interceptor;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.starter.web.config.BaseonWebProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserContextInterceptorTest {

    private final UserContextInterceptor interceptor =
            new UserContextInterceptor(new BaseonWebProperties());

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    void should_parse_user_headers_into_context() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1001");
        request.addHeader("X-Tenant-Id", "2002");
        request.addHeader("X-Username", "alice");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(1001L, SecurityUtil.getUserId());
        assertEquals(2002L, SecurityUtil.getTenantId());
        assertEquals("alice", SecurityUtil.getUsername());
    }

    @Test
    void should_clear_context_after_completion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1001");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertFalse(SecurityUtil.isLogin());
        assertNull(SecurityUtil.get());
    }

    @Test
    void should_stay_anonymous_without_headers() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertNull(SecurityUtil.get());
    }

    @Test
    void should_ignore_malformed_user_id() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-number");
        request.addHeader("X-Tenant-Id", "2002");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertNull(SecurityUtil.getUserId());
        assertEquals(2002L, SecurityUtil.getTenantId());
    }
}
