package io.github.ayongbox.baseon.core.launch.config;

import io.github.ayongbox.baseon.core.launch.BaseonBannerPrinter;
import io.github.ayongbox.baseon.core.launch.GracefulShutdownListener;
import io.github.ayongbox.baseon.core.launch.LaunchCheck;
import io.github.ayongbox.baseon.core.launch.LaunchCheckExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BaseonLaunchAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseonLaunchAutoConfiguration.class));

    @Test
    void should_register_all_beans_by_default() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(BaseonBannerPrinter.class);
            assertThat(context).hasSingleBean(GracefulShutdownListener.class);
            assertThat(context).hasSingleBean(LaunchCheckExecutor.class);
            assertThat(context).hasBean("requiredConfigurationCheck");
        });
    }

    @Test
    void should_back_off_when_launch_disabled() {
        runner.withPropertyValues("baseon.launch.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(BaseonBannerPrinter.class);
            assertThat(context).doesNotHaveBean(GracefulShutdownListener.class);
            assertThat(context).doesNotHaveBean(LaunchCheckExecutor.class);
        });
    }

    @Test
    void should_skip_banner_printer_when_banner_disabled() {
        runner.withPropertyValues("baseon.launch.banner-enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(BaseonBannerPrinter.class);
            assertThat(context).hasSingleBean(GracefulShutdownListener.class);
        });
    }

    @Test
    void should_allow_business_to_override_required_configuration_check() {
        LaunchCheck custom = env -> {
        };
        runner.withBean("requiredConfigurationCheck", LaunchCheck.class, () -> custom).run(context -> {
            assertThat(context).hasBean("requiredConfigurationCheck");
            assertThat(context.getBean("requiredConfigurationCheck")).isSameAs(custom);
        });
    }
}
