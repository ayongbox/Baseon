package io.github.ayongbox.baseon.starter.tracing.otel.config;

import io.opentelemetry.sdk.trace.SpanProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BaseonTracingOtelAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseonTracingOtelAutoConfiguration.class));

    @Test
    void should_not_register_logging_processor_by_default() {
        runner.run(context ->
                assertThat(context).doesNotHaveBean(SpanProcessor.class));
    }

    @Test
    void should_register_logging_processor_when_log_exporter_enabled() {
        runner.withPropertyValues("baseon.tracing.otel.log-exporter-enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(SpanProcessor.class));
    }

    @Test
    void should_back_off_when_module_disabled() {
        runner.withPropertyValues(
                        "baseon.tracing.otel.enabled=false",
                        "baseon.tracing.otel.log-exporter-enabled=true")
                .run(context ->
                        assertThat(context).doesNotHaveBean(SpanProcessor.class));
    }
}
