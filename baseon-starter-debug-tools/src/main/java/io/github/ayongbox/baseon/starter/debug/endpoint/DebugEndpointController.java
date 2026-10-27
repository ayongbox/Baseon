package io.github.ayongbox.baseon.starter.debug.endpoint;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.starter.debug.config.BaseonDebugProperties;
import io.github.ayongbox.baseon.starter.debug.config.DebugProdProfileGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开发调试端点：暴露当前登录上下文、traceId 与生效配置，供联调自检。
 *
 * <p>设计意图：这些端点返回的信息（用户标识、权限集合、激活 profile）在生产环境
 * 属于信息泄露面，因此生产失效是「硬失效」而非可配置关闭——装配期由
 * {@code NotProdProfileCondition} 拦截（第一层），这里每个端点入口再执行一次
 * {@link DebugProdProfileGuard#isProdActive} 二次拦截（第二层），即使装配被误判放行，
 * 运行期也只会得到 404 语义（{@link BizException} 携带 {@link CommonErrorCode#NOT_FOUND}，
 * 由 starter-web 的全局异常处理映射为 HTTP 404），对外表现为「端点不存在」。
 *
 * <p>本类不标注组件扫描注解，由自动配置以 {@code @Bean} 注册，未启用时端点不存在。
 */
@RestController
@RequestMapping("/debug")
public class DebugEndpointController {

    private final Environment environment;
    private final BaseonDebugProperties properties;

    public DebugEndpointController(Environment environment, BaseonDebugProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    /**
     * 当前请求上下文自检：traceId、登录用户信息与请求头摘要（仅 Host/UA）。
     */
    @GetMapping("/context")
    public R<Map<String, Object>> context(HttpServletRequest request) {
        rejectIfProd();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("traceId", MDC.get("traceId"));
        LoginUser user = SecurityUtil.get();
        data.put("userId", user == null ? null : user.getUserId());
        data.put("username", user == null ? null : user.getUsername());
        data.put("tenantId", user == null ? null : user.getTenantId());
        data.put("permissions", user == null ? null : user.getPermissions());
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", request.getHeader("Host"));
        headers.put("userAgent", request.getHeader("User-Agent"));
        data.put("headers", headers);
        return R.ok(data);
    }

    /**
     * 配置自检：激活的 profile 列表与 {@code baseon.debug.enabled} 生效值。
     */
    @GetMapping("/profiles")
    public R<Map<String, Object>> profiles() {
        rejectIfProd();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activeProfiles", environment.getActiveProfiles());
        data.put("debugEnabled", properties.isEnabled());
        return R.ok(data);
    }

    /**
     * 生产硬失效第二层：命中生产 profile 时抛 404 语义的业务异常，
     * 对外与「端点不存在」无差别，不泄露任何调试信息。
     */
    private void rejectIfProd() {
        if (DebugProdProfileGuard.isProdActive(environment, properties.getProdProfiles())) {
            throw new BizException(CommonErrorCode.NOT_FOUND);
        }
    }
}
