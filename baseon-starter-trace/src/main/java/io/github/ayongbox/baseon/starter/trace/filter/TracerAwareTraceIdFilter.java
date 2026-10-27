package io.github.ayongbox.baseon.starter.trace.filter;

import io.github.ayongbox.baseon.starter.trace.TraceIdUtil;
import io.github.ayongbox.baseon.starter.trace.config.BaseonTraceProperties;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 全链路追踪在场的 TraceId 过滤器：复用当前 span 的 traceId，不再自生成。
 *
 * <p>装配条件：classpath 存在 micrometer-tracing（典型为引入 baseon-starter-tracing-otel）。
 * Boot 的 {@code ServerHttpObservationFilter} 顺序为 {@code HIGHEST_PRECEDENCE + 1}，
 * 先于本过滤器执行，故 tracing 启用时 {@link Tracer#currentSpan()} 稳定可得；复用其
 * traceId 后，日志（%X{traceId} 与 micrometer 写 MDC 的 key 同名）、响应头、追踪后端、
 * 下游服务四处 ID 一致。此时 MDC 生命周期交给 micrometer 管理（scope 关闭时自行恢复），
 * 本过滤器不清理。
 *
 * <p>无当前 span（tracing 被禁用、{@code management.tracing.enabled=false} 导致 Tracer
 * 为 NOOP 等）时回退自有 ID 逻辑：生成/继承 traceId、写 MDC、请求结束清理。
 */
public class TracerAwareTraceIdFilter extends OncePerRequestFilter {

    private final BaseonTraceProperties properties;

    private final Tracer tracer;

    public TracerAwareTraceIdFilter(BaseonTraceProperties properties, Tracer tracer) {
        this.properties = properties;
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Span current = tracer.currentSpan();
        if (current != null) {
            String traceId = current.context().traceId();
            TraceIdUtil.set(traceId);
            if (properties.isResponseHeaderEnabled()) {
                response.setHeader(properties.getTraceIdHeader(), traceId);
            }
            filterChain.doFilter(request, response);
            return;
        }
        String header = request.getHeader(properties.getTraceIdHeader());
        String traceId = TraceIdUtil.set(header);
        if (properties.isResponseHeaderEnabled()) {
            response.setHeader(properties.getTraceIdHeader(), traceId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdUtil.clear();
        }
    }
}
