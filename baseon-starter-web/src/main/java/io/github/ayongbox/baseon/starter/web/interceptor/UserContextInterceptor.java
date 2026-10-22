package io.github.ayongbox.baseon.starter.web.interceptor;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.tool.StrUtil;
import io.github.ayongbox.baseon.starter.web.config.BaseonWebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录上下文拦截器：从请求头解析用户信息写入 {@link SecurityUtil}，请求结束后清理。
 *
 * <p><b>安全前提</b>：这些 Header 必须由网关校验 JWT 后注入。服务直连公网时，
 * 任何人都可以伪造用户身份，因此生产部署必须禁止业务服务直接暴露。
 */
public class UserContextInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(UserContextInterceptor.class);

    private final BaseonWebProperties properties;

    public UserContextInterceptor(BaseonWebProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        BaseonWebProperties.UserContext config = properties.getUserContext();
        if (!config.isEnabled()) {
            return true;
        }

        String userId = request.getHeader(config.getUserIdHeader());
        String tenantId = request.getHeader(config.getTenantIdHeader());
        String username = request.getHeader(config.getUsernameHeader());

        if (StrUtil.isBlank(userId) && StrUtil.isBlank(tenantId) && StrUtil.isBlank(username)) {
            SecurityUtil.clear();
            return true;
        }

        LoginUser user = new LoginUser();
        user.setUserId(parseLong(userId, config.getUserIdHeader()));
        user.setTenantId(parseLong(tenantId, config.getTenantIdHeader()));
        user.setUsername(StrUtil.isBlank(username) ? null : username);
        SecurityUtil.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        if (properties.getUserContext().isEnabled()) {
            SecurityUtil.clear();
        }
    }

    private Long parseLong(String value, String headerName) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            log.warn("请求头 {} 不是合法数字，已忽略: {}", headerName, value);
            return null;
        }
    }
}
