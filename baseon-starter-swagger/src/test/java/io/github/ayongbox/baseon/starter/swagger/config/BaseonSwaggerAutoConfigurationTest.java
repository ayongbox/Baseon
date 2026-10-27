package io.github.ayongbox.baseon.starter.swagger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
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

    @Test
    void serverDescriptionCustomizerUsesApplicationNameByDefault() {
        // Given：未配置 server-description，环境里有 spring.application.name
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "order-service");
        OpenAPI openAPI = new OpenAPI().servers(List.of(
                new Server().url("http://192.168.0.4:8081").description("Generated server url")));

        // When：对 springdoc 自动补全后的 servers 应用定制器
        configuration.baseonServerDescriptionCustomizer(new BaseonSwaggerProperties(), environment)
                .customise(openAPI);

        // Then：说明文字替换为服务名，url 保持不变（Try it out 仍指向原地址）
        assertThat(openAPI.getServers()).singleElement().satisfies(server -> {
            assertThat(server.getDescription()).isEqualTo("order-service");
            assertThat(server.getUrl()).isEqualTo("http://192.168.0.4:8081");
        });
    }

    @Test
    void serverDescriptionCustomizerPrefersExplicitlyConfiguredValue() {
        // Given：显式配置了说明文字
        BaseonSwaggerProperties properties = new BaseonSwaggerProperties();
        properties.setServerDescription("订单中心");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "order-service");
        OpenAPI openAPI = new OpenAPI().servers(List.of(new Server().url("http://127.0.0.1:8081")));

        // When：应用定制器
        configuration.baseonServerDescriptionCustomizer(properties, environment).customise(openAPI);

        // Then：配置值优先于服务名
        assertThat(openAPI.getServers()).singleElement()
                .satisfies(server -> assertThat(server.getDescription()).isEqualTo("订单中心"));
    }

    @Test
    void serverDescriptionCustomizerSkipsWhenNoServersOrName() {
        // Given：既无 servers 也无服务名
        OpenAPI openAPI = new OpenAPI();

        // When / Then：定制器静默跳过，不抛异常也不塞入空说明
        configuration.baseonServerDescriptionCustomizer(new BaseonSwaggerProperties(), new MockEnvironment())
                .customise(openAPI);
        assertThat(openAPI.getServers()).isNull();
    }
}
