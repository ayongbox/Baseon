package io.github.ayongbox.baseon.demob;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.starter.trace.TraceIdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 下游验证接口：回显请求头与上下文，验证上游 Feign 透传是否完整。
 */
@RestController
@RequestMapping("/demo")
public class DemoBController {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private int port;

    @GetMapping("/ping")
    public R<Map<String, Object>> ping(
            @RequestHeader(name = "X-Trace-Id", required = false) String traceIdHeader,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(name = "X-Gray-Tag", required = false) String grayTagHeader) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", appName);
        body.put("port", port);

        // 线上原始请求头：验证 Feign 拦截器是否把上下文写到了请求里
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("X-Trace-Id", traceIdHeader);
        headers.put("X-User-Id", userIdHeader);
        headers.put("X-Gray-Tag", grayTagHeader);
        body.put("headers", headers);

        // 本地上下文：验证 starter-web / starter-trace 是否成功从请求头恢复上下文
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("traceId", TraceIdUtil.get());
        context.put("userId", SecurityUtil.getUserId());
        body.put("context", context);

        return R.ok(body);
    }
}
