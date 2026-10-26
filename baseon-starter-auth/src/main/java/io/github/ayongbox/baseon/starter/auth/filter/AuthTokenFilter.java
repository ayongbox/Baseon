package io.github.ayongbox.baseon.starter.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.starter.auth.config.BaseonAuthProperties;
import io.github.ayongbox.baseon.starter.auth.support.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Token 认证过滤器：解析 Bearer Token，校验通过后把登录用户写入 {@link SecurityUtil}。
 *
 * <p>放行策略：开关关闭、命中 {@code baseon.auth.ignore-paths}、未携带 Bearer 头，
 * 三种情况直接放行不报错——是否强制登录由下游 {@code @RequiresPermission} 决定，
 * 免登录接口因此天然可用。
 *
 * <p><b>注意</b>：Filter 抛出的异常到不了 {@code @RestControllerAdvice}
 * （异常发生在 DispatcherServlet 之前），校验失败必须在这里自写 401 JSON 响应。
 *
 * <p>注册顺序高于 starter-web 的拦截器：认证成功后 UserContextInterceptor
 * 发现已登录会跳过 Header 覆盖；finally 中的清理与拦截器的清理幂等共存。
 */
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final BaseonAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthTokenFilter(TokenService tokenService, BaseonAuthProperties properties,
                           ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveBearerToken(request);
            if (token != null) {
                LoginUser user = tokenService.verify(token);
                SecurityUtil.set(user);
            }
            filterChain.doFilter(request, response);
        } catch (BizException e) {
            writeUnauthorized(response, e.getMessage());
        } finally {
            SecurityUtil.clear();
        }
    }

    /**
     * 返回 null 表示本请求无需认证（开关关闭 / 白名单 / 无 Bearer 头），直接放行。
     */
    private String resolveBearerToken(HttpServletRequest request) {
        if (!properties.isEnabled() || isIgnored(request.getRequestURI())) {
            return null;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }

    private boolean isIgnored(String uri) {
        return properties.getIgnorePaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                R.fail(CommonErrorCode.UNAUTHORIZED.getCode(), message)));
    }
}
