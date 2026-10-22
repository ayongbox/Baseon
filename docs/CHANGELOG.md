# 迭代能力台账

> 记录每个迭代周期「交付了什么能力、改变了什么、需要业务方注意什么」。
> 版本号统一使用 `${revision}`，发布时由 Maven 注入；开发期按迭代期记录。

---

## [1.0.0-SNAPSHOT] · 2 期：starter-web

**日期**：2026-10-22

### 新增能力

- `baseon-starter-web` 模块，通过 `AutoConfiguration.imports` 自动装配，业务无需 `@Import`。
- 全局异常处理：`BizException` 按 `ErrorType` 映射 HTTP 状态；参数校验失败 400；未知异常 500 且不泄露内部信息。
- Jackson 统一配置：`Long` 转字符串（防 JS 精度丢失）、`LocalDateTime/LocalDate/LocalTime` 与 `java.util.Date` 格式统一。
- 用户上下文拦截器：`X-User-Id` / `X-Tenant-Id` / `X-Username` → `SecurityUtil`，请求结束自动清理。
- 请求日志过滤器：出入参脱敏（password / token / mobile / Bearer 等）+ 截断 + 状态码 + 耗时；multipart 与二进制跳过。
- CORS：默认关闭，配置 `baseon.web.cors.allowed-origins` 后开启。
- `BizException` 增加 `ErrorType` 字段；`ErrorType` 增加 `METHOD_NOT_ALLOWED(405)`。
- 13 个单元测试（异常映射、上下文拦截器、日志脱敏）。

### 注意事项

1. **Header 信任前提**：用户信息 Header 必须由网关校验 JWT 后注入，业务服务不得直接暴露公网。
2. **CORS 默认关闭**：`allowed-origins` 为空时不注册跨域配置；生产环境禁止 `*` 与 `allow-credentials=true` 组合。
3. **日志脱敏不完整**：正则只覆盖常见敏感字段，不能替代字段级加密与日志平台规则；`max-body-length` 默认 1000。
4. **可覆盖**：所有自动装配 Bean 均带 `@ConditionalOnMissingBean`，业务声明同类型 Bean 即生效。
5. **响应日志固定 UTF-8**：Tomcat 的 `getCharacterEncoding()` 默认 ISO-8859-1，直接使用会导致中文乱码（详见 pitfalls P-006）。

### 升级动作

- demo 移除 `spring-boot-starter-web` 直接依赖，改为依赖 `baseon-starter-web`，`baseon-core` 由其传递引入。

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
