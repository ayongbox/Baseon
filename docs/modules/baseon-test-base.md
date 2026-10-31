# baseon-test-base · 测试基座

## 定位

统一各模块与业务工程的测试写法，消除重复的上下文清理、容器启动、配置注入样板代码。拆为两个子模块：

| 构件 | 依赖 | 适用场景 |
|---|---|---|
| `baseon-test-base-core` | baseon-core + JUnit5/Mockito/AssertJ | 单元测试；零 Spring 依赖，core 层也能用 |
| `baseon-test-base-integration` | test-base-core + Spring Boot Test + Testcontainers + H2 | 集成测试；提供 MySQL/Redis 容器或 H2 两套基类 |

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-test-base-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-test-base-integration</artifactId>
    <scope>test</scope>
</dependency>
```

> 两个模块的框架依赖（JUnit/Mockito/AssertJ/Spring Boot Test/Testcontainers/H2）均为 compile 范围，
> 消费方以 `scope=test` 依赖本模块即可传递获得，无需重复声明。

## baseon-test-base-core

### `BaseUnitTest`（基类方式）

继承后自动在每个测试方法结束后清理 `SecurityUtil`、租户忽略、数据权限忽略、灰度标签等线程上下文，避免线程复用串号：

```java
class OrderServiceTest extends BaseUnitTest {
    @Test
    void something() {
        SecurityUtil.set(MockFactory.loginUser());
        // ...断言
        // @AfterEach 由基类清理，无需手写
    }
}
```

### `ContextCleanupExtension`（组合方式）

无法继承基类时用扩展：

```java
@ExtendWith(ContextCleanupExtension.class)
class AlreadyHasParentTest { ... }
```

### `TestConstants`

常用默认值：`DEFAULT_TENANT_ID=1001L`、`DEFAULT_USER_ID=1L`、`DEFAULT_USERNAME`、`DEFAULT_TRACE_ID`、`DEFAULT_GRAY_TAG`。

### `MockFactory`

```java
LoginUser user = MockFactory.loginUser();                       // 默认租户/用户
LoginUser user = MockFactory.loginUser(2002L, 9L);              // 指定值
LoginUser user = MockFactory.loginUserInContext(2002L, 9L);     // 构造并写入 SecurityUtil
```

## baseon-test-base-integration

### `BaseIntegrationTest`（Testcontainers，需 Docker）

自动拉起 MySQL 8 + Redis 7，`@DynamicPropertySource` 注入 `spring.datasource.*` 与 `spring.redis.*`：

```java
class OrderRepositoryIT extends BaseIntegrationTest {
    @Autowired
    private OrderMapper orderMapper;
    // 直接使用，连接信息已就绪
}
```

容器开启 `withReuse(true)`，同 JVM 内多测试类共享，加快速度；全局开关在
`testcontainers.properties`（`testcontainers.reuse.enable=true`）。

### `H2IntegrationTest`（零依赖兜底）

无 Docker 或只需验证 ORM/SQL 改写时用 H2 内存库（MySQL 兼容模式）：

```java
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
class DataScopeIT extends H2IntegrationTest { ... }
```

建表脚本放 `src/test/resources/schema.sql`，由 `spring.sql.init` 加载；不要与 `@Sql` 双开（P-027）。

## 基类选择

| 场景 | 用哪个 |
|---|---|
| 纯单元测试、无 Spring 容器 | `BaseUnitTest` 或 `ContextCleanupExtension` |
| 真实 MySQL/Redis 行为、有 Docker | `BaseIntegrationTest` |
| 验证 SQL 改写/ORM、CI 无 Docker | `H2IntegrationTest` |

## 注意事项

1. **scope 必须是 test**：test-base 模块带 JUnit/Testcontainers，误设 compile 会把测试框架打进生产包。
2. **容器复用需要 Docker**：`BaseIntegrationTest` 在无 Docker 环境直接失败，CI 若无 Docker 改用 `H2IntegrationTest`。
3. **不强制迁移存量测试**：存量手写 `@AfterEach` 的测试继续可用，新测试优先用基座。
