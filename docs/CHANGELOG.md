# 迭代能力台账

> 记录每个迭代周期「交付了什么能力、改变了什么、需要业务方注意什么」。
> 版本号统一使用 `${revision}`，发布时由 Maven 注入；开发期按迭代期记录。

---

## [1.0.0-SNAPSHOT] · 15 期：分库分表（S-10 starter-sharding）

**日期**：2026-11-01

### 新增能力

- `baseon-starter-datasource` 新增扩展点 `RoutingDataSourceContributor`（support 包）：主库/从库装配完成后按序回调，向动态路由表追加数据源；无 contributor 时行为完全不变（向后兼容）。
- `baseon-starter-sharding`（S-10 落地）：ShardingSphere-JDBC 5.5.2 基础装配，分片规则业务自定。
  - 装配链：`baseon.sharding.datasources` 声明物理分片库（复用 starter-datasource 的连接/Hikari 池模型）→ 读取 `rules-location`（classpath:/file:）规则 YAML（只含 rules+props，物理数据源不进 YAML）→ `YamlShardingSphereDataSourceFactory` 聚合为逻辑数据源 → 经 `RoutingDataSourceContributor` 挂入动态路由表。
  - 业务用法：分片表的 Mapper/Service 标注 `@DS("sharding")`（route-key 可配），与读写分离、@DS 切换同一条路由决策链。
  - 生命周期收口：`ShardingRoutingDataSourceContributor` 实现 `DisposableBean`，容器关闭时先关 ShardingSphere 聚合数据源再关各物理 Hikari 池。
  - 快速失败：datasources 为空、规则文件不存在、route-key 与已有路由 key 冲突均启动即报。
  - 与 MyBatis-Plus 天然兼容：MP 插件链（租户→数据权限→分页）在 MyBatis 层执行，ShardingSphere 在 JDBC 层执行，租户列可直接作分片键。
- 工程接线：根 POM `<modules>` / `<shardingsphere.version>` / `<dependencyManagement>` 与 `baseon-bom` 同步新增；guava 33.3.0-jre / checker-qual 3.43.0 / error_prone_annotations 2.28.0 锁定收敛（shardingsphere 与传递 calcite 的版本分歧）。
- 9 个新测试（datasource SPI 2 + sharding 装配 4 + ArchUnit 2 + H2 双库分片端到端 1），集成测试验证 user_id 取模路由落位与跨库 COUNT 归并，全量回归 450 个测试全绿。

### 影响面（业务升级须知）

- `BaseonDatasourceAutoConfiguration#dataSource` 方法签名新增 `ObjectProvider<RoutingDataSourceContributor>` 参数（Spring 按类型注入，业务无感知）；未引入任何 contributor 时路由表与行为不变。

### 注意事项

1. **分片规则完全业务自定**：本 starter 只装配引擎不提供默认规则；表/库策略、广播表、绑定表写法见模块手册与 ShardingSphere 5.5.x 文档。
2. **物理数据源名必须与 YAML 对齐**：`baseon.sharding.datasources` 的 key 即规则中 `actualDataNodes` 引用的 ds 名，错配启动失败。
3. **分片表操作必须走 `@DS("sharding")`**：未标注的分片表 SQL 会落到 master 库报「表不存在」；非分片表禁止标 `@DS("sharding")`（规则引擎内无该表元数据）。
4. **分布式主键自行选择**：规则 YAML 中配 `keyGenerateStrategy`（如 SNOWFLAKE），MP 的 `@TableId(type = ASSIGN_ID)` 也可生成，二者取其一，不要叠加。
5. **跨分片事务不在本 starter 范围**：ShardingSphere-JDBC 的 XA/Seata 集成按需后续迭代；当前跨库写需业务保证最终一致性。
6. **Hikari `setDriverClassName` 会立即加载并实例化驱动类**（P-036）：单测构建连接池需真实驱动或桩 Driver，不能传 null 绕过。

---

## [1.0.0-SNAPSHOT] · 14 期：集成底座（MQ/OSS 抽象 + RocketMQ/MinIO 实现）

**日期**：2026-11-01

### 新增能力

- `core.spi` MQ 抽象契约：`MqMessage`（topic/tag/key/body/headers，登录上下文头常量收口）、`MqProducer`（send/sendAsync/sendOneway/sendDelay）、`MqConsumerListener` + `ConsumeResult`（SUCCESS/RETRY）、`SendCallback`；`core.annotation` 新增 `@MqListener`（topic/consumerGroup/tags）。
- `core.spi` OSS 抽象契约：`ObjectStorageService`（upload/download/delete/exists/presignedUrl，bucket 传 null 走默认桶）、`ObjectInfo`。
- `baseon-starter-mq-rocketmq`（S-24 RocketMQ 实现落地，抽象先行）：
  - 生产者：`DefaultMQProducer` 装配 + `RocketMqProducerAdapter` 实现 `MqProducer`，失败统一 `BizException(REMOTE_CALL_FAILED)`；`sendDelay` 就近向上映射 4.9.x 固定 18 级延迟。
  - 消费者：`MqConsumerRegistrar` 扫描 `MqConsumerListener` Bean + `@MqListener` 注解（缺注解启动失败），单条消费，RETRY/异常 → `RECONSUME_LATER`。
  - 消费幂等：引入 starter-redis 时自动生效，`mq:idem:` + 租户前缀 + 业务 key（空用 msgId），窗口默认 24h 覆盖 16 次重投全周期。
  - 登录上下文透传：发送时注入 userId/tenantId/username 到消息头，消费前恢复 `SecurityUtil`，finally 清理。
  - ACL：accessKey/secretKey 成对配置；客户端为 RocketMQ 4.9.7 经典 remoting 版，兼容 4.9/5.x 服务端。
- `baseon-starter-oss-minio`（计划外增补）：MinIO SDK 8.5.17 实现 `ObjectStorageService`；S3 协议即聚合层，AWS S3/阿里云 OSS/腾讯 COS/华为 OBS 换 endpoint/凭据即用；download 不存在抛 `BizException(NOT_FOUND)`，流式上传按 -1 + 10MiB 分片。
- 装配降级链：未配 `name-server`/`endpoint` 整体不装配；未引 starter-redis 消费幂等自动跳过；所有默认 Bean 均可被业务覆盖。
- ArchUnit：`CoreArchitectureTest` 零 Spring 规则包列表扩为 8 个（新增 `core.annotation`/`core.cloud`/`core.spi`）；两个新 starter 各自闭环依赖规则。
- 38 个新测试（MQ 23 + OSS 15），全量回归 441 个测试全绿。

### 影响面（业务升级须知）

- core 仅新增纯契约类，无破坏性变更；`CoreArchitectureTest` 零 Spring 规则收紧到 8 个包，仓库内自建模块若在 `core.annotation`/`core.cloud`/`core.spi` 引入 Spring 会构建失败（现有代码无此情况）。

### 注意事项

1. **`rocketmq-acl` 是独立构件**：`rocketmq-client` 不传递，ACL 场景必须显式声明（父 POM 已管控版本，P-034）。
2. **PushConsumer 开 ACL 必须三参构造器**：4.9.7 无 `(String, RPCHook)` 两参版，且 `AllocateMessageQueueAveragely` 在 `consumer.rebalance` 包（P-035）。
3. **延迟消息是固定级别映射**：18 级（1s~2h）就近向上，不是任意毫秒精度；精确定时走调度任务。
4. **消费幂等依赖共享 Redis**；业务 key 才是跨重投稳定标识（msgId 兜底只覆盖 Broker 重投），有天然业务键务必设置。
5. **同步发送失败即抛异常不重试**：需要重试语义走 `sendAsync` 回调补偿，或上游本地消息表兜底。
6. **`auto-create-bucket` 仅限联调**：生产桶由运维预创建并授权；预签名 URL 有效期按业务最小化。
7. **厂商切换只改配置**：endpoint/region/凭据三要素；归档存储等厂商特性不在抽象契约内，业务可覆盖默认 Bean 扩展。

