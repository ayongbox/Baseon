package io.github.ayongbox.baseon.starter.cloud.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.ayongbox.baseon.core.cloud.CloudHeaders;
import io.github.ayongbox.baseon.core.context.GrayContext;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.starter.trace.TraceIdUtil;
import io.github.ayongbox.baseon.starter.trace.config.BaseonTraceProperties;
import io.github.ayongbox.baseon.starter.web.config.BaseonWebProperties;

/**
 * Feign 上下文透传拦截器：把当前请求的 traceId、登录用户、灰度标签写入下游请求头。
 *
 * <p>读取侧与写入侧共用同一套请求头名配置（starter-web / starter-trace 的 properties），
 * 保证全链路 Header 名一致。调用方已显式设置的同名请求头不会被覆盖。
 */
public class ContextPropagateRequestInterceptor implements RequestInterceptor {

    private final BaseonWebProperties webProperties;
    private final BaseonTraceProperties traceProperties;

    public ContextPropagateRequestInterceptor(BaseonWebProperties webProperties,
                                              BaseonTraceProperties traceProperties) {
        this.webProperties = webProperties;
        this.traceProperties = traceProperties;
    }

    @Override
    public void apply(RequestTemplate template) {
        putIfAbsent(template, traceProperties.getTraceIdHeader(), TraceIdUtil.get());

        LoginUser user = SecurityUtil.get();
        if (user != null) {
            BaseonWebProperties.UserContext userContext = webProperties.getUserContext();
            putIfAbsent(template, userContext.getUserIdHeader(),
                    user.getUserId() == null ? null : String.valueOf(user.getUserId()));
            putIfAbsent(template, userContext.getTenantIdHeader(),
                    user.getTenantId() == null ? null : String.valueOf(user.getTenantId()));
            putIfAbsent(template, userContext.getUsernameHeader(), user.getUsername());
        }

        putIfAbsent(template, CloudHeaders.GRAY_TAG_HEADER, GrayContext.get());
    }

    private void putIfAbsent(RequestTemplate template, String name, String value) {
        if (value == null || value.isBlank() || template.headers().containsKey(name)) {
            return;
        }
        template.header(name, value);
    }
}
