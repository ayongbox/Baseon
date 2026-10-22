package io.github.ayongbox.baseon.starter.trace.filter;

import io.github.ayongbox.baseon.starter.trace.TraceIdUtil;
import io.github.ayongbox.baseon.starter.trace.config.BaseonTraceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceId 过滤器：请求入口生成/继承 TraceId，写入 MDC，响应头返回，请求结束清理。
 *
 * <p>执行顺序在 Spring Security 之前，确保后续所有日志（包括认证失败日志）都带 traceId。
 * 与 {@code UserContextInterceptor} 的分工：本 Filter 只管 traceId，用户上下文由后者负责。
 */
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    private final BaseonTraceProperties properties;

    public TraceIdFilter(BaseonTraceProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
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