---

## [1.0.0-SNAPSHOT] · 13 期：内核收口（core.db 迁移 + baseon-core-launch）

**日期**：2026-11-01

### 新增能力

- `core.db` 落地：`PageQuery` / `PageResult` / `BaseEntity` / `TenantEntity` 从 `core.model` 迁入 `io.github.ayongbox.baseon.core.db`（`git mv` 保历史）。该包只放分页与持久化 PO 纯契约，ArchUnit 新增规则禁止其依赖 `tool` / `context` 与任何 ORM（MP、JPA），同时并入「零 Spring」规则。
- `baseon-core-launch`：`core.launch` 逻辑包的物理构件，承载启动治理能力。
  - `LaunchCheck` SPI + `LaunchCheckExecutor`：`ApplicationReadyEvent` 时统一执行全部检查，失败汇总为「名称： 原因」摘要；`baseon.launch.fail-fast`（默认 true）抛 `LaunchCheckException` 终止启动，`false` 降级 WARN（仅限本地联调）。
  - `RequiredConfigurationCheck`：内置必选配置检查，`baseon.launch.required-properties` / `required-environment-variables` 缺失即启动失败，把「本服务依赖的外部配置」从文档约定变成硬约束。
  - `ShutdownHook` SPI + `GracefulShutdownListener`：`ContextClosedEvent` 最高优先级统一执行停机钩子，单钩子异常隔离不影响其余。
  - `BaseonBannerPrinter`：ASCII 横幅 + 应用名 / Boot / JDK 版本，走 `ApplicationRunner` 而非 `banner.txt`，不与业务横幅冲突。
  - 整体开关 `baseon.launch.enabled`（默认 true），横幅独立开关 `baseon.launch.banner-enabled`；每个默认 Bean 均可被业务覆盖。
- 工程接线：根 POM `<modules>` / `<dependencyManagement>` 与 `baseon-bom` 同步新增 `baseon-core-launch` 坐标。
- 20 个新测试（执行器 5 + 必选配置 5 + 停机钩子 2 + 横幅 2 + 装配 4 + ArchUnit 2），全量回归 403 个测试全绿。

### 影响面（业务升级须知）

- `PageQuery` / `PageResult` / `BaseEntity` / `TenantEntity` 的**包名变更**：`core.model` → `core.db`。仓库内引用点（generator 常量、archetype 模板、mybatis-plus 测试）已全部同步；业务工程按 import 报错全局替换即可，类签名无任何变化。
- `MyMetaObjectHandler` 走字符串反射填充公共字段，不直接引用实体类型，不受迁移影响。

### 注意事项

1. **`core.db` 只放纯契约**：新增持久化相关类型时禁止引入 ORM 注解与工具/上下文依赖，ArchUnit 会直接构建失败。
2. **`LaunchCheck` 必须只读**：执行时机在启动阶段，写操作可能依赖未就绪的 Bean；初始化逻辑用 `ApplicationRunner`。
3. **`fail-fast=false` 仅限本地联调**：生产关闭会让配置缺失的服务带病上线。
4. **横幅不走 `banner.txt`**：避免覆盖业务自定义横幅；关闭用 `baseon.launch.banner-enabled=false`。
5. **停机钩子清理线程池**：`ThreadPoolFactory` 创建的池非守护，业务必须在 `ShutdownHook` 中 `shutdown()`。
6. **spring-boot 依赖为 optional**：不向下游强制传递；非 Boot 工程引入时自动装配不生效。

---

## [1.0.0-SNAPSHOT] · 12 期：文档资产（docs/assets）

**日期**：2026-11-01

### 新增能力

- `docs/assets/ci`：两个 GitLab CI 模板 + k6 脚本。`security-scan.gitlab-ci.yml` 用 Trivy 扫描 Maven 依赖漏洞、容器镜像漏洞与 K8s/Dockerfile 配置风险，输出 JSON/CycloneDX SBOM；`performance-regression.gitlab-ci.yml` 用恒定 VU 压测存活探针，P95、失败率、check 通过率任一不达标即失败。
- `docs/assets/container`：JDK 17 多阶段 `Dockerfile`（非 root、容器感知堆参数、健康检查、`exec` 接管信号）；`docker-compose.yml` 编排 MySQL 8 / Redis 7 / Nacos 2.3 / demo-service，`monitoring` profile 追加 Prometheus 与 AlertManager；K8s 清单包含 ConfigMap、Secret 示例、Deployment（三类探针 + 滚动发布 + 优雅停机）、Service、HPA。
- `docs/assets/alerting`：Prometheus 抓取配置、8 条告警规则（抓取失败、5xx 比例、P95、堆、GC、连接池等待/超时、慢 SQL）、AlertManager 按 severity 路由与同源抑制模板。
- `docs/assets/adr`：ADR 模板与索引、5 条已采纳决策（revision/BOM、core 零 Spring、机制/策略 SPI、条件装配与生产硬失效、测试双轨）、版本 Roadmap、15 条反模式清单。
- 全部为**非 Maven 资产**：不新增模块、不修改 POM，不参与 reactor 与 `mvn verify`。

### 联调验证

- 所有新增 YAML 文件通过 PyYAML 解析，YAML 内引用路径（`../alerting/*`、`dockerfile`、`perf/smoke.js`）与仓库实际结构一致；
- 模板中的端口（demo-service 8091 / demo-service-b 8092 / gateway 8090）、actuator 端点、`baseon.*` 配置前缀与现有代码逐项核对；
- 本迭代未实际拉起容器与 K8s 集群，模板定位为「复制后按环境替换占位值」，真实演练列入 1.0.0 GA 出口条件。

### 注意事项

1. **模板不要直接放仓库根**：`Dockerfile`、`.gitlab-ci.yml` 放在根目录会被平台自动识别并触发构建/流水线，保留在 `docs/assets` 下按需复制。
2. **demo 未引入 `micrometer-registry-prometheus`**：`/actuator/prometheus` 当前不存在，直接启用 Prometheus 抓取会得到 404（P-032）。
3. **接入 starter-auth 后探针会 401**：必须把 `/actuator/health`、`/actuator/health/liveness`、`/actuator/health/readiness` 放进 `baseon.auth.ignore-paths`（P-031）。
4. **JVM 堆不要写死**：K8s 与 Docker 模板统一使用 `MaxRAMPercentage`，堆随 `limits.memory` 伸缩（P-030）。
5. **Nacos 2.x 必须暴露 9848**：只映射 8848 会导致客户端 gRPC 连接失败，服务注册不上。
6. **CI 模板不含真实凭据**：镜像地址、Nacos Token、AlertManager Webhook 均为占位值，接入时通过 CI/CD Variables 或 Secret 注入。
7. **Trivy 原生 JSON 不是 GitLab Dependency Scanning 报告格式**：模板只归档产物，不挂 `reports.dependency_scanning`，避免平台解析失败。
8. **本迭代无代码与测试变更**：全量回归结果沿用 11 期（`628d784`），无需重新跑 `clean verify`。

---

## [1.0.0-SNAPSHOT] · 11 期：内核启动扩展（baseon-core-boot）

**日期**：2026-11-01

### 新增能力

