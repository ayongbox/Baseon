# 反模式清单

> 按「错误做法 → 后果 → 正确做法」记录。新增条目前先查 `docs/pitfalls.md`，能从编号引用就不重复描述。

| # | 反模式 | 后果 | 正确做法 | 关联 |
|---|---|---|---|---|
| 1 | 业务工程直接依赖 `baseon-core` | 绕过 Starter 的装配，后续 Starter 升级无法传递 | 只依赖 Starter，`baseon-core` 由其传递引入 | ADR-0002 |
| 2 | 往 `baseon-core` 里加 Spring 类型 | ArchUnit 失败；纯工具场景被迫带 Spring | 需要容器的能力放 `baseon-core-boot` | ADR-0002 |
| 3 | 子模块 pom 写 `<version>` | 版本与 `${revision}` 漂移，`dependencyConvergence` 失败 | 子模块不写版本，由父 POM 统一 | ADR-0001 / P-001 |
| 4 | 手写 `ExecutorService` 或 `CompletableFuture` 不包装上下文 | 异步线程丢失登录人/租户/灰度标签 | 用 `ContextWrapper` 包装，或直接用 `ThreadPoolFactory` | `baseon-core-boot` |
| 5 | 用静态字段缓存当前请求的 `LoginUser` | 线程复用导致串号，越权或审计错误 | 只通过 `SecurityUtil` 取；线程池提交时捕获上下文 | `baseon-core` |
| 6 | 用 `@RefreshScope` 代替 `ConfigHolder` 读配置 | 配置变更后 Bean 重建，连接池/线程池状态丢失 | 动态配置统一走 `ConfigHolder` | `baseon-starter-cloud-nacos` |
| 7 | 新 Starter 依赖 pom 闭包之外的 Starter | 依赖方向失控，ArchUnit 失败 | 显式声明依赖，同步允许清单 | `references/conventions.md` |
| 8 | 生产只靠 `baseon.xxx.enabled=false` 关闭危险能力 | 配置误提交或配置中心误改即暴露 | profile 判定 + 入口二次守卫 + `EnvironmentPostProcessor` | ADR-0004 |
| 9 | 多数据源不指定 `@Primary` 事务管理器 | `TransactionUtil` 首次调用抛 `NoUniqueBeanDefinitionException` | 业务默认管理器标 `@Primary` | CHANGELOG 11 期 |
| 10 | 未开 `nestedTransactionAllowed` 就用 NESTED | 抛 `NestedTransactionNotSupportedException` | 优先 REQUIRES_NEW；确需保存点时自定义事务管理器开启 | P-029 |
| 11 | 缓存 key 不带租户前缀 | 跨租户读到他人缓存，数据越权 | 使用 `CacheHelper` 或实现 `CacheKeyPrefixProvider` | ADR-0003 |
| 12 | 分布式锁/幂等占位不设 TTL | Redis key 永久残留，业务永久被锁 | 锁与幂等 key 必须设置过期时间，并校验租户维度 | `baseon-starter-redis` / `idempotent` |
| 13 | 测试依赖 `baseon-test-base-*` 用 compile 范围 | JUnit/Testcontainers 打进生产包 | 消费方 `scope=test` | `docs/modules/baseon-test-base.md` |
| 14 | 只在 H2 通过就上线 SQL 改写/租户能力 | 方言差异导致生产行为不一致 | SQL 改写、租户、JSON 函数用 Testcontainers MySQL 复测 | ADR-0005 |
| 15 | 告警规则上线前不校验指标存在 | 规则永远不触发，故障无告警 | 先在 Prometheus 查询指标有数据，再启用规则 | P-032 |
