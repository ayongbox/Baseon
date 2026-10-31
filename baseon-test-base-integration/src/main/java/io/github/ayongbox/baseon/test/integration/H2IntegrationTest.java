package io.github.ayongbox.baseon.test.integration;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * 集成测试基类（H2 版）：零外部依赖，使用 H2 内存库（MySQL 兼容模式）。
 *
 * <p>适用场景：CI 无 Docker、或只需验证 ORM/SQL 改写逻辑（如数据权限、租户插件）
 * 而不依赖真实 MySQL 特性。需要真实 MySQL/Redis 行为时用 {@link BaseIntegrationTest}。
 *
 * <p>使用方式：子类在 {@code src/test/resources} 下放 {@code schema.sql} / {@code data.sql}，
 * 由 {@code spring.sql.init} 加载；或在测试类上用 {@code @Sql}。
 *
 * <pre>{@code
 * @SpringBootTest(properties = {
 *     "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1",
 *     "spring.datasource.driver-class-name=org.h2.Driver"
 * })
 * class DataScopeIT extends H2IntegrationTest { ... }
 * }</pre>
 */
@SpringBootTest
public abstract class H2IntegrationTest {

    /**
     * 由子类通过 {@code @SpringBootTest#properties()} 或 application-test.yml 覆盖，
     * 基类不强制指定，避免约束子类的库名与参数。
     */
    protected H2IntegrationTest() {
    }
}
