# ADR-0001 · 统一版本与 BOM 依赖治理

- **状态**：已采纳（0 期建立）
- **日期**：2026-10（0 期）
- **决策者**：脚手架维护者
- **关联**：根 `pom.xml`、`baseon-bom`、`docs/pitfalls.md` P-001

## 背景

仓库是 Maven 多模块 reactor，内核、Starter、示例工程共享同一套依赖。若每个模块独立声明版本，会出现：

- 同一依赖在不同模块解析到不同版本，`dependencyConvergence` 失败；
- 业务工程引入脚手架后仍要自己维护 Boot / Cloud 版本，升级成本转移给使用方；
- BOM `import` 只传递 `dependencyManagement`，**不传递 `pluginManagement`**，未锁定版本的插件被 Maven 解析到里程碑版本（P-001）。

## 决策

1. 全仓库使用单一属性 `<revision>1.0.0-SNAPSHOT</revision>`，子模块 parent 写 `${revision}`，**子模块 pom 禁止出现 `<version>`**；
2. 父 POM 的 `pluginManagement` 显式锁定全部构建插件（compiler、surefire、enforcer、flatten、checkstyle、archunit 等）；
3. `baseon-bom` 是业务工程唯一 import 入口，承载所有发布构件的版本；
4. enforcer 绑定 `validate`，规则为 `requireMavenVersion [3.8.6,)`、`requireJavaVersion [17,)`、`banDuplicatePomDependencyVersions`、`dependencyConvergence`；
5. 发布时由 flatten 插件展开 `${revision}`，`.flattened-pom.xml` 不提交。

## 后果

### 正面

- 升级 Boot / Cloud / 第三方版本只改父 POM 属性；
- 业务工程 `import baseon-bom` 后不写版本号，避免版本漂移；
- 构建期即失败，不把版本冲突留到运行期。

### 负面 / 代价

- 版本变更必须改父 POM 并触发全量构建；
- 新增依赖必须同时考虑父 POM `dependencyManagement` 与 `baseon-bom`（前者供 reactor 内使用，后者供外部消费）；
- 本地开发需理解 flatten 产物为何被 gitignore。

## 备选方案

| 方案 | 放弃原因 |
|---|---|
| 每个模块独立版本号 | 发布协调成本高，跨模块兼容性无法保证 |
| 只依赖 `spring-boot-dependencies` 的 import | 插件版本失控（P-001），且无法覆盖脚手架自有构件 |
| 使用 Gradle platform | 现有工程与生成器均基于 Maven，迁移成本大于收益 |

## 验证方式

- `mvn -q -T 1C clean verify` 通过，enforcer 四条规则无告警；
- 新增子模块未写 `<version>` 时仍然可以构建；
- 业务侧仅通过 `baseon-bom` 引入时依赖版本可解析。

## 变更记录

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-10 | 建立 | 脚手架维护者 |
