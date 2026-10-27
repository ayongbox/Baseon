package io.github.ayongbox.baseon.starter.swagger.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProdProfileSwaggerDisabler 端点硬关闭测试。
 */
class ProdProfileSwaggerDisablerTest {

    private final ProdProfileSwaggerDisabler disabler = new ProdProfileSwaggerDisabler();

    @Test
    void injectsTwoDisabledPropertiesWhenProdActive() {
        // Given：激活 prod profile 的环境
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        // When：执行环境后处理
        disabler.postProcessEnvironment(environment, null);

        // Then：两个 springdoc 开关被塞成 false
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
        // Then：属性源放最前，优先级最高
        assertThat(environment.getPropertySources().iterator().next().getName())
                .isEqualTo("baseonSwaggerProdDisabler");
    }

    @Test
    void injectedFalseOverridesMisconfiguredTrueInExistingPropertySource() {
        // Given：prod 环境且已有属性源误把 springdoc 打开（模拟 Nacos/yml 误配置）
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.getPropertySources().addFirst(new MapPropertySource("misconfigured",
                Map.of("springdoc.api-docs.enabled", "true",
                        "springdoc.swagger-ui.enabled", "true")));

        // When：执行环境后处理
        disabler.postProcessEnvironment(environment, null);

        // Then：误配置被强制覆盖为 false
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
    }

    @Test
    void doesNotInjectWhenOnlyDevActive() {
        // Given：仅激活 dev 的环境
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        // When：执行环境后处理
        disabler.postProcessEnvironment(environment, null);

        // Then：不塞任何关闭属性
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isNull();
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isNull();
        assertThat(environment.getPropertySources().stream()
                .noneMatch(source -> "baseonSwaggerProdDisabler".equals(source.getName()))).isTrue();
    }

    @Test
    void injectsWhenCustomProdProfileMatched() {
        // Given：激活 production，且配置自定义名单包含 production
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.swagger.prod-profiles[0]", "production");
        environment.setActiveProfiles("production");

        // When：执行环境后处理
        disabler.postProcessEnvironment(environment, null);

        // Then：自定义名单同样触发硬关闭
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
    }
}
