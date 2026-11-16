---
name: baseon-consumer
description: 基于 Baseon 脚手架开发业务工程，涵盖 BOM 引入、starter 选型、配置前缀、core 契约、运行期顺序与常见陷阱。用户询问 Baseon 依赖、模块用法、自动装配顺序，或在任何消费 Baseon 的工程中开发时使用；不适用于修改 Baseon 仓库本身。
---

# 消费 Baseon 规约

本 skill 描述**在业务工程中使用 Baseon**，适用于所有消费 Baseon 的项目。修改 Baseon 仓库的 core、starter、示例模块时，打开 Baseon 仓库并使用其项目内 `.trae/skills/baseon-architecture`；该 skill 仅在 Baseon 工作区可见。

## 仓库定位

文中所有 `docs/` 路径均相对 **Baseon 仓库根**，按以下顺序解析：

1. 当前工作区根存在 `baseon-bom/pom.xml` 与根 `pom.xml` → 使用当前工作区根；
2. 当前工作区同级的 `Baseon/` 目录；
3. 回退 `/Users/ayong/devcode/private_project/java/Baseon/`。

三者都不存在时，只用本 skill 的速查内容，并明确告知用户无法读取 Baseon 仓库文档。

## 版本事实

| 项 | 版本 |
|---|---|
| JDK | 17 |
| Spring Boot | 3.2.12 |
| Spring Cloud | 2023.0.5 |
| MyBatis-Plus | 3.5.9 |
| Baseon 坐标 | `io.github.ayongbox` |

版本以 `baseon-bom` 为准，业务工程不手写版本号。此表是快照，与 Baseon 仓库 `pom.xml` 不一致时以代码为准。

## 依赖引入

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.ayongbox</groupId>
            <artifactId>baseon-bom</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

之后所有 `baseon-*` 依赖不写 `<version>`。业务工程使用自己的 groupId，与 Baseon 分开管理。

依赖方向只有一条：**业务工程 → Baseon**。不得修改 Baseon 的 groupId、artifactId、包名，也不得把 Baseon 模块复制进业务工程再改。

## 业务工程分层

建议按职责拆分，避免单模块膨胀：

```text
xxx-platform（parent）
├── xxx-core         领域契约、编译/解析等纯逻辑
├── xxx-runtime      数据访问、服务实现、流程引擎
├── xxx-designer     设计器与配置界面
├── xxx-web          控制台后端与前端聚合
├── xxx-license      授权与版本能力
└── xxx-ext-*        商业扩展包，闭源，通过 SPI 装配
```

通用约束：

- 业务代码只依赖 `baseon-starter-*`，不单独声明 `baseon-core`，core 由 starter 传递；
- 开源构件与商业实现分仓或分模块，商业实现不得打进社区包；
- 构件名不出现 `plus`、`free`、`lite`，版本由 License 决定；
- 配置前缀使用项目自己的前缀，不要在业务代码里覆盖 `baseon.*`；
- 具体模块划分与包名以各项目仓库的 `features.md` / `docs/` 为准。

## Starter 选型

| 场景 | 引入 |
|---|---|
| 最小可用 | `baseon-starter-web`、`baseon-starter-trace` |
| 数据库 | `baseon-starter-datasource` → `baseon-starter-mybatis-plus` |
| 缓存与锁 | `baseon-starter-redis`、`baseon-starter-cache` |
| 多租户/数据权限 | `baseon-starter-tenant`、`baseon-starter-data-scope` |
| 认证鉴权 | `baseon-starter-auth` |
| 微服务 | `baseon-starter-cloud-nacos`、`baseon-starter-cloud-feign`、`baseon-starter-loadbalance` |
| 稳定性 | `baseon-starter-sentinel`、`baseon-starter-resilience`、`baseon-starter-idempotent` |
| 安全与审计 | `baseon-starter-xss`、`baseon-starter-log` |
| 集成 | `baseon-starter-mq-rocketmq`、`baseon-starter-oss-minio` |
| 工具 | `baseon-starter-excel`、`baseon-starter-swagger`、`baseon-starter-tracing-otel` |
| 按需 | `baseon-starter-sharding`、`baseon-starter-seata` |

完整清单、配置前缀与自动配置类见 [references/modules.md](references/modules.md)。

## 租户隔离模型

`baseon-starter-tenant` 采用 **`tenant_id` 列隔离**：自动为 SQL 追加 `tenant_id = ?` 条件。