- `baseon-core-boot`：`core.boot` 逻辑包的物理构件。`baseon-core` 由 ArchUnit 强制零 Spring 依赖，容器相关能力（容器持有者、编程式事务、线程池工厂）必须外置，包名仍为 `io.github.ayongbox.baseon.core.boot`。
- `SpringContextHolder`：`ApplicationContextAware` 静态持有者，提供按类型/名称取 Bean、`getBeanOrNull` 可选获取、事件发布、活动 Profile 与配置读取；容器未就绪时除可选获取外统一抛 `IllegalStateException`，避免静态调用拿到 null 静默失败。
- `TransactionUtil`：静态方法 / Lambda 场景的编程式事务模板。REQUIRED（加入或新建）、REQUIRES_NEW（挂起外层独立提交）、NESTED（保存点）三种传播行为，`afterCommit` / `afterRollback` 回调在无事务时立即执行；事务管理器按类型从容器获取。
- `ThreadPoolFactory`：强制线程命名前缀的线程池统一创建入口，所有提交路径（`execute` / `submit` / `invokeAll` / 定时调度）自动经由 `ContextWrapper` 捕获并注入登录上下文；提供 ABORT / CALLER_RUNS / DISCARD / DISCARD_OLDEST 四种拒绝策略、`buildScheduled` 周期池与 `ThreadPoolStats` 运行快照（活跃数/队列/完成数），供日志与监控端点接入。
- 工程接线：根 POM `<modules>` / `<dependencyManagement>` 与 `baseon-bom` 同步新增坐标；`BaseonCoreBootAutoConfiguration` 注册 `SpringContextHolder`，开关 `baseon.boot.enabled`（默认 true）。
- 19 个新测试（容器持有 4 + 事务 6 + 线程池 7 + ArchUnit 2），全量回归通过。

### 联调验证

- H2 + `DataSourceTransactionManager` 真实事务链路 6 场景：提交后回调、运行时异常回滚、REQUIRES_NEW 外层回滚而内层保留、NESTED 内层回滚外层提交、`status.setRollbackOnly()` 回滚、无事务回调立即执行。
- 线程池 7 场景：命名前缀、提交期上下文透传与工作线程清理、ABORT/CALLER_RUNS 拒绝行为、运行快照、定时任务命名与上下文、参数校验。

### 注意事项

1. **`baseon-core-boot` 与 `baseon-core` 是两个构件**：前者依赖 Spring（optional），后者保持零第三方依赖；业务查文档时按包名 `core.boot` 理解即可。
2. **事务管理器必须唯一**：`TransactionUtil` 按类型取 `PlatformTransactionManager`，多数据源需用 `@Primary` 标默认管理器，否则首次调用抛 `NoUniqueBeanDefinitionException`。
3. **受检异常默认不回滚**：与 `@Transactional` 默认一致；需要回滚时在回调内调用 `status.setRollbackOnly()`。
4. **NESTED 需显式开启**：`DataSourceTransactionManager` 默认 `nestedTransactionAllowed=false`，未开启时抛 `NestedTransactionNotSupportedException`（P-029）。
5. **线程池生命周期归调用方**：工厂只创建不托管，非守护线程会阻止 JVM 退出；上下文在提交（周期任务在调度）时捕获。
6. **Spring 依赖为 optional**：不向下游强制传递；纯 Spring 工程使用 `TransactionUtil` 需自行保证 `spring-tx` 在类路径。

---

## [1.0.0-SNAPSHOT] · 10 期：测试基座（test-base-core + test-base-integration）

**日期**：2026-10-31

### 新增能力

- `baseon-test-base-core`：单元测试基座，零 Spring 依赖。`BaseUnitTest` 基类 / `ContextCleanupExtension` 扩展在每个测试方法后统一清理 `SecurityUtil`、租户忽略、数据权限忽略、灰度标签四类线程上下文，消除手写 `@AfterEach`；`TestConstants` 提供默认租户/用户/traceId；`MockFactory` 快捷构造并写入 `LoginUser`。
- `baseon-test-base-integration`：集成测试基座，提供两套基类——`BaseIntegrationTest`（Testcontainers 自动拉起 MySQL 8 + Redis 7，`@DynamicPropertySource` 注入 datasource/redis，容器 `withReuse` 共享）与 `H2IntegrationTest`（H2 内存库 `MODE=MySQL`，无 Docker 环境 / CI 快速跑）。Redis 用 `GenericContainer`（testcontainers 1.19.x 不再单独发布 redis 模块）。
- 工程接线：根 POM `<modules>` 与 `<dependencyManagement>` 追加两模块；`baseon-bom` 已有对应条目无需改动。框架依赖（JUnit/Mockito/AssertJ/Spring Boot Test/Testcontainers/H2）均为 compile 范围，消费方 `scope=test` 依赖即可传递获得。
- 4 个自测试（core 3 + integration 1），全量回归通过。

### 注意事项

1. **依赖 scope 必须是 test**：test-base 模块带 JUnit/Testcontainers，误设 compile 会把测试框架打进生产包。
2. **`BaseIntegrationTest` 需要 Docker**：无 Docker 环境改用 `H2IntegrationTest`，否则容器启动失败。
3. **存量测试无需迁移**：基座只提供默认能力，已手写 `@AfterEach` 的测试保持原样。
4. **testcontainers 1.19.8 无独立 redis 模块**：Redis 容器用 `GenericContainer` 替代 `RedisContainer`（依赖核心包即可，版本由 BOM 托管）。

---

## [1.0.0-SNAPSHOT] · 9 期：行列级数据权限（starter-data-scope）

**日期**：2026-10-28

### 新增能力

- `baseon-starter-data-scope`：行级数据权限 + 字段级过滤。`@DataScope` 标在 Service 方法/类上，切面从 `DataScopeProvider` 拉取当前用户可见 ID 集合写入线程上下文；`DataScopeLineInnerInterceptor`（继承 MP `BaseMultiTableInnerInterceptor`，JSqlParser AST 改写）自动给 SELECT 追加 `column IN (...)` 条件。支持 `@IgnoreDataScope` 豁免、`baseon.data-scope.ignore-tables` 表白名单、`enabled` 总开关、`strict` 模式（未提供 Provider 时启动即失败，防权限漏配静默放行）。
- 机制/策略分层落地：`DataScopeProvider` SPI 收在 `core.spi`（`visibleIds` / `column` / `visibleFields` 三方法），业务侧实现注入可见范围——**空集 = 查不到数据**（生成 `column = -1` 永假条件）、**null = 不限制**（放行）；框架只承载拦截与改写机制，权限规则留业务。
- 字段级过滤走 Jackson 序列化阶段：`DataScopeFieldSerializerModifier` 包装 BeanSerializer，每次 serialize 按 `visibleFields()` 动态裁剪字段——不改 SQL、不破坏 ORM 映射与索引；`baseon.data-scope.field-filter-enabled=true` 且 classpath 有 ObjectMapper 才装配，与行级互不影响。
- 拦截器顺序固化「租户 → 数据权限 → 分页」：新增 `DataScopeInnerInterceptor` 标记接口落在 starter-mybatis-plus 内做类型锚点（依赖倒置，mybatis-plus 不反向依赖 data-scope），保证分页 count SQL 带全部过滤条件。
- 行级权限只保护读：INSERT/UPDATE/DELETE 不改写，写路径的数据归属由业务自身约束。
- 28 个新测试（拦截器 10 + 切面 6 + 字段序列化 4 + ArchUnit 2 + H2 集成 6），全量构建全绿。

### 联调验证

