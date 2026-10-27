package io.github.ayongbox.baseon.starter.tracing.otel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenTelemetry 分布式追踪配置，前缀 {@code baseon.tracing.otel}。
 */
@ConfigurationProperties(prefix = "baseon.tracing.otel")
public class BaseonTracingOtelProperties {

    /** 本模块定制 Bean 总开关（OTel 官方装配本身由 management.tracing.enabled 控制） */
    private boolean enabled = true;

    /**
     * 本地开发档：span 通过 LoggingSpanExporter 立即输出到日志，无需部署 collector。
     * 仅本地调试用；生产应关闭本开关并配置 management.otlp.tracing.endpoint 上报。
     */
    private boolean logExporterEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogExporterEnabled() {
        return logExporterEnabled;
    }

    public void setLogExporterEnabled(boolean logExporterEnabled) {
        this.logExporterEnabled = logExporterEnabled;
    }
}
