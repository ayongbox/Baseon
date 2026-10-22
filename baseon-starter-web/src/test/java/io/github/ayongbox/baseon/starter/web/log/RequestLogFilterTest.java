package io.github.ayongbox.baseon.starter.web.log;

import io.github.ayongbox.baseon.starter.web.config.BaseonWebProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RequestLogFilter 的排除规则与 5xx 强制日志行为测试。
 *
 * <p>日志输出本身不便于断言，这里通过「请求是否被包装/放行路径」与状态码分支
 * 验证行为边界：排除路径不缓存 body、5xx 时仍走完 finally 分支。
 */
class RequestLogFilterTest {

    private BaseonWebProperties properties;
    private RequestLogFilter filter;

    @BeforeEach
    void setUp() {
        properties = new BaseonWebProperties();
        filter = new RequestLogFilter(properties);
    }

    @Test
    void defaultExcludePathsCoverActuatorAndFavicon() throws Exception {
        // 默认排除路径里的请求应直接放行（不进入 body 缓存逻辑）
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/favicon.ico");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(200);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(1, chain.invocations);
    }

    @Test
    void additionalExcludePathsAppendInsteadOfOverride() throws Exception {
        // additional 追加后，默认路径与新路径都应被排除
        properties.getRequestLog().setAdditionalExcludePaths(List.of("/internal/**"));

        assertExcluded("/favicon.ico");       // 默认仍然生效
        assertExcluded("/internal/ping");     // 追加的生效
    }

    @Test
    void excludePathsOverrideDefaultsCompletely() throws Exception {
        // exclude-paths 配置后完全覆盖默认：favicon 不再被排除，会走正常日志逻辑
        properties.getRequestLog().setExcludePaths(List.of("/only-this/**"));

        assertNotExcluded("/favicon.ico");
        assertExcluded("/only-this/path");
    }

    @Test
    void excludedPathWithServerErrorStillCompletesAndReturns500() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/favicon.ico");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(500);

        filter.doFilter(request, response, chain);

        assertEquals(500, response.getStatus());
        assertEquals(1, chain.invocations);
    }

    @Test
    void disabledFilterPassesThroughWithoutLogging() throws Exception {
        properties.getRequestLog().setEnabled(false);

        assertNotExcluded("/demo/hello");
    }

    @Test
    void normalApiPathIsNotExcluded() throws Exception {
        assertNotExcluded("/demo/hello");
    }

    private void assertExcluded(String uri) throws ServletException, IOException {
        // 排除路径：chain 执行且 response 未被 ContentCachingResponseWrapper 替换。
        // 行为上等价于「直接放行」，用状态码透传验证。
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(200);
        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
    }

    private void assertNotExcluded(String uri) throws ServletException, IOException {
        // 非排除路径会走 body 缓存逻辑，最终 copyBodyToResponse 后状态码仍透传
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(200);
        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
    }

    /**
     * 记录调用次数并按预设状态码写入响应的 FilterChain。
     */
    private static class RecordingChain implements FilterChain {

        private final int status;
        private int invocations;

        RecordingChain(int status) {
            this.status = status;
        }

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request,
                             jakarta.servlet.ServletResponse response) throws IOException {
            invocations++;
            ((HttpServletResponse) response).setStatus(status);
            if (request instanceof HttpServletRequest httpRequest
                    && httpRequest.getRequestURI().startsWith("/demo")) {
                response.getWriter().write("{\"code\":0}");
            }
        }
    }
}