- H2（MODE=MySQL）+ MyBatis-Plus 全链路集成测试 6 场景通过：`@DataScope` 行过滤、`@IgnoreDataScope` 豁免、无注解放行、空集返回空、多部门 IN 命中、Provider 返回 null 全量放行。

### 注意事项

1. **visibleIds 空集与 null 语义相反**：空集 = 看不到任何数据，null = 不限制。业务实现 `DataScopeProvider` 时「无权限」务必返回空集而非 null，写反等于放开全量数据。
2. **嵌套 @DataScope 内层覆盖外层**：切面用线程栈保存外层现场，内层方法退出后外层条件自动恢复；方法退出（含异常）清理上下文，线程复用不串号。
3. **字段裁剪必须每次 serialize 动态求值**：Jackson 缓存 BeanSerializer，`changeProperties` 静态裁剪会固化成首个请求的结果（P-026）。
4. **继承 BaseMultiTableInnerInterceptor 必须补齐四个 process 方法**：基类默认抛 UnsupportedOperationException，参考 MP 官方 TenantLineInnerInterceptor 源码实现（P-023）。
5. **jsqlparser 5.1 构造 IN 用 ParenthesedExpressionList**：直接传 ExpressionList 渲染无括号、SQL 语法错误（P-024）。
6. **生产建议开 strict**：`baseon.data-scope.strict=true` 时未提供 DataScopeProvider 直接启动失败，避免权限体系漏配导致全量数据暴露。

---

## [1.0.0-SNAPSHOT] · 补充：Swagger Servers 下拉显示服务名

**日期**：2026-10-27

### 新增能力

- `baseon-starter-swagger` 新增 `baseonServerDescriptionCustomizer`：在 springdoc 补全 servers 后（`AbstractOpenApiResource` 的 customiser 阶段）把下拉说明文字从 `Generated server url` 换成服务名，多服务聚合时不必靠 IP+端口辨认文档归属。
- 新增配置项 `baseon.swagger.server-description`：显式指定说明文字；留空回退 `spring.application.name`，两者皆空则跳过。
- 3 个新单元测试覆盖回退服务名、配置优先、无 servers 时静默跳过。

### 注意事项

1. **只改说明文字，不改 url**：直连与经网关聚合两种访问方式下 server url 语义不同，改写会让 Try it out 打到错误地址。
2. **已启动的服务需重新打包才能看到效果**：该定制器随 starter jar 生效，老进程重启前仍显示 `Generated server url`。

---

## [1.0.0-SNAPSHOT] · 8 期：分布式追踪接入（tracing-otel + starter-trace 协同）

**日期**：2026-10-27

### 新增能力

- `baseon-starter-tracing-otel`：OpenTelemetry 分布式追踪一站式接入。聚合 actuator（micrometer-observation）+ micrometer-tracing-bridge-otel + OTLP exporter + feign-micrometer，引入后 Boot 自动装配 OTel SDK、web server span、Feign client span 与 W3C `traceparent` 传播。`baseon.tracing.otel.log-exporter-enabled=true` 开启本地日志导出档（LoggingSpanExporter + SimpleSpanProcessor 立即输出，零基础设施验证链路）；生产关本地档、配 `management.otlp.tracing.endpoint` 上报 collector。
- `baseon-starter-trace` 协同改造：TraceId 过滤器按 classpath 二选一——无 micrometer-tracing 时装配原 `TraceIdFilter`（自有 ID 行为不变）；有时装配新增 `TracerAwareTraceIdFilter`：复用当前 span 的 traceId（`ServerHttpObservationFilter` 顺序 HIGHEST_PRECEDENCE+1 必先执行，currentSpan 稳定可得），MDC 生命周期让位 micrometer（key 同名天然对齐，logback pattern 零改动）；无当前 span（tracing 禁用/NOOP）自动回退自有 ID 逻辑。拆两个过滤器类保证无 OTel 依赖的工程运行期无需 micrometer 类。
- samples/demo-service + demo-service-b 接入 tracing-otel，本地档配置：采样 1.0、`management.otlp.tracing.enabled=false`、log-exporter 开启。
- 8 个新单元测试（TracerAwareTraceIdFilter 3 + tracing-otel 装配 3 + ArchUnit 2），全量 327 个测试全绿。

### 联调验证

- 同一 trace 贯穿三 span：A `http get /demo/call-b` SERVER → A Feign `HTTP GET` CLIENT → B `http get /demo/ping` SERVER，traceId 均为 `f1d813b0b62949cd0b1a47217d22e906`。
- 日志 MDC 对齐：A/B 两边 RequestLogFilter 业务日志的 `[traceId]` 与 span 的 traceId 一致；响应头 `X-Trace-Id` 与 B 服务上下文 traceId 一致。
- 回退验证（单测）：Tracer 无 currentSpan 时回退自有 ID 生成/继承，行为与 7 期一致。

### 注意事项

1. **feign-micrometer 是链路打通的关键**：缺失时 Feign 不注入 `traceparent`，下游另起 trace 导致链路断裂（P-022）；tracing-otel 已聚合，不用本模块而自行桥接 OTel 时必须记得补。
2. **本地无 collector 务必关 OTLP**：`management.otlp.tracing.enabled=false`，否则 exporter 持续重试打噪音日志；生产环境反向操作（关 log-exporter、开 OTLP 配 endpoint）。
3. **复用路径下 starter-trace 不再清理 MDC**：交给 micrometer scope 恢复；业务代码依然不得自行 `MDC.clear()`。
4. **接入后 actuator 随之引入**：默认仅暴露 `/actuator/health`，接了 starter-auth 的服务会被认证拦截，按需加进 `baseon.auth.ignore-paths`。
5. **采样率默认 0.1**：Boot 原生默认值；demo 里 1.0 仅为本地演示，生产高流量服务调回 0.01~0.1。
6. **LoggingSpanExporter 日志行的 MDC traceId 为空**：exporter 回调在 observation scope 关闭后执行，属正常现象，以 span 内容里的 traceId 为准。

---

## [1.0.0-SNAPSHOT] · 7 期：工程化收口（generator + archetype + 网关 + 全量架构/风格卡点）

**日期**：2026-10-27

### 新增能力

- `baseon-generator-maven-plugin`：CRUD 代码生成 Maven 插件，`mvn baseon:generate -Dtable=t_xxx`（goalPrefix 显式锁定为 `baseon`）。渲染核心是纯库（`generator.core`：命名转换/MySQL→Java 类型映射/freemarker 渲染，零 Maven 依赖可单测），Mojo 仅薄壳；表元数据经 `mybatis-plus-generator` 的 ConfigBuilder 读取。输出 9 件套：Entity（含 tenant_id 自动继承 TenantEntity）/Mapper/Service/ServiceImpl/Controller（`R<T>`/`PageResult<T>` 风格与手写代码一致）/Converter(MapStruct)/Req/VO/Resp。参数：`-Dtable`（必填，逗号分隔多表）、`-Dmodule`、`-DbasePackage`、`-DoutputDir`、`-DdbUrl/-DdbUser/-DdbPassword`（默认对齐本地 demo 库）。
- `baseon-archetype-clean`：老项目迁移用空结构原型——生成 pom（import baseon-bom）+ Application + application.yml + logback-spring.xml（复制 starter-trace 内置 pattern，local/dev 下业务包 DEBUG）。
- `baseon-archetype-sample`：新项目全景模板——clean 全部内容 + 示例 Controller/Service/单测（演示 `R<T>` 统一返回、`PageQuery` 分页、`BizException` 抛出）。生成时可用 `-DbaseonVersion=x.y.z` 指定脚手架版本（默认 1.0.0-SNAPSHOT）。
- `samples/demo-gateway`：Spring Cloud Gateway 示例（端口 8090），注册 Nacos；`discovery.locator` 自动路由 + 一条显式路由示例（`/api/demo/**` StripPrefix=2 → `lb://baseon-demo-service`）。
- **Swagger 网关聚合方案落地**：业务服务只保留 `/v3/api-docs`、关闭各自 swagger-ui（`springdoc.swagger-ui.enabled=false`）；网关引 `springdoc-openapi-starter-webflux-ui`（与 webmvc 线同版本 2.5.0，父 POM 统一管控），`GatewaySwaggerDocConfig` 从 DiscoveryClient 动态生成各服务文档地址填入 UI 下拉列表（启动一次 + HeartbeatEvent 刷新，排除网关自身）。demo-service-b 本期补入 swagger starter。
- **全量 ArchUnit**：19 个 starter 各补架构约束测试——不得依赖 samples；不得依赖「pom 声明的 starter 直接依赖 + 传递闭包」之外的 starter 包。core 的 ArchUnit 规则不变。
- **Checkstyle 构建卡点**：父 POM 接入 maven-checkstyle-plugin 3.6.0（内核锁 10.18.0），精简规则集 15 条（仓库根 checkstyle.xml：未用 import/星号 import/花括号/缩进/行尾空格/文件末尾换行/每行单语句/switch default 等），绑定 `mvn verify` 全模块卡点，存量 43 处违规一次修齐。

