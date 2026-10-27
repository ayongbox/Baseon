package io.github.ayongbox.baseon.starter.tracing.otel.config;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * OpenTelemetry 分布式追踪自动装配。
 *
 * <p>本模块的主要价值是依赖聚合（actuator + micrometer-tracing-bridge-otel +
 * OTLP exporter）：引入后 Spring Boot 自动完成 OTel SDK、web 观测过滤器、
 * Feign 客户端观测与 W3C traceparent 传播的装配；starter-trace 检测到
 * {@code io.micrometer.tracing.Tracer} 后自动切换为复用全链路 traceId。
 *
 * <p>此处仅补充一个定制 Bean：本地开发档的日志导出 SpanProcessor。
 * Boot 的 {@code OpenTelemetryAutoConfiguration} 会收集容器中所有
 * {@link SpanProcessor} 加入 {@code SdkTracerProvider}。
 */
@AutoConfiguration
@ConditionalOnClass(SpanProcessor.class)
@EnableConfigurationProperties(BaseonTracingOtelProperties.class)
@ConditionalOnProperty(prefix = "baseon.tracing.otel", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class BaseonTracingOtelAutoConfiguration {

    /**
     * 本地开发档：span 结束时立即输出到日志（SimpleSpanProcessor 无批量延迟）。
     * 与 OTLP exporter 可并存；本地无 collector 时建议同时配置
     * {@code management.otlp.tracing.enabled=false} 关闭 OTLP 重试噪音。
     */
    @Bean
    @ConditionalOnProperty(prefix = "baseon.tracing.otel", name = "log-exporter-enabled",
            havingValue = "true")
    public SpanProcessor loggingSpanProcessor() {
        return SimpleSpanProcessor.create(LoggingSpanExporter.create());
    }
}
