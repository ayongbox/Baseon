# ADR-0002 · 内核零 Spring 依赖，`core.boot` 外置

- **状态**：已采纳（11 期落地外置构件）
- **日期**：2026-11-01
- **决策者**：脚手架维护者
- **关联**：`baseon-core`、`baseon-core-boot`、`CoreArchitectureTest`

## 背景

`baseon-core` 被所有 Starter 传递依赖，承载 `R<T>`、分页、错误码、上下文、注解、SPI 等纯模型与工具。若内核直接依赖 Spring：

- 纯工具类场景（批处理、单测、非 Spring 服务）被迫带上 Spring 运行时；
- 内核升级与 Boot 升级耦合，无法独立演进；
- ArchUnit 无法用「零第三方依赖」作为可执行约束。

但容器持有者、编程式事务、线程池工厂天然需要 `ApplicationContext` / `PlatformTransactionManager`。

## 决策

1. `baseon-core` 只保留 `model` / `tool` / `context` / `annotation` / `cloud` / `spi` / `secure`，不依赖 Spring 与任何 Starter；
2. 需要容器的 `core.boot` 物理外置为独立构件 `baseon-core-boot`，**包名保持 `io.github.ayongbox.baseon.core.boot`**，业务查文档不需要感知构件拆分；
3. `baseon-core-boot` 仅允许依赖 `spring-context` / `spring-tx` / `spring-boot-autoconfigure`，Spring 依赖标 `optional`，不向下游强制传递；
4. ArchUnit 卡点：`core.model / core.tool / core.context / core.secure` 不得引用 `org.springframework..`；core 不得依赖 `starter..`；model 不得反向依赖 tool/context。

## 后果

### 正面

- 内核可在纯 Java 环境使用，单测无需 Spring 上下文；
- 依赖方向单向，Starter 依赖内核、内核不感知 Starter；
- 容器能力的物理边界清晰，`mvn -pl baseon-core` 可独立验证零依赖。

### 负面 / 代价

- 业务代码不能直接依赖 `baseon-core`（由 Starter 传递），排障时要理解「包名相同、构件不同」；
- `baseon-core-boot` 的清单需要在根 POM、BOM、文档、Skill 四处同步；
- 使用 `TransactionUtil` 的纯 Spring 工程需自行保证 `spring-tx` 在类路径。

## 备选方案

| 方案 | 放弃原因 |
|---|---|
| core 直接依赖 `spring-context` | 破坏零依赖约束，纯工具场景被迫带 Spring |
| 全部容器能力放 `core-boot`，core 连注解也移走 | 注解与上下文是跨 Starter 的通用契约，移走会导致依赖倒挂 |
| 通过反射/SPI 间接访问容器 | 运行期错误延迟暴露，调试成本高，不如构件拆分直接 |

## 验证方式

- `CoreArchitectureTest` 三条规则通过；
- `mvn -q -pl baseon-core verify` 不解析任何 Spring 构件；
- `baseon-core-boot` 的 19 个测试覆盖容器持有、事务传播、线程池上下文透传。

## 变更记录

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-11-01 | 11 期将 `core.boot` 外置为 `baseon-core-boot` | 脚手架维护者 |
