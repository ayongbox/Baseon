# Baseon 模块索引

> 以代码为准的速查表。配置前缀为 `@ConfigurationProperties` 的实际值；自动配置类均在
> `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中登记。
> 每个构件对应 Baseon 仓库根下的手册 `docs/modules/<artifactId>.md`（仓库定位见 SKILL.md）。

## 内核与版本

| 构件 | 职责 | 配置前缀 | 自动配置类 | 手册 |
|---|---|---|---|---|
| `baseon-parent` | `${revision}` 统一版本、enforcer、Checkstyle、flatten | — | — | — |
| `baseon-bom` | 对外版本单一入口，业务工程 `import` 后禁写版本号 | — | — | — |
| `baseon-core` | `model` / `tool` / `context` / `annotation` / `cloud` / `spi` / `secure` / `db` | — | — | `baseon-core.md` |
| `baseon-core-boot` | `core.boot` 外置构件：容器持有者、编程式事务、线程池工厂 | `baseon.boot` | `BaseonCoreBootAutoConfiguration` | `baseon-core-boot.md` |
| `baseon-core-launch` | `core.launch` 外置构件：启动前置校验、优雅停机钩子、启动横幅 | `baseon.launch` | `BaseonLaunchAutoConfiguration` | `baseon-core-launch.md` |

`baseon-core` 包职责：

| 包 | 内容 |
|---|---|
| `model` | `R<T>`、`ErrorCode`、`CommonErrorCode`、`ErrorType`、`BizException`、`LoginUser` |
| `db` | `PageQuery`、`PageResult<T>`、`BaseEntity`、`TenantEntity`（零 ORM 纯契约，13 期自 `model` 迁入） |
| `tool` | `StrUtil`、`MoneyUtil`、`BeanCopyUtil` |
| `context` | `SecurityUtil`、`ContextWrapper`、`GrayContext`、`TenantIgnoreContext`、`DataScopeIgnoreContext` |
| `annotation` | `@IgnoreTenant`、`@IgnoreTenantCache`、`@RequiresPermission`、`@Idempotent`、`@Degrade`、`@OperateLog`、`@DataScope`、`@IgnoreDataScope`、`@MqListener` |
| `cloud` | `CloudHeaders`（灰度 / 链路 Header 常量） |
| `spi` | `CacheKeyPrefixProvider`、`DataScopeProvider`；MQ 抽象：`MqProducer`、`MqMessage`、`MqConsumerListener`、`ConsumeResult`、`SendCallback`；OSS 抽象：`ObjectStorageService`、`ObjectInfo` |
| `secure` | `AuthProvider`、`PermissionChecker`、`CryptoService`、`SensitiveType` |

`core.boot` 需要 Spring，物理上独立为 `baseon-core-boot`：

| 类 | 内容 |
|---|---|
| `SpringContextHolder` | 静态取 Bean、`getBeanOrNull`、事件发布、Profile 与配置读取 |
| `TransactionUtil` | REQUIRED / REQUIRES_NEW / NESTED 编程式事务、`afterCommit` / `afterRollback` |
| `ThreadPoolFactory` / `ThreadPoolStats` | 强制命名前缀、拒绝策略、登录上下文透传、运行快照 |

`core.launch` 需要 Spring Boot 生命周期，物理上独立为 `baseon-core-launch`：

| 类 | 内容 |
|---|---|
| `LaunchCheck` / `LaunchCheckExecutor` | 启动前置校验 SPI 与编排（`ApplicationReadyEvent` 执行、fail-fast 摘要） |
| `RequiredConfigurationCheck` | 内置必选环境变量/配置项检查，缺失即启动失败 |
| `ShutdownHook` / `GracefulShutdownListener` | 优雅停机 SPI，`ContextClosedEvent` 最高优先级执行、异常隔离 |
| `BaseonBannerPrinter` | `ApplicationRunner` 横幅（应用名 / Boot / JDK 版本），不走 banner.txt |

## Starter

| 构件 | 职责 | 配置前缀 | 自动配置类 | 手册 |
|---|---|---|---|---|
| `baseon-starter-web` | 全局异常、Jackson、用户上下文、请求日志脱敏、CORS | `baseon.web` | `BaseonWebAutoConfiguration` | `baseon-starter-web.md` |
| `baseon-starter-trace` | TraceId、MDC、线程池/异步透传、统一 logback | `baseon.trace` | `BaseonTraceAutoConfiguration` | `baseon-starter-trace.md` |
| `baseon-starter-tracing-otel` | OTel 接入、OTLP 上报、本地日志导出档 | `baseon.tracing.otel` | `BaseonTracingOtelAutoConfiguration` | `baseon-starter-tracing-otel.md` |
| `baseon-starter-cloud-nacos` | 注册配置三档策略、ConfigHolder、Jasypt | `baseon.cloud.nacos` | `BaseonCloudNacosAutoConfiguration` | `baseon-starter-cloud-nacos.md` |
| `baseon-starter-cloud-feign` | 上下文/灰度透传、超时重试 | `baseon.cloud.feign` | `BaseonCloudFeignAutoConfiguration` | `baseon-starter-cloud-feign.md` |
| `baseon-starter-loadbalance` | 灰度标签路由、本地实例过滤 | `baseon.cloud.loadbalance` | `BaseonLoadBalanceAutoConfiguration` | `baseon-starter-loadbalance.md` |
| `baseon-starter-datasource` | 动态数据源、`@DS`、只读事务轮询从库、`RoutingDataSourceContributor` SPI | `baseon.datasource` | `BaseonDatasourceAutoConfiguration` | `baseon-starter-datasource.md` |
| `baseon-starter-sharding` | ShardingSphere-JDBC 挂入动态路由表、`@DS("sharding")`、规则 YAML 业务自定 | `baseon.sharding` | `BaseonShardingAutoConfiguration` | `baseon-starter-sharding.md` |
| `baseon-starter-seata` | 分布式事务可选装配、AT/TCC 模式推导、编程式全局事务（默认不引入） | `baseon.seata` | `BaseonSeataAutoConfiguration` + `BaseonSeataEnvironmentPostProcessor` | `baseon-starter-seata.md` |
| `baseon-starter-mybatis-plus` | 插件链、公共字段填充、逻辑删除、慢 SQL | `baseon.mybatis-plus` | `BaseonMybatisPlusAutoConfiguration` | `baseon-starter-mybatis-plus.md` |
| `baseon-starter-redis` | key 规范校验、`RedisLock`、统一序列化 | `baseon.redis` | `BaseonRedisAutoConfiguration` | `baseon-starter-redis.md` |
| `baseon-starter-cache` | L1 Caffeine + L2 Redis、三害防护、租户前缀 | `baseon.cache` | `BaseonCacheAutoConfiguration` | `baseon-starter-cache.md` |
| `baseon-starter-tenant` | 租户 SQL 过滤、缓存租户前缀、`TenantOps` | `baseon.tenant` | `BaseonTenantAutoConfiguration` | `baseon-starter-tenant.md` |
| `baseon-starter-data-scope` | `@DataScope` 行级 SQL 过滤、字段级序列化裁剪 | `baseon.data-scope` | `BaseonDataScopeAutoConfiguration` | `baseon-starter-data-scope.md` |
| `baseon-starter-auth` | JWT 双 Token、`@RequiresPermission`、黑名单 | `baseon.auth` | `BaseonAuthAutoConfiguration` | `baseon-starter-auth.md` |
| `baseon-starter-xss` | 表单/头/JSON 转义、exclude-paths | `baseon.xss` | `BaseonXssAutoConfiguration` | `baseon-starter-xss.md` |
| `baseon-starter-sentinel` | BlockException 映射、租户热点限流 | `baseon.sentinel` | `BaseonSentinelAutoConfiguration` | `baseon-starter-sentinel.md` |
| `baseon-starter-resilience` | `@Degrade` 三策略、熔断告警 SPI | `baseon.resilience` | `BaseonResilienceAutoConfiguration` | `baseon-starter-resilience.md` |
| `baseon-starter-idempotent` | `@Idempotent` Redis 原子占位、租户前缀 | `baseon.idempotent` | `BaseonIdempotentAutoConfiguration` | `baseon-starter-idempotent.md` |
| `baseon-starter-excel` | `ExcelTemplate` 导出、逐行容错导入 | `baseon.excel` | `BaseonExcelAutoConfiguration` | `baseon-starter-excel.md` |
| `baseon-starter-swagger` | OpenAPI3（Knife4j UI，`/doc.html`）、Bearer scheme、生产三层硬关闭 | `baseon.swagger` | `BaseonSwaggerAutoConfiguration` + `SwaggerEnvironmentPostProcessor` | `baseon-starter-swagger.md` |
| `baseon-starter-log` | `@OperateLog` 审计、`OperateLogHandler` SPI | `baseon.log.operate` | `BaseonLogAutoConfiguration` | `baseon-starter-log.md` |
| `baseon-starter-debug-tools` | `/debug/context`、`/debug/profiles`，生产失效 | `baseon.debug` | `BaseonDebugAutoConfiguration` | `baseon-starter-debug-tools.md` |
| `baseon-starter-mq-rocketmq` | `MqProducer` RocketMQ 实现、`@MqListener` 消费注册、消费幂等、登录上下文透传、ACL | `baseon.mq.rocketmq` | `BaseonMqAutoConfiguration` | `baseon-starter-mq-rocketmq.md` |
| `baseon-starter-oss-minio` | `ObjectStorageService` MinIO/S3 实现、预签名 URL、自动建桶（联调） | `baseon.oss.minio` | `BaseonOssAutoConfiguration` | `baseon-starter-oss-minio.md` |

## 工程化与示例

| 构件 | 职责 | 使用方式 | 手册 |
|---|---|---|---|
| `baseon-generator-maven-plugin` | CRUD 九件套生成 | `mvn baseon:generate -Dtable=t_xxx` | `baseon-generator-maven-plugin.md` |
| `baseon-archetype-clean` | 老项目迁移空原型 | `mvn archetype:generate -DarchetypeArtifactId=baseon-archetype-clean` | `baseon-archetype.md` |
| `baseon-archetype-sample` | 新项目全景原型 | 同上，替换 artifactId | `baseon-archetype.md` |
| `samples/demo-service` | 单服务全能力示例（端口见 `application.yml`） | 仅验证，不发布 | — |
| `samples/demo-service-b` | 跨服务调用对端 | 仅验证，不发布 | — |
| `samples/demo-gateway` | Gateway + Nacos + Swagger 聚合 | 仅验证，不发布 | — |

## 依赖引入速记

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

之后任何 `baseon-*` 依赖都不写版本号。最小可用组合：`baseon-starter-web` + `baseon-starter-trace`；
微服务再加 `cloud-nacos`、`cloud-feign`、`loadbalance`；需要数据库时按「datasource → mybatis-plus」
顺序引入。

## 交付资产（非 Maven）

以下路径相对 Baseon 仓库根：

| 路径 | 内容 | 使用方式 |
|---|---|---|
| `docs/assets/ci/` | Trivy 安全扫描、k6 性能回归 | 复制到业务工程 `ci/` 后 `include` |
| `docs/assets/container/` | 多阶段 Dockerfile、docker-compose、K8s 清单 | 复制到根 / `deploy/k8s/`，替换占位值 |
| `docs/assets/alerting/` | Prometheus 抓取、告警规则、AlertManager | 挂载到监控组件或转 `PrometheusRule` |
| `docs/assets/adr/` | ADR、Roadmap、反模式 | 阅读并按模板新增决策记录 |
