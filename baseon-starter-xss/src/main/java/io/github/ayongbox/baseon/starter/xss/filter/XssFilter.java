package io.github.ayongbox.baseon.starter.xss.filter;

import io.github.ayongbox.baseon.starter.xss.config.BaseonXssProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * XSS 过滤器：对命中请求用 {@link XssRequestWrapper} 包装后放行。
 *
 * <p>开关关闭或命中排除路径时原样放行，不做任何包装，保证例外通道零损耗。
 * 排除路径用于富文本提交等「业务自己管转义」的接口。
 */
public class XssFilter extends OncePerRequestFilter {

    private final BaseonXssProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public XssFilter(BaseonXssProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || isExcluded(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new XssRequestWrapper(request), response);
    }

    private boolean isExcluded(String uri) {
        return properties.getExcludePaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