### 联调验证

- 网关路由：`GET :8090/demo-service-b/demo/ping` 200（traceId 透传）；显式路由 `/api/demo/**` 到达 demo-service 处理器链。
- 聚合文档：`:8090/v3/api-docs/swagger-config` 列出两个服务、排除网关自身；`:8090/swagger-ui.html` 302 → webjars UI；两服务 api-docs 均可经网关拉取；业务服务 swagger-ui 404、api-docs 200。
- 生成器实测：对真实表 `t_tenant_user` 生成 9 文件，Controller 风格与手写一致。
- archetype 出口标准「5 分钟起服务」：sample 生成 → `mvn test` 绿 → `spring-boot:run` → `/example/hello` 200；clean 生成 → compile 绿。
- 全量回归：`mvn -T 1C clean verify` 全绿（319 个单测 + 28 模块 checkstyle 0 违规）。

### 注意事项

1. **`mvn baseon:generate` 短前缀需配 pluginGroup**：在 `~/.m2/settings.xml` 的 `<pluginGroups>` 加 `io.github.ayongbox` 后才可用短前缀；否则用全限定坐标 `mvn io.github.ayongbox:baseon-generator-maven-plugin:1.0.0-SNAPSHOT:generate`。
2. **mybatis-plus-generator 不传递 MP 核心包**：插件 pom 已显式补 `com.baomidou:mybatis-plus`，改动插件依赖时不要误删（P-020）。
3. **网关侧不要关 springdoc api-docs**：webflux 线 `api-docs.enabled=false` 会把 swagger-ui 的配置 Bean 一并带走导致启动失败（P-019）；业务服务侧关 UI 留 docs 安全。
4. **Spring 代理类构造器用 protected**：Checkstyle 的 HideUtilityClassConstructor 不适用于 `@Configuration`/启动类，private 构造器会破坏 CGLIB 代理（P-021）。
5. **网关聚合的服务列表刷新有心跳延迟**：服务上下线后下拉列表依赖 Nacos HeartbeatEvent（约 30s）刷新，非实时。
6. **生成器未映射的列类型会显式抛错**（如 blob/time），遇到后按提示补 `JdbcTypeMapper` 映射，不静默降级。

---

## [1.0.0-SNAPSHOT] · 6 期：通用工具批（excel + swagger + log + debug-tools）

**日期**：2026-10-27

### 新增能力

- `baseon-starter-excel`：`ExcelTemplate` 封装 FastExcel——导出自动写 xlsx 响应头（文件名 UTF-8 编码、自动补 `.xlsx` 后缀）；导入逐行容错（单行脏数据不中断整批），`ImportResult` 携带成功行与失败行明细（物理行号 + 原始内容 + 原因）；非 xlsx 文件先嗅探 ZIP 魔数再解析，避免 FastExcel 静默降级 CSV 解析返回空结果。
- `baseon-starter-swagger`：OpenAPI3 元信息装配（title/description/version 可配）+ 全局 Bearer security scheme（与 starter-auth 联动，Swagger UI 右上角贴 token 即可调试受保护接口）；生产硬关闭双层机制：装配层条件拦截定制 Bean，`EnvironmentPostProcessor` 层强制覆盖 `springdoc.api-docs.enabled=false` 与 `swagger-ui.enabled=false`（即使 Nacos/yml 误开也无效），触发 profile 名单 `baseon.swagger.prod-profiles` 可配（默认 `[prod]`）。
- `baseon-starter-log`：`@OperateLog(module, action)` 业务语义操作审计——环绕采集操作人/租户/traceId/方法签名/入参摘要（`recordArgs=false` 可关，`argMaxLength` 截断）/耗时/成败结果，异常原样上抛不吞；输出走 `OperateLogHandler` SPI，默认 `LoggingOperateLogHandler` 打日志（success INFO / failed WARN），业务声明自己的实现即可改写库/MQ。
- `baseon-starter-debug-tools`：`/debug/context`（当前登录上下文 + traceId + 请求头摘要）、`/debug/profiles`（生效 profile 与开关状态）；生产硬失效三层：装配期 `Condition` 拦截端点注册、端点方法内二次拦截返回 404、WARN 留痕——debug 端点暴露上下文属信息泄露面，是硬失效而非可配置关闭。
- `core` 新增 `@OperateLog` 注解（module/action/recordArgs）。
- `samples/demo-service` 接线：`/demo/excel/export|import`（导出当前租户用户、导入逐行容错落库）、`TenantUserController.create` 挂 `@OperateLog`、auth 白名单放行 swagger/debug 端点、Swagger 元信息配置。
- 47 个新单元测试（excel 10 + swagger 15 + log 7 + debug-tools 15），全量 255 个测试全绿。
- 联调验证：Swagger api-docs/UI 可访问且携带 bearerAuth；debug 端点放行并返回上下文；Excel 导出 200（中文文件名正确编码）+ 导出文件回灌导入 5 行全成功；`@OperateLog` 审计日志字段完整；prod profile 下 `/v3/api-docs`、`/swagger-ui/*`、`/debug/*` 全部 404 且两条硬失效 WARN 留痕。

### 注意事项

1. **`ignore-paths` 是整体覆盖**：starter-auth 的白名单默认仅 `/auth/login`、`/auth/refresh`，业务在 yml 配置时会整体替换默认列表（demo 中为放行 swagger/debug 需把 login/refresh 一并列出），与请求日志排除路径的双 key 设计不同，注意别锁死登录入口。
2. **导出 fileName 不含扩展名**：`ExcelTemplate.export` 自动补 `.xlsx`，调用方再带后缀会得到 `xx.xlsx.xlsx`。
3. **FastExcel 按 getter 取值**：导出 DTO 必须有标准 getter/setter，否则只导出表头。
4. **commons-io 版本收敛**：fastexcel（2.16.1）与 feign-form-spring（2.11.0）传递依赖分歧，父 POM 已锁定 2.16.1（P-018）。
5. **JWT 篡改测试勿改签名末字符**：Base64URL 末字符含填充 bit，改动可能不破坏签名导致测试概率性失败，应改倒数第二个字符（P-017）。
6. **demo  jar 资源缺失排查**：reactor 全量打包偶发丢 `application.yml` 等资源导致启动报 `No spring.config.import property has been defined`，对 demo 模块单独 `clean package` 重打即可（P-016 同类）。

