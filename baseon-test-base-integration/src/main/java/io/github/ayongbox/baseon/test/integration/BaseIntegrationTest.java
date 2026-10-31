package io.github.ayongbox.baseon.test.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 集成测试基类（Testcontainers 版）：自动拉起 MySQL 8 与 Redis 7，
 * 并通过 {@link DynamicPropertySource} 把连接信息注入 Spring 环境。
 *
 * <p>使用前提：本机有可用的 Docker 守护进程；无 Docker 时改用 {@link H2IntegrationTest}。
 * Redis 用 {@link GenericContainer} 启动（testcontainers 1.19.x 不再单独发布 redis 模块），
 * MySQL 沿用官方 {@link MySQLContainer}。
 *
 * <pre>{@code
 * class OrderRepositoryIT extends BaseIntegrationTest {
 *     // 直接 @Autowired Mapper / Repository 即可，datasource/redis 已就绪
 * }
 * }</pre>
 */
@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withReuse(true)
            .withUsername("root")
            .withPassword("root")
            .withDatabaseName("baseon_test");

    @Container
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7")
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void registerContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);

        registry.add("spring.redis.host", REDIS::getHost);
        registry.add("spring.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
