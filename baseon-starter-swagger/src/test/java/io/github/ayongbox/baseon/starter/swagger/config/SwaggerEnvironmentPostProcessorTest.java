package io.github.ayongbox.baseon.starter.swagger.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SwaggerEnvironmentPostProcessor 端点启停语义测试。
 *
 * <p>覆盖三种分支：显式关闭 / 生产硬关闭 / 非生产自动开启 Knife4j。
 */
class SwaggerEnvironmentPostProcessorTest {

    private final SwaggerEnvironmentPostProcessor processor = new SwaggerEnvironmentPostProcessor();

    @Test
    void prodActiveDisablesSpringdocAndKnife4j() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        processor.postProcessEnvironment(environment, null);

        // Then：springdoc 端点关闭
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
        // Then：Knife4j 关闭并切到生产模式
        assertThat(environment.getProperty("knife4j.enable")).isEqualTo("false");
        assertThat(environment.getProperty("knife4j.production")).isEqualTo("true");
        // Then：属性源放最前
        assertThat(environment.getPropertySources().iterator().next().getName())
                .isEqualTo("baseonSwaggerEndpointOverrides");
    }

    @Test
    void prodOverridesMisconfiguredTrue() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.getPropertySources().addFirst(new MapPropertySource("misconfigured",
                Map.of("springdoc.api-docs.enabled", "true",
                        "springdoc.swagger-ui.enabled", "true",
                        "knife4j.enable", "true")));

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("knife4j.enable")).isEqualTo("false");
    }

    @Test
    void devActiveEnablesKnife4j() {
        // Knife4j 4.x 的 enable 默认 false，非生产需由本处理器显式打开
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("knife4j.enable")).isEqualTo("true");
        // Then：springdoc 端点保持业务自身配置，不被本处理器改写
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isNull();
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isNull();
    }

    @Test
    void noProfileStillEnablesKnife4jByDefault() {
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("knife4j.enable")).isEqualTo("true");
    }

    @Test
    void explicitEnabledFalseDisablesKnife4jOnly() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.swagger.enabled", "false");
        environment.setActiveProfiles("dev");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("knife4j.enable")).isEqualTo("false");
        // 显式关闭只关 Knife4j，不动 springdoc 端点
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isNull();
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isNull();
    }

    @Test
    void customProdProfileTriggersHardDisable() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.swagger.prod-profiles[0]", "production");
        environment.setActiveProfiles("production");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("knife4j.enable")).isEqualTo("false");
    }

    @Test
    void orderIsLowestPrecedence() {
        assertThat(processor.getOrder()).isEqualTo(org.springframework.core.Ordered.LOWEST_PRECEDENCE);
    }
}
