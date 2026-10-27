package io.github.ayongbox.baseon.starter.swagger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * isProdActive 判定分支与 OpenAPI 定制 Bean 内容测试。
 */
class BaseonSwaggerAutoConfigurationTest {

    private final BaseonSwaggerAutoConfiguration configuration = new BaseonSwaggerAutoConfiguration();

    @Test
    void isProdActiveReturnsFalseWhenNoActiveProfile() {
        // Given：无任何激活 profile 的环境
        MockEnvironment environment = new MockEnvironment();

        // When：判定
        boolean result = BaseonSwaggerAutoConfiguration.isProdActive(environment, List.of("prod"));

        // Then：不命中生产
        assertThat(result).isFalse();
    }

    @Test
    void isProdActiveReturnsFalseWhenOnlyDevActive() {
        // Given：仅激活 dev
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        // When：判定
        boolean result = BaseonSwaggerAutoConfiguration.isProdActive(environment, List.of("prod"));

        // Then：不命中生产
        assertThat(result).isFalse();
    }

    @Test
    void isProdActiveReturnsTrueWhenProdActive() {
        // Given：激活 prod
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        // When：判定
        boolean result = BaseonSwaggerAutoConfiguration.isProdActive(environment, List.of("prod"));

        // Then：命中生产
        assertThat(result).isTrue();
    }

    @Test
    void isProdActiveReturnsTrueWhenProdAndDevBothActive() {
        // Given：prod 与 dev 并存（误配场景，宁可错杀）
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev", "prod");

        // When：判定
        boolean result = BaseonSwaggerAutoConfiguration.isProdActive(environment, List.of("prod"));

        // Then：仍视为生产生效
        assertThat(result).isTrue();
    }

    @Test
    void isProdActiveRespectsCustomProdProfiles() {
        // Given：自定义名单 ["production"]
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        // When：按自定义名单判定
        boolean result = BaseonSwaggerAutoConfiguration.isProdActive(environment, List.of("production"));

        // Then：命中生产
        assertThat(result).isTrue();
    }

    @Test
    void isProdActiveReturnsFalseWhenActiveProfileNotInCustomList() {
        // Given：激活 prod，但名单只认 "production"
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        // When：按自定义名单判定
        boolean result = BaseonSwaggerAutoConfiguration.isProdActive(environment, List.of("production"));

        // Then：prod 不在名单内，不命中
        assertThat(result).isFalse();
    }

    @Test
    void isProdActiveReturnsFalseWhenProdProfilesEmpty() {
        // Given：激活 prod，但名单为空（等于关闭硬关闭能力）
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        // When / Then：空名单与 null 名单都不命中
        assertThat(BaseonSwaggerAutoConfiguration.isProdActive(environment, List.of())).isFalse();
        assertThat(BaseonSwaggerAutoConfiguration.isProdActive(environment, null)).isFalse();
    }

    @Test
    void openApiBeanContainsInfoAndGlobalBearerSecurityScheme() {
        // Given：默认属性
        BaseonSwaggerProperties properties = new BaseonSwaggerProperties();
        properties.setTitle("订单服务 API");
        properties.setDescription("订单中心接口文档");
        properties.setVersion("2.0.0");

        // When：装配 OpenAPI 定制 Bean
        OpenAPI openAPI = configuration.baseonOpenAPI(properties);

        // Then：info 三要素来自属性
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("订单服务 API");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("订单中心接口文档");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("2.0.0");
        // Then：bearerAuth 为 HTTP Bearer + JWT，且全局生效
        SecurityScheme bearerAuth = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
        assertThat(bearerAuth.getBearerFormat()).isEqualTo("JWT");
        assertThat(openAPI.getSecurity())
                .singleElement()
                .satisfies(requirement -> assertThat(requirement).containsKey("bearerAuth"));
    }
}
