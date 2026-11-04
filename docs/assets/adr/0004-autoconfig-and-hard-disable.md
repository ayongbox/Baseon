# ADR-0004 · 条件装配与生产危险能力双层硬失效

- **状态**：已采纳（2 期起，6 期补强）
- **日期**：2026-10（2 期起）
- **决策者**：脚手架维护者
- **关联**：各 Starter 的 `BaseonXxxAutoConfiguration`、`SwaggerEnvironmentPostProcessor`、`DebugProdProfileGuard`

## 背景

脚手架由 20+ Starter 组成，业务按需引入。装配必须满足：

- 引入即生效，不要求业务写 `@ComponentScan` 或额外配置类；
- 业务可整体替换默认 Bean，而不是被框架锁死；
- Swagger、调试端点、灰度等能力在生产必须不可用，**不能只依赖 yml 开关**（配置误提交、配置中心误改都会击穿）。

## 决策

1. 每个 Starter 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登记装配类，不依赖业务扫描包；
2. 默认 Bean 一律 `@ConditionalOnMissingBean`；整体开关 `baseon.<能力>.enabled` 默认 `true`（`matchIfMissing = true`）；
3. 只在类路径满足条件时装配可选能力（如 Micrometer 存在才注册慢 SQL 指标回调），避免硬依赖；
4. 生产危险能力采用**双层硬失效**：
   - 第一层：装配条件按 active profile 判定，命中生产 profile 直接不装配；
   - 第二层：入口再执行一次 profile 守卫（如 `DebugEndpointController` 每个端点调用 `DebugProdProfileGuard`），即使装配被误判也不可用；
   - Swagger 额外用 `EnvironmentPostProcessor`（`SwaggerEnvironmentPostProcessor`）在环境准备阶段改写配置，优先级高于 yml / 配置中心；
5. 生产 profile 名单统一由 `baseon.<能力>.prod-profiles` 配置，默认 `["prod"]`，按企业命名扩展。

## 后果

### 正面

- 业务引入 Starter 即获得默认能力，替换默认 Bean 不影响框架其他部分；
- 生产误配不会导致 Swagger/调试端点暴露；
- 可选依赖缺失时降级而不是启动失败。

### 负面 / 代价

- 双层判定增加代码量，新增危险能力必须照此实现，不能只写一个 `@Profile`；
- profile 命名不一致时（如生产用 `production` 而非 `prod`）需要显式扩展 `prod-profiles`；
- `EnvironmentPostProcessor` 在测试中需要单独覆盖，不能只测自动配置。

## 备选方案

| 方案 | 放弃原因 |
|---|---|
| 只靠 `baseon.xxx.enabled=false` | 配置错误即可击穿，事故不可接受 |
| 只用 `@Profile("!prod")` | 与配置中心动态 profile 配合不稳定，且无法防御误判 |
| 让业务自行引入 Swagger 依赖 | 无法保证生产关闭，且与文档约定脱节 |

## 验证方式

- `SwaggerEnvironmentPostProcessorTest` / `SwaggerEnvironmentPostProcessor` 相关测试；
- `DebugEndpointController` 在 prod profile 下返回 404 的集成测试；
- 所有 Starter 在 `AutoConfiguration.imports` 中可检索到装配类。

## 变更记录

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-10 | 2 期建立条件装配约定 | 脚手架维护者 |
| 2026-10 | 6 期补强 Swagger 与调试端点双层失效 | 脚手架维护者 |