---

## [1.0.0-SNAPSHOT] · 5 期：安全与韧性（auth + xss + sentinel + resilience + idempotent）

**日期**：2026-10-24

### 新增能力

- `baseon-starter-auth`：JWT 双 Token（access + refresh）签发/校验/刷新/吊销；`AuthTokenFilter` 验签后写登录上下文；`@RequiresPermission` 方法级鉴权（未登录 1002/401，权限不足 1003/403）；refresh 会话与 access 黑名单托管 Redis，支持登出吊销。
- `baseon-starter-xss`：`XssFilter` 包装请求，对 query/form 参数与请求头做 HTML 转义；`XssStringDeserializer` 在 JSON 反序列化阶段转义字符串字段；`baseon.xss.exclude-paths` 为富文本等场景留例外通道。
- `baseon-starter-sentinel`：`BlockException` 统一异常映射（限流 429/1005，熔断降级 503/9003）；`TenantHotParamGuard` 租户维度热点参数限流（热点参数 = 当前租户 ID，无上下文兜底 -1）；规则 Nacos 持久化走 SCA 原生 `spring.cloud.sentinel.datasource.*`。
- `baseon-starter-resilience`：`@Degrade` 三策略取值兜底（CACHE / DEFAULT / FAIL_FAST）；Caffeine 降级结果缓存（容量上限 + 写入过期）；熔断状态变更告警 `AlarmHook` SPI；熔断/舱壁直接复用 resilience4j 原生 `@CircuitBreaker` / `@Bulkhead`。
- `baseon-starter-idempotent`：`@Idempotent` 注解 + Redis `SET NX EX` 原子占位防重复提交；幂等 key 三级来源（SpEL 表达式 → 请求头令牌 → 方法级兜底）；引入 starter-tenant 后 key 自动带租户前缀。
- `core` 新增 `secure` 包契约（`AuthProvider` / `PermissionChecker`），业务声明自己的 `AuthProvider` Bean 即可整体替换认证体系（SSO/OAuth2），默认 `TokenService` 退避。
- `LoginUser` 新增 `permissions` 字段：权限串随 Token claims 携带，`@RequiresPermission` 切面据此比对。
- `RedisUtil` 新增 `setIfAbsent`（SET NX EX 原子语义），幂等占位与分布式锁共用。
- `starter-web` 拦截器协调改动：`UserContextInterceptor` 发现 `SecurityUtil.isLogin()` 时跳过 Header 覆盖，`AuthTokenFilter` 先于其执行，认证上下文不会被伪造 Header 顶掉，两种信任模式幂等共存。
- `samples/demo-service` 接线 5 个 starter：`/auth/login|refresh|logout|me`、`/demo/sentinel/qps|tenant-qps`、`/demo/resilience/call-b`（熔断 + CACHE 降级）、`/demo/xss/echo|echo-json`，租户用户接口加权限与幂等注解。
- 53 个新单元测试（auth 18 + xss 7 + sentinel 8 + resilience 9 + idempotent 7 + core 4），全量 208 个测试全绿。
- 联调验证：鉴权（登录/权限/刷新/吊销）、Sentinel 限流（资源 QPS + 租户热点）、幂等（重复提交 1006/409）、XSS（表单/JSON 转义）、熔断降级完整闭环（CLOSED→OPEN 告警 → CallNotPermitted 快速失败 + CACHE 兜底 → 10s 半开 → 探测成功 → CLOSED 告警）均通过。

### 注意事项

1. **Filter 层异常到不了 `@RestControllerAdvice`**：`AuthTokenFilter` 在 DispatcherServlet 之前执行，认证失败不经过 `GlobalExceptionHandler`，必须在 Filter 内自写 401 JSON（格式固定为 `{"code":1002,"msg":...}`，与全局返回体一致）；同理 `BlockException` 不是 `BizException`，需 starter-sentinel 的 Advice 接管，业务自定义 Filter/底层组件时不要再指望全局异常处理器兜底（P-012）。
2. **auth secret 是安全配置**：不足 32 字节启动直接失败（HS256 硬性要求）；demo 中的明文密钥仅本地演示，生产必须走环境变量或 KMS 注入，禁止照搬提交。
3. **Redis 是吊销状态的唯一载体**：refresh 会话与 access 黑名单都依赖 Redis，Redis 不可用等同于「登出/吊销不生效」，需纳入监控。
4. **XSS 转义与富文本冲突**：编辑器提交的 HTML 会被无差别转义成实体，此类接口必须加入 `baseon.xss.exclude-paths` 并由业务自行做白名单过滤；转义后入库的数据不可还原原文。
5. **@Degrade 依赖 AOP 代理**：同类自调用降级不生效；CACHE 策略是多实例独立的本地缓存，不保证跨节点一致，接口契约上需让调用方知晓可能拿到兜底值。
6. **幂等占位失败不删 key**：窗口（默认 10s）内只放行一次，失败重试与恶意重复提交无法区分；需要「失败立即放行重试」语义的接口不要标 `@Idempotent`。
7. **ignore-paths / 热点规则索引**：登录与刷新接口必须在 `baseon.auth.ignore-paths` 内（默认已含）；`TenantHotParamGuard` 的热点参数索引固定为 0，配 `ParamFlowRule` 时 `paramIdx` 必须为 0，否则规则不命中。
8. **Sentinel datasource 的 Map key 不能带连字符**：Spring Boot 绑定 `Map` 配置时会剥离 key 中的连字符（`param-flow` → `paramflow`），导致数据源静默不生效；yml key 用纯字母数字，`rule-type` 的值不受影响（P-013）。
9. **Nacos 3.x OpenAPI 路径变更**：v1 接口已移除，脚本发布配置/登录用 `POST /nacos/v3/auth/user/login` 与 `POST /nacos/v3/admin/cs/config?accessToken=...`（group 参数名 `groupName`，P-014）。
10. **@Degrade 与 @CircuitBreaker 组合的切面顺序**：resilience4j-spring6 2.2.x 熔断器切面默认 order 为 `LOWEST_PRECEDENCE - 4`（字节码确认）；`DegradeAspect` 固定 `LOWEST_PRECEDENCE - 5` 稳处其外层，否则兜底返回值被熔断器误判为成功、熔断永不打开（P-015）。业务自定义 `circuitBreakerAspectOrder` 时必须大于该值。
11. **starter 改动后重打 demo 包需 clean package**：Maven jar 插件不感知依赖 jar 更新，仅 package 会把旧版 starter 打进 fat jar（P-016）。

---

## [1.0.0-SNAPSHOT] · 4 期：数据与缓存（datasource + mybatis-plus + redis + cache + tenant）

**日期**：2026-10-23

### 新增能力

