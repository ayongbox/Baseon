package io.github.ayongbox.baseon.starter.cloud.feign.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class FeignDefaultsEnvironmentPostProcessorTest {

    private final FeignDefaultsEnvironmentPostProcessor processor = new FeignDefaultsEnvironmentPostProcessor();

    @Test
    void injectsDefaultTimeouts() {
        MockEnvironment environment = new MockEnvironment();
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("feign.client.config.default.connect-timeout")).isEqualTo("3000");
        assertThat(environment.getProperty("feign.client.config.default.read-timeout")).isEqualTo("5000");
    }

    @Test
    void baseonPropertiesAdjustDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.cloud.feign.connect-timeout", "1000")
                .withProperty("baseon.cloud.feign.read-timeout", "2000");
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("feign.client.config.default.connect-timeout")).isEqualTo("1000");
        assertThat(environment.getProperty("feign.client.config.default.read-timeout")).isEqualTo("2000");
    }

    @Test
    void explicitFeignConfigWins() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("feign.client.config.default.connect-timeout", "9999");
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("feign.client.config.default.connect-timeout")).isEqualTo("9999");
    }
}
