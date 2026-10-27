package io.github.ayongbox.baseon.starter.debug.endpoint;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.starter.debug.config.BaseonDebugProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DebugEndpointController 的端点行为与生产环境二次拦截（第二层硬失效）。
 */
class DebugEndpointControllerTest {

    @AfterEach
    void tearDown() {
        // 清理线程上下文，避免污染其他测试
        MDC.clear();
        SecurityUtil.clear();
    }

    private DebugEndpointController controller(MockEnvironment environment) {
        return new DebugEndpointController(environment, new BaseonDebugProperties());
    }

    @Test
    void contextReturnsTraceIdAndLoginUserAndHeaderDigest() {
        // Given：dev 环境，写入 traceId 与登录用户
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        MDC.put("traceId", "trace-001");
        LoginUser user = new LoginUser();
        user.setUserId(1001L);
        user.setUsername("ayong");
        user.setTenantId(9L);
        user.setPermissions(Set.of("user:create"));
        SecurityUtil.set(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "localhost:8080");
        request.addHeader("User-Agent", "curl/8.0");

        // When：调用 /debug/context
        R<Map<String, Object>> result = controller(environment).context(request);

        // Then：返回 traceId、登录信息与请求头摘要
        assertTrue(result.success());
        Map<String, Object> data = result.getData();
        assertEquals("trace-001", data.get("traceId"));
        assertEquals(1001L, data.get("userId"));
        assertEquals("ayong", data.get("username"));
        assertEquals(9L, data.get("tenantId"));
        assertEquals(Set.of("user:create"), data.get("permissions"));
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) data.get("headers");
        assertEquals("localhost:8080", headers.get("host"));
        assertEquals("curl/8.0", headers.get("userAgent"));
    }

    @Test
    void contextReturnsNullFieldsWhenNoLoginContext() {
        // Given：dev 环境，无登录上下文、无 traceId
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        // When：调用 /debug/context
        R<Map<String, Object>> result = controller(environment).context(new MockHttpServletRequest());

        // Then：用户相关字段为 null，不抛异常
        Map<String, Object> data = result.getData();
        assertNull(data.get("traceId"));
        assertNull(data.get("userId"));
        assertNull(data.get("permissions"));
    }

    @Test
    void profilesReturnsActiveProfilesAndEnabledFlag() {
        // Given：dev 环境
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        // When：调用 /debug/profiles
        R<Map<String, Object>> result = controller(environment).profiles();

        // Then：返回激活 profile 与 enabled 生效值
        Map<String, Object> data = result.getData();
        assertArrayEquals(new String[]{"dev"}, (String[]) data.get("activeProfiles"));
        assertEquals(true, data.get("debugEnabled"));
    }

    @Test
    void contextEndpointIsBlockedWithNotFoundWhenProdActive() {
        // Given：prod 环境（模拟装配期误判放行后的运行期场景）
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        DebugEndpointController controller = controller(environment);

        // When：调用 /debug/context
        // Then：二次拦截抛出 404 语义的业务异常
        BizException exception = assertThrows(BizException.class,
                () -> controller.context(new MockHttpServletRequest()));
        assertEquals(CommonErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void profilesEndpointIsBlockedWithNotFoundWhenProdActive() {
        // Given：prod 环境
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        DebugEndpointController controller = controller(environment);

        // When：调用 /debug/profiles
        // Then：二次拦截抛出 404 语义的业务异常
        BizException exception = assertThrows(BizException.class, controller::profiles);
        assertEquals(CommonErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }
}
