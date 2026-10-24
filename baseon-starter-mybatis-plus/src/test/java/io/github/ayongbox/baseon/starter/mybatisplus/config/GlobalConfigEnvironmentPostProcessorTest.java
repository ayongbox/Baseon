package io.github.ayongbox.baseon.starter.mybatisplus.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalConfigEnvironmentPostProcessorTest {

    private final GlobalConfigEnvironmentPostProcessor processor = new GlobalConfigEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    void providesMybatisPlusGlobalDefaults() {
        MockEnvironment environment = new MockEnvironment();
        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("mybatis-plus.global-config.db-config.logic-delete-field"))
                .isEqualTo("deleted");
        assertThat(environment.getProperty("mybatis-plus.global-config.db-config.logic-delete-value"))
                .isEqualTo("1");
        assertThat(environment.getProperty("mybatis-plus.global-config.db-config.logic-not-delete-value"))
                .isEqualTo("0");
        assertThat(environment.getProperty("mybatis-plus.configuration.map-underscore-to-camel-case"))
                .isEqualTo("true");
    }

    @Test
    void explicitUserConfigWinsOverDefaults() {
        // 用户显式声明逻辑删除值时，以用户配置为准，默认值只在缺省时兜底
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mybatis-plus.global-config.db-config.logic-delete-value", "true");
        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("mybatis-plus.global-config.db-config.logic-delete-value"))
                .isEqualTo("true");
    }
}
