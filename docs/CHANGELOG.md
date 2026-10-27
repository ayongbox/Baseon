# 迭代能力台账

> 记录每个迭代周期「交付了什么能力、改变了什么、需要业务方注意什么」。
> 版本号统一使用 `${revision}`，发布时由 Maven 注入；开发期按迭代期记录。

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