- `baseon-starter-datasource`：动态数据源，`@DS` 注解显式切换；`@Transactional(readOnly=true)` 由 `ReadOnlyRouteAspect` 在事务开启前探测并轮询路由从库；Hikari 生产默认参数（20/5/3s/30min）。
- `baseon-starter-mybatis-plus`：InnerInterceptor 链顺序固化（租户 → 分页）；逻辑删除与驼峰映射全局默认值（用户显式配置优先）；公共字段填充（createTime/updateTime/deleted/tenantId）；慢 SQL 拦截（默认阈值 1000ms，WARN 日志 + 可选 Micrometer 指标）。
- 慢 SQL 拦截降噪改造：日志补 `tenantId` / `traceId` 归因信息；参数摘要按实体字段渲染（不再输出 `Object@hashCode`）；每个 statementId 前 3 次预热不判定；同一 statementId 的 WARN 限流 60s（指标仍逐次计数）；新增开关 `baseon.mybatis-plus.slow-sql-enabled`，关闭时拦截器不进入插件链。
- `baseon-starter-redis`：key 规范校验（白名单字符、禁止连续冒号等，strict 模式抛异常）；`RedisLock` 分布式锁（SET NX EX + Lua compare-and-del 防误删）；`redisTemplate` 统一 String key + JSON value 序列化。
- `baseon-starter-cache`：`CacheHelper` 两级缓存（L1 Caffeine + L2 Redis）；空值标记防穿透、回源互斥锁防击穿、TTL 随机抖动防雪崩；`CacheKeyPrefixProvider` SPI 支持 key 前缀。
- `baseon-starter-tenant`：`TenantLineHandler` 读登录上下文 tenantId 自动过滤 SQL（无租户上下文时兜底 -1 查不到数据，strict 模式抛异常）；`@IgnoreTenant` 跨租户放行；缓存 key 自动追加 `t<tenantId>:` 前缀实现缓存租户隔离；`@IgnoreTenantCache` 关闭前缀。
- 租户忽略新增编程式 API：core 的 `TenantIgnoreContext` 提供 `callSqlIgnored` / `runCacheIgnored` 等代码块级入口（嵌套安全，call/run 前缀区分函数形态避免 lambda 歧义）；starter-tenant 新增 `TenantOps.callWithSqlIgnored`，与注解切面同一套双保险，覆盖定时任务、消息消费等非 Web 场景。
- 文档补充：cache 手册新增「扩展方向：binlog 自动失效」章节（目标架构、与现有组件衔接点、适用边界）；tenant 手册新增「编程式忽略」与「复杂 SQL 与手动租户条件」两节。
- `core` 新增：`annotation.IgnoreTenant` / `IgnoreTenantCache`、`context.TenantIgnoreContext`、`spi.CacheKeyPrefixProvider`。
- 83 个新单元测试（datasource 19 + mybatis-plus 19 + redis 20 + cache 7 + tenant 13 + core 5）。

### 注意事项

1. **MP 3.5.9 构件拆分**：`PaginationInnerInterceptor` 等已移至 `mybatis-plus-jsqlparser`，仅引 starter 会编译失败（P-010）。
2. **读写分离的探测时机**：`AbstractRoutingDataSource` 在事务内读不到 `readOnly` 标记，必须靠前置 AOP；嵌套事务不做切换（P-011）。
3. **缓存一致性边界**：L1 本地缓存不做失效广播，靠短 TTL（默认 30s）收敛；写后立刻强一致读取的场景请显式 `evict`。
4. **@DS 必须在事务边界外**：事务开启后连接已绑定，方法内切换数据源无效。
5. **demo 的 logging 已收窄至 demo 包**：`io.github.ayongbox.baseon.demo: debug` 保留 mapper SQL 逐条输出（学习用），框架 starter 包恢复 INFO；业务工程切勿照搬 `io.github.ayongbox: debug` 通配到生产，SQL 参数值逐条打印会打爆日志且可能泄露敏感数据。

---

## [1.0.0-SNAPSHOT] · 3 期：微服务三件套（cloud-nacos + cloud-feign + loadbalance）

**日期**：2026-10-23

### 新增能力

- `baseon-starter-cloud-nacos`：引入 SCA 2023.0.3.4，一个依赖同时获得注册发现 + 配置中心；`ConfigHolder` 直读 `Environment`，配置刷新免 `@RefreshScope`。
- 三档注册策略 `baseon.cloud.nacos.register-strategy`：`LOCAL_NO_REGISTER`（本地起服务不污染注册中心）/ `LOCAL_REGISTER_PRIVATE`（注册但打 `baseon.local=true` 标记，其他服务默认不路由过来）/ `FULL_REGISTER`（生产默认）。
- `baseon-starter-cloud-feign`：`ContextPropagateRequestInterceptor` 自动透传 `X-Trace-Id`、`X-User-Id` / `X-Tenant-Id` / `X-Username`、`X-Gray-Tag`，不覆盖调用方已显式设置的请求头；默认超时 connect 3s / read 5s，默认不重试（`baseon.cloud.feign.retry.enabled=true` 才开启）。
- `baseon-starter-loadbalance`：`GrayTagLoadBalancer` 灰度路由——带 `X-Gray-Tag` 只选元数据 `gray-tag` 相同实例（无匹配回退全部 + WARN 降级）；不带标签排除灰度实例与 `baseon.local` 本地实例；对 Feign / RestTemplate / WebClient 全生效。
- `baseon-core` 新增 `GrayContext`（ThreadLocal）与 `CloudHeaders` 常量（`X-Gray-Tag` / `gray-tag` / `baseon.local`，固定不可配置，防三 Starter 配置漂移）。
- `GrayContextFilter`：Servlet 入口解析 `X-Gray-Tag` 写入 `GrayContext`，请求结束自动清理。
- `samples/demo-service-b`（8081）：下游验证服务，回显请求头与上下文。
- `demo-service` 接入三件套：`@EnableFeignClients` + `DemoBClient` + `/demo/call-b`（跨服务调用）+ `/demo/config-info`（配置中心读取）。
- 23 个新单元测试（nacos 5 + feign 6 + loadbalance 8 + core 4），全量 72 个单测通过。

### 联调验证记录

- 两服务注册 Nacos 成功，`demo-service-b` 双实例（8081 正常 / 8082 带 `gray-tag=gray` 元数据）。
- 无标签调用 `/demo/call-b` → 稳定路由 8081；带 `X-Gray-Tag: gray` → 路由 8082；带不存在标签 → 回退正常实例并打 WARN。
- traceId / userId 经 Feign 透传，下游请求头与上下文恢复完整，两服务日志 traceId 一致。
- Nacos 发布 `baseon-demo-service.yaml` 后 `/demo/config-info` 无需重启读到新值。

### 注意事项

1. **Nacos 3.x 端口分离**：`server-addr` 必须指向主端口（默认 8848，客户端 API + gRPC），8181 只是控制台 UI，指向它会导致 `Client not connected`（详见 pitfalls P-009）。
2. **Header 常量不可配置**：`X-Gray-Tag` / `gray-tag` / `baseon.local` 三个名称固定在 `CloudHeaders`，灰度链路跨三个 Starter，允许各自配置会出现同链路名称不一致的隐蔽故障。
3. **Feign 默认不重试**：超时即失败，由网关或调用方决定重试策略；开启重试前必须确认下游接口幂等。
4. **灰度回退是降级不是路由**：无匹配灰度实例时回退到全部实例保证可用，WARN 日志是定位灰度发布遗漏的关键信号，不要屏蔽。
5. **SCA 配置中心强制 import-check**：引入 nacos config 后必须声明 `spring.config.import`（可用 `optional:nacos:` 前缀）或显式关闭检查，否则启动失败（详见 pitfalls P-008）。

### 升级动作

- 父 POM 与 `baseon-bom` 引入 `spring-cloud-alibaba-dependencies 2023.0.3.4`，登记三个新构件。
- `demo-service` 新增三个 starter 依赖与 `spring.config.import=optional:nacos:baseon-demo-service.yaml`。

---

## [1.0.0-SNAPSHOT] · 2 期：starter-web + starter-trace

**日期**：2026-10-22

### 新增能力

