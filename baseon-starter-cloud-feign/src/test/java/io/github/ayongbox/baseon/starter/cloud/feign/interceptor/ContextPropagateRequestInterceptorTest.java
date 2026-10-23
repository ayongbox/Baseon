package io.github.ayongbox.baseon.starter.cloud.feign.interceptor;

import feign.RequestTemplate;
import io.github.ayongbox.baseon.core.cloud.CloudHeaders;
import io.github.ayongbox.baseon.core.context.GrayContext;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.starter.trace.TraceIdUtil;
import io.github.ayongbox.baseon.starter.trace.config.BaseonTraceProperties;
import io.github.ayongbox.baseon.starter.web.config.BaseonWebProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextPropagateRequestInterceptorTest {

    private final ContextPropagateRequestInterceptor interceptor =
            new ContextPropagateRequestInterceptor(new BaseonWebProperties(), new BaseonTraceProperties());

    @AfterEach
    void tearDown() {
        TraceIdUtil.clear();
        SecurityUtil.clear();
        GrayContext.clear();
    }

    @Test
    void propagatesTraceUserAndGrayTag() {
        TraceIdUtil.set("trace-abc");

        LoginUser user = new LoginUser();
        user.setUserId(1001L);
        user.setTenantId(2002L);
        user.setUsername("ayong");
        SecurityUtil.set(user);

        GrayContext.set("gray");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("X-Trace-Id")).containsExactly("trace-abc");
        assertThat(template.headers().get("X-User-Id")).containsExactly("1001");
        assertThat(template.headers().get("X-Tenant-Id")).containsExactly("2002");
        assertThat(template.headers().get("X-Username")).containsExactly("ayong");
        assertThat(template.headers().get(CloudHeaders.GRAY_TAG_HEADER)).containsExactly("gray");
    }

    @Test
    void skipsHeadersWhenContextEmpty() {
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).isEmpty();
    }

    @Test
    void doesNotOverrideExplicitHeaders() {
        TraceIdUtil.set("trace-abc");

        RequestTemplate template = new RequestTemplate();
        template.header("X-Trace-Id", "caller-fixed");
        interceptor.apply(template);

        assertThat(template.headers().get("X-Trace-Id")).containsExactly("caller-fixed");
    }
}
