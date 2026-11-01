# ADR-0005 · 测试双轨：单测零 Spring，集成测试 H2 / Testcontainers

- **状态**：已采纳（10 期补建测试基座）
- **日期**：2026-10-31
- **决策者**：脚手架维护者
- **关联**：`baseon-test-base-core`、`baseon-test-base-integration`、`docs/modules/baseon-test-base.md`

## 背景

脚手架横跨纯内核、Starter 装配、SQL 改写、缓存与线程上下文。测试需要同时满足：

- 大量逻辑可快速单测，不启动 Spring、不依赖 Docker；
- SQL 改写、租户隔离、事务传播必须在真实或近似真实的数据库上验证；
- 测试之间不能相互污染线程上下文（登录人、租户忽略、数据权限忽略、灰度标签）。

## 决策

1. `baseon-test-base-core`：零 Spring 依赖，提供 `BaseUnitTest`、`ContextCleanupExtension`、`TestConstants`、`MockFactory`；
   - `ContextCleanupExtension` 在每个测试方法后统一清理四类线程上下文，不再要求手写 `@AfterEach`；
2. `baseon-test-base-integration`：两套基类
   - `BaseIntegrationTest`：Testcontainers 拉起 MySQL 8 + Redis 7，`@DynamicPropertySource` 注入连接，容器 `withReuse`；
   - `H2IntegrationTest`：H2 内存库 `MODE=MySQL`，供无 Docker 环境与快速 CI；
3. 测试框架依赖为 compile 范围，消费方必须使用 `scope=test` 引入，防止打进生产包；
4. 集成测试的建表与种子数据只走一套初始化机制，禁止 `spring.sql.init` 与 `@Sql` 同时使用（P-027）。

## 后果

### 正面

- 单元测试启动成本接近零，反馈快；
- 需要方言/事务语义的用例可切到 Testcontainers；
- 上下文清理标准化，跨测试污染不再是隐性缺陷。

### 负面 / 代价

- H2 与 MySQL 在函数、锁、隔离级别上存在差异，H2 通过的用例不保证生产等价；
- Testcontainers 需要 Docker 与可用镜像源，CI 首次拉取镜像较慢；
- 两套基类增加选择成本，团队需要约定「默认 H2、SQL 改写用 MySQL」的边界。

## 备选方案

| 方案 | 放弃原因 |
|---|---|
| 只用 H2 | SQL 改写、租户插件、JSON 函数等场景方言差异大，误报/漏报都不可接受 |
| 只用 Testcontainers | 每个单测都受 Docker 启动与镜像拉取影响，反馈慢且 CI 不稳定 |
| 自研内存数据库/模拟 SQL | 维护成本高，无法覆盖 JSqlParser 与真实驱动的组合行为 |

## 验证方式

- `baseon-test-base-core` 3 个自测试、`baseon-test-base-integration` 1 个自测试通过；
- 数据权限的 6 个 H2 集成场景通过；
- 全量 `mvn -q -T 1C clean verify` 通过。

## 变更记录

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-10-31 | 10 期补建测试基座 | 脚手架维护者 |