- `baseon-starter-web` 模块，通过 `AutoConfiguration.imports` 自动装配，业务无需 `@Import`。
- 全局异常处理：`BizException` 按 `ErrorType` 映射 HTTP 状态；参数校验失败 400；未知异常 500 且不泄露内部信息。
- Jackson 统一配置：`Long` 转字符串（防 JS 精度丢失）、`LocalDateTime/LocalDate/LocalTime` 与 `java.util.Date` 格式统一。
- 用户上下文拦截器：`X-User-Id` / `X-Tenant-Id` / `X-Username` → `SecurityUtil`，请求结束自动清理。
- 请求日志过滤器：出入参脱敏（password / token / mobile / Bearer 等）+ 截断 + 状态码 + 耗时；multipart 与二进制跳过。
- CORS：默认关闭，配置 `baseon.web.cors.allowed-origins` 后开启。
- `baseon-starter-trace` 模块：TraceId 生成/继承、MDC 注入、线程池/异步/定时任务透传。
- `TraceIdFilter`：请求入口生成 32 位 TraceId，响应头返回 `X-Trace-Id`，请求结束清理。
- `MdcTaskDecorator`：自动包装所有 `ThreadPoolTaskExecutor`，异步任务携带父线程 MDC。
- `ScheduledTraceAspect`：定时任务执行周期内生成独立 TraceId，执行结束清理。
- `BizException` 增加 `ErrorType` 字段；`ErrorType` 增加 `METHOD_NOT_ALLOWED(405)`。
- 静态资源 404 治理：`NoResourceFoundException` 返回 404，不再走未知异常的 500 + ERROR 堆栈（favicon.ico 场景）。
- 请求日志排除规则双 key：`exclude-paths`（完全覆盖默认列表）+ `additional-exclude-paths`（在默认之上追加）。
- 排除路径的 5xx 强制打简版访问日志：服务端错误不被排除规则吞掉；4xx 遵循排除规则保持安静。
- 统一 logback 配置：starter-trace 内置 `logback-spring.xml`，日志行自动携带应用名与 traceId；业务放同名文件可整体覆盖。
- 25 个单元测试（web 13 个 + trace 12 个）。

### 注意事项

1. **Header 信任前提**：用户信息 Header 必须由网关校验 JWT 后注入，业务服务不得直接暴露公网。
2. **CORS 默认关闭**：`allowed-origins` 为空时不注册跨域配置；生产环境禁止 `*` 与 `allow-credentials=true` 组合。
3. **日志脱敏不完整**：正则只覆盖常见敏感字段，不能替代字段级加密与日志平台规则；`max-body-length` 默认 1000。
4. **可覆盖**：所有自动装配 Bean 均带 `@ConditionalOnMissingBean`，业务声明同类型 Bean 即生效。
5. **响应日志固定 UTF-8**：Tomcat 的 `getCharacterEncoding()` 默认 ISO-8859-1，直接使用会导致中文乱码（详见 pitfalls P-006）。
6. **TraceId 与业务上下文分离**：trace 只管 traceId，userId/tenantId 仍由 web 拦截器负责；两者不互相依赖。
7. **定时任务 TraceId 独立生成**：与请求 TraceId 无关联，用于排查定时任务自身执行链路。
8. **MDC 清理必须成对**：`TraceIdFilter` 和 `ScheduledTraceAspect` 都在 finally 中清理，业务代码不得自行 `MDC.clear()`。

### 升级动作

- demo 移除 `spring-boot-starter-web` 直接依赖，改为依赖 `baseon-starter-web`，`baseon-core` 由其传递引入。
- demo 新增 `baseon-starter-trace` 依赖。

---

## [1.0.0-SNAPSHOT] · 1 期：基础内核

**日期**：2026-10-22

### 新增能力

- `baseon-core` 模块建立，物理单 Jar、逻辑多包。
- 统一返回体 `R<T>`：`ok()` / `ok(data)` / `fail(ErrorCode)` / `fail(BizException)` / `fail(code, msg)`。
- 统一分页体 `PageResult<T>`：`total/pages/current/size/records`。
- 分页请求基类 `PageQuery`：默认 `current=1`、`size=10`，上限 `500`，非法值自动回落。
- 错误码契约 `ErrorCode` + 内置枚举 `CommonErrorCode`（01xxx 系统段、09xxx 远程调用段）。
- 错误语义 `ErrorType`：自带 HTTP 状态码（400/401/403/404/409/429/500/502/503），Starter 直接映射。
- 业务异常 `BizException`：携带错误码，**不采集堆栈**。
- 实体父类 `BaseEntity`（含逻辑删除字段 `deleted`）、多租户父类 `TenantEntity`、登录用户模型 `LoginUser`。
- 工具：`StrUtil`、`MoneyUtil`（scale=2 HALF_UP）、`BeanCopyUtil`。
- 上下文：`SecurityUtil`（ThreadLocal）、`ContextWrapper`（Runnable / Callable / wrapSupplier）。
- 单元测试 19 个（`R`、`PageResult`、`PageQuery`、`ContextWrapper`）+ 3 条 ArchUnit 规则，`mvn test` 强制校验。

### 注意事项

1. `baseon-core` 编译期与运行期**零第三方依赖**，JSON 注解、Spring 注解一律不得进入内核。
2. 内核 POJO 中非属性的判断方法**禁止使用 `is` / `get` 前缀**，否则会被 Jackson 序列化为额外字段（详见 pitfalls P-002）。
3. `BizException` 关闭了堆栈采集，只适用于预期内业务失败；系统故障请抛原生异常或 `SystemException`（待实现）。
4. `SecurityUtil` 的写入与清理由 `starter-web` 拦截器负责，业务代码只读；异步场景必须用 `ContextWrapper`。
5. `BaseEntity` 不含 ORM 注解，字段映射由 `starter-mybatis-plus` 通过命名约定完成。
6. 业务错误码**必须显式覆盖 `getType()`**，默认值是 `SERVER_ERROR`，不覆盖会被当作 500 并触发系统告警。
7. 分页查询对象统一继承 `PageQuery`，禁止自定义 `pageNum/pageSize`；`size` 超过 500 会被静默截断。
8. `ContextWrapper` 的 Supplier 重载命名为 `wrapSupplier`，因为 `wrap(Callable)` 与 `wrap(Supplier)` 对无参 lambda 存在重载歧义。

### 升级动作

无（初始版本）。

---

## [1.0.0-SNAPSHOT] · 0 期：工程骨架

**日期**：2026-10-22

### 新增能力

- `baseon-parent`：Maven 聚合 + 父 POM，`${revision}` 统一版本。
- `flatten-maven-plugin`：安装/发布前展开 `${revision}`，生成 `.flattened-pom.xml`。
- `maven-enforcer-plugin`：Maven/JDK 版本门禁、依赖冲突检测、重复声明检测。
- `baseon-bom`：业务工程依赖版本单一入口。
- `samples/demo-service`：构建链路验证工程。

### 注意事项

1. 导入的 BOM **只传递 dependencyManagement，不传递 pluginManagement**，所有构建插件必须在父 POM 显式锁版本（详见 pitfalls P-001）。
2. 父 POM 与 `baseon-bom` 各自维护一份内部构件版本：前者服务脚手架自身构建，后者服务外部业务工程。
3. 开发期 `revision` 为 `1.0.0-SNAPSHOT`，执行 `mvn verify -Prelease` 会因 `requireReleaseDeps` 规则失败，这是发布前门禁的预期行为。

### 升级动作

无（初始版本）。

---

## 待办（尚未进入任何版本）

- 2 期：`starter-web`（全局异常、Jackson 统一配置、拦截器写上下文、出入参日志）、`starter-trace`。
- 1 期剩余：`core.boot`（线程池工厂、事务模板）、`core.cloud`、`core.db`、`core.secure`、`core.launch`、`core.spi`。
