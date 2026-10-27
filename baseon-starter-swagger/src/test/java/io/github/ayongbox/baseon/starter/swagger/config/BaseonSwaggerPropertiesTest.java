package io.github.ayongbox.baseon.starter.swagger.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseonSwaggerProperties 默认值与绑定测试。
 */
class BaseonSwaggerPropertiesTest {

    @Test
    void defaultValuesMatchConvention() {
        // Given：直接 new，不绑定任何配置

        // When：读取默认值
        BaseonSwaggerProperties properties = new BaseonSwaggerProperties();

        // Then：默认值符合骨架约定
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getTitle()).isEqualTo("Baseon API");
        assertThat(properties.getDescription()).isNotBlank();
        assertThat(properties.getVersion()).isEqualTo("1.0.0");
        assertThat(properties.getProdProfiles()).containsExactly("prod");
    }

    @Test
    void bindsCustomValuesFromEnvironment() {
        // Given：环境里配置了自定义标题与生产 profile 名单
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.swagger.title", "订单服务 API")
                .withProperty("baseon.swagger.description", "订单中心接口文档")
                .withProperty("baseon.swagger.version", "2.3.0")
                .withProperty("baseon.swagger.prod-profiles[0]", "prod")
                .withProperty("baseon.swagger.prod-profiles[1]", "production");

        // When：通过 Binder 绑定
        BaseonSwaggerProperties properties = Binder.get(environment)
                .bind("baseon.swagger", BaseonSwaggerProperties.class)
                .get();

        // Then：全部按配置生效
        assertThat(properties.getTitle()).isEqualTo("订单服务 API");
        assertThat(properties.getDescription()).isEqualTo("订单中心接口文档");
        assertThat(properties.getVersion()).isEqualTo("2.3.0");
        assertThat(properties.getProdProfiles()).containsExactly("prod", "production");
    }

    @Test
    void bindsDefaultsWhenNoPropertyPresent() {
        // Given：空环境，没有任何 baseon.swagger.* 配置

        // When：Binder 绑定不到时回退默认实例
        BaseonSwaggerProperties properties = Binder.get(new MockEnvironment())
                .bind("baseon.swagger", BaseonSwaggerProperties.class)
                .orElse(new BaseonSwaggerProperties());

        // Then：仍是约定的默认值
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getTitle()).isEqualTo("Baseon API");
        assertThat(properties.getVersion()).isEqualTo("1.0.0");
        assertThat(properties.getProdProfiles()).containsExactly("prod");
    }
}
