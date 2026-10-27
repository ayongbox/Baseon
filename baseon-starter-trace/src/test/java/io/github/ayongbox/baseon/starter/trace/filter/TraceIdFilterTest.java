package io.github.ayongbox.baseon.starter.trace.filter;

import io.github.ayongbox.baseon.starter.trace.TraceIdUtil;
import io.github.ayongbox.baseon.starter.trace.config.BaseonTraceProperties;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter(new BaseonTraceProperties());

    @AfterEach
    void tearDown() {
        TraceIdUtil.clear();
    }

    @Test
    void should_generate_trace_id_when_header_missing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        String traceId = response.getHeader("X-Trace-Id");
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        assertNull(TraceIdUtil.get()); // 请求结束后已清理
    }

    @Test
    void should_reuse_incoming_trace_id() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals("abc123", response.getHeader("X-Trace-Id"));
    }

    @Test
    void should_not_set_response_header_when_disabled() throws ServletException, IOException {
        BaseonTraceProperties properties = new BaseonTraceProperties();
        properties.setResponseHeaderEnabled(false);
        TraceIdFilter disabledFilter = new TraceIdFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        disabledFilter.doFilterInternal(request, response, chain);

        assertNull(response.getHeader("X-Trace-Id"));
    }
}
