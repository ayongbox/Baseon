package io.github.ayongbox.baseon.starter.trace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 链路追踪配置，前缀 {@code baseon.trace}。
 */
@ConfigurationProperties(prefix = "baseon.trace")
public class BaseonTraceProperties {

    /** 是否启用 TraceId 过滤器 */
    private boolean enabled = true;

    /** 请求头名称，也作为 MDC key */
    private String traceIdHeader = "X-Trace-Id";

    /** 是否在响应头返回 TraceId */
    private boolean responseHeaderEnabled = true;

    /** 线程池包装开关 */
    private final Async async = new Async();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTraceIdHeader() {
        return traceIdHeader;
    }

    public void setTraceIdHeader(String traceIdHeader) {
        this.traceIdHeader = traceIdHeader;
    }

    public boolean isResponseHeaderEnabled() {
        return responseHeaderEnabled;
    }

    public void setResponseHeaderEnabled(boolean responseHeaderEnabled) {
        this.responseHeaderEnabled = responseHeaderEnabled;
    }

    public Async getAsync() {
        return async;
    }

    public static class Async {

        /** 是否自动包装 ThreadPoolTaskExecutor */
        private boolean executorEnabled = true;

        /** 是否自动包装 @Async 注解方法 */
        private boolean annotationEnabled = true;

        /** 是否自动包装 @Scheduled 注解方法 */
        private boolean scheduledEnabled = true;

        public boolean isExecutorEnabled() {
            return executorEnabled;
        }

        public void setExecutorEnabled(boolean executorEnabled) {
            this.executorEnabled = executorEnabled;
        }

        public boolean isAnnotationEnabled() {
            return annotationEnabled;
        }

        public void setAnnotationEnabled(boolean annotationEnabled) {
            this.annotationEnabled = annotationEnabled;
        }

        public boolean isScheduledEnabled() {
            return scheduledEnabled;
        }

        public void setScheduledEnabled(boolean scheduledEnabled) {
            this.scheduledEnabled = scheduledEnabled;
        }
    }
}
