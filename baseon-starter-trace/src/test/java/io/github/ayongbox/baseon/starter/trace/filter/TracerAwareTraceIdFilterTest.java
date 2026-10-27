package io.github.ayongbox.baseon.starter.trace.filter;

import io.github.ayongbox.baseon.starter.trace.TraceIdUtil;
import io.github.ayongbox.baseon.starter.trace.config.BaseonTraceProperties;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TracerAwareTraceIdFilterTest {

    private static final String OTEL_TRACE_ID = "0af7651916cd43dd8448eb211c80319c";

    @AfterEach
    void tearDown() {
        TraceIdUtil.clear();
    }

    @Test
    void should_reuse_current_span_trace_id() throws ServletException, IOException {
        Tracer tracer = tracerWithCurrentSpan();
        TracerAwareTraceIdFilter filter = new TracerAwareTraceIdFilter(new BaseonTraceProperties(), tracer);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(OTEL_TRACE_ID, response.getHeader("X-Trace-Id"));
        // 复用路径不清理 MDC，交给 micrometer scope 恢复（此处模拟 scope 未关闭，值仍在）
        assertEquals(OTEL_TRACE_ID, TraceIdUtil.get());
    }

    @Test
    void should_fall_back_to_own_trace_id_when_no_current_span() throws ServletException, IOException {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);
        TracerAwareTraceIdFilter filter = new TracerAwareTraceIdFilter(new BaseonTraceProperties(), tracer);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        String traceId = response.getHeader("X-Trace-Id");
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        assertNull(TraceIdUtil.get()); // 回退路径请求结束后已清理
    }

    @Test
    void should_inherit_header_when_falling_back() throws ServletException, IOException {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);
        TracerAwareTraceIdFilter filter = new TracerAwareTraceIdFilter(new BaseonTraceProperties(), tracer);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "legacy-upstream-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals("legacy-upstream-id", response.getHeader("X-Trace-Id"));
    }

    private Tracer tracerWithCurrentSpan() {
        TraceContext context = mock(TraceContext.class);
        when(context.traceId()).thenReturn(OTEL_TRACE_ID);
        Span span = mock(Span.class);
        when(span.context()).thenReturn(context);
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(span);
        return tracer;
    }
}