- 所有业务表必须包含 `tenant_id`，联合索引以 `tenant_id` 前导；
- 字典表、跨租户统计表等非租户表必须加入 `ignore-tables`，否则被误拼条件后查不到数据；
- 单租户运行也要写入固定租户值，保留后续开启多租户的能力；
- 需要跨租户查询时用 `@IgnoreTenant`，不要在 SQL 里手写绕过。

不要在同一工程内混用列隔离与 schema 隔离两套模型。

## core 关键契约

| 契约 | 用法 |
|---|---|
| `R<T>` | Controller 统一返回；判断成功用 `r.success()`，不要手写 `getCode() == 0` |
| `PageQuery` / `PageResult<T>` | 分页入参继承 `PageQuery`，`size` 上限 500；出参用 `PageResult` |
| `CommonErrorCode` / `BizException` | 业务失败抛 `new BizException(CommonErrorCode.NOT_FOUND)`；自定义错误码实现 `ErrorCode` |
| `SecurityUtil` | 取当前 `LoginUser`（userId/tenantId/permissions） |
| `ContextWrapper` | 手写线程池或 `CompletableFuture` 时必须包装，否则丢登录与租户上下文 |
| `core.annotation` | `@IgnoreTenant`、`@IgnoreTenantCache`、`@RequiresPermission`、`@Idempotent`、`@Degrade`、`@OperateLog`、`@DataScope`、`@IgnoreDataScope`、`@MqListener` |
| `core.spi` | `CacheKeyPrefixProvider`、`DataScopeProvider`、`MqProducer`/`MqConsumerListener`、`ObjectStorageService` |
| `core.secure` | `AuthProvider`、`PermissionChecker`、`CryptoService`、`SensitiveType` |

## 运行期顺序

- MyBatis-Plus 插件链固定为 **租户 → 数据权限 → 分页**；顺序错了分页 count SQL 会丢过滤条件。
- 数据权限只改写 SELECT；INSERT/UPDATE/DELETE 不处理，写入归属由业务约束。
- `visibleIds()` 语义：**空集 = 一条都查不到，null = 不限制**；无权限必须返回空集。
- `AuthTokenFilter` 先于 `UserContextInterceptor`，登录态不会被伪造请求头覆盖。
- ShardingSphere 在 JDBC 层、MP 插件在 MyBatis 层；分片表必须标 `@DS("sharding")`，非分片表不可标。
- Seata AT 会把整个动态路由数据源代理成一个分支资源；多物理库共享 resourceId，需要单独注册 `DataSourceProxy` 或排除自动代理。

## 高频陷阱

| 编号 | 结论 |
|---|---|
| P-001 | 插件版本必须写进 `pluginManagement`，BOM 不传递插件版本 |
| P-019 | springdoc webflux 线关 api-docs 会连带 swagger-ui 报废 |
| P-022 | 跨服务追踪断裂先查 `feign-micrometer` 是否在类路径 |
| P-023 | 继承 `BaseMultiTableInnerInterceptor` 必须自行实现四个 `processXxx` |
| P-026 | Jackson 请求态裁剪必须放 serialize 阶段，`changeProperties` 结果会被缓存 |
| P-027 | `spring.sql.init` 与 `@Sql` 双开会重复插入种子数据 |
| P-033 | `spring-boot` 构件不传递 slf4j-api，外置构件需 optional 显式声明 |
| P-036 | HikariCP `setDriverClassName` 会立即加载驱动，单测需真实驱动或桩 Driver |
| P-037 | Seata 2.1+ 坐标为 `org.apache.seata`，Boot 3 不能用旧 `io.seata` |
| P-038 | Knife4j 4.5.0 必须配 springdoc 2.3.0，2.4+ 会 `NoSuchMethodError` |

完整清单查 Baseon 仓库 `docs/pitfalls.md`（编号递增，当前已到 P-038 以上）。

## 文档入口

以上路径均相对 Baseon 仓库根（定位规则见文首）：

- 模块手册：`docs/modules/<artifactId>.md`
- 陷阱清单：`docs/pitfalls.md`
- 变更台账：`docs/CHANGELOG.md`
- 交付资产：`docs/assets/`（CI、容器、告警、ADR 模板，复制使用，不参与构建）

## 反模式

- 在业务工程里复制 Baseon 源码再改，而不是通过 BOM 依赖；
- 为 Baseon 已提供的能力另起实现，先查 starter 地图；
- 绕过 BOM 手写 `baseon-*` 版本号；
- 用字段注入、把 Entity 暴露到接口、Controller 直接依赖 Mapper；
- 在社区构件里放商业实现，或用临时前缀替代项目正式配置前缀；
- 同一工程混用两套租户隔离模型。
