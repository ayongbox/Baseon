package io.github.ayongbox.baseon.starter.loadbalance.filter;

import io.github.ayongbox.baseon.core.cloud.CloudHeaders;
import io.github.ayongbox.baseon.core.context.GrayContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 灰度标签入口过滤器：解析 {@code X-Gray-Tag} 请求头写入 {@link GrayContext}，
 * 请求结束清理。与负载均衡、Feign 透传共用同一上下文。
 */
public class GrayContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        GrayContext.set(request.getHeader(CloudHeaders.GRAY_TAG_HEADER));
        try {
            filterChain.doFilter(request, response);
        } finally {
            GrayContext.clear();
        }
    }
}
