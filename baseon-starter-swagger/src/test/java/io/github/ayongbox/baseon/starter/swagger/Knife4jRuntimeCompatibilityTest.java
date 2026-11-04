package io.github.ayongbox.baseon.starter.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Knife4j 4.5.0 与 springdoc 2.5.0 的运行时兼容性验证：
 * 真实启动 Spring 上下文，确认 OpenAPI Bean 与 Knife4j 定制器能正常装配，
 * 无 NoSuchMethod / ClassNotFound 等版本不兼容异常。
 */
@SpringBootTest(classes = Knife4jRuntimeCompatibilityTest.TestApp.class,
        properties = {"spring.application.name=knife4j-compat-test"})
class Knife4jRuntimeCompatibilityTest {

    @SpringBootApplication
    static class TestApp {
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsWithKnife4jAndSpringdoc() {
        // baseon 的全局 OpenAPI 定制 Bean 必须存在
        OpenAPI openAPI = applicationContext.getBean(OpenAPI.class);
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isNotBlank();
        // Knife4j 的 OpenApiCustomizer 因 knife4j.enable=true 而装配
        assertThat(applicationContext.getBeanNamesForType(
                com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer.class)).isNotEmpty();
    }
}
