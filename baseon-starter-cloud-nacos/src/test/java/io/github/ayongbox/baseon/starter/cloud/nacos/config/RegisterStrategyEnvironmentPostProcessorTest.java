package io.github.ayongbox.baseon.starter.cloud.nacos.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterStrategyEnvironmentPostProcessorTest {

    private final RegisterStrategyEnvironmentPostProcessor processor = new RegisterStrategyEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    void fullRegisterIsDefaultAndAddsNothing() {
        MockEnvironment environment = new MockEnvironment();
        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.cloud.nacos.discovery.register-enabled")).isNull();
        assertThat(environment.getPropertySources().get(RegisterStrategyEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isNull();
    }

    @Test
    void localNoRegisterDisablesRegistration() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.cloud.nacos.register-strategy", "local-no-register");
        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.cloud.nacos.discovery.register-enabled")).isEqualTo("false");
    }

    @Test
    void localRegisterPrivateMarksLocalMetadata() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.cloud.nacos.register-strategy", "LOCAL_REGISTER_PRIVATE");
        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.cloud.nacos.discovery.register-enabled")).isEqualTo("true");
        assertThat(environment.getProperty("spring.cloud.nacos.discovery.metadata[baseon.local]")).isEqualTo("true");
    }

    @Test
    void explicitUserConfigWinsOverStrategy() {
        // 用户显式打开注册开关时，即使策略是 LOCAL_NO_REGISTER 也以用户配置为准
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.cloud.nacos.register-strategy", "LOCAL_NO_REGISTER")
                .withProperty("spring.cloud.nacos.discovery.register-enabled", "true");
        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.cloud.nacos.discovery.register-enabled")).isEqualTo("true");
    }
}
