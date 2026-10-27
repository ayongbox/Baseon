# Baseon 脚手架文档

> 本目录是脚手架的**活文档**，与代码同一仓库、同一迭代节奏演进。
> 架构总纲见仓库根目录《通用SpringCloud企业微服务脚手架.md》，本目录只记录「当前已实现什么、怎么用、有什么坑」。

## 目录结构

```
docs/
├── README.md             本文档：导航 + 当前能力矩阵
├── CHANGELOG.md          版本能力台账：每次迭代新增能力与注意事项
├── modules/              模块手册：一个模块一篇，记录用法、配置、边界
└── pitfalls.md           工程踩坑记录：现象 / 根因 / 解决 / 规避
```

## 当前进度

| 期 | 内容 | 状态 |
|---|---|---|
| 0 期 | 根 POM + BOM + demo 验证工程 | ✅ 完成 |
| 1 期 | baseon-core（model / tool / context / spi）+ ArchUnit | ✅ 完成（boot/db/secure/launch 按需后补） |
| 2 期 | starter-web（异常、Jackson、上下文、日志、CORS）+ starter-trace（TraceId、MDC、异步透传） | ✅ 完成 |
| 3 期 | starter-cloud-nacos + starter-cloud-feign + starter-loadbalance（注册发现、上下文透传、灰度路由） | ✅ 完成（`0d7426c`） |
| 4 期 | starter-datasource + starter-mybatis-plus + starter-redis + starter-cache + starter-tenant | ✅ 完成（`4b230e6`，155 个单测全绿） |
| 5 期 | starter-auth / xss / sentinel / resilience / idempotent | ✅ 完成（`05678e0`，208 个单测全绿，鉴权/限流/幂等/熔断闭环已联调验证） |
| 6 期 | 通用工具批：starter-excel / swagger / log / debug-tools | ✅ 完成（255 个单测全绿，prod 硬失效已联调验证）；数据层深水批（S-10/S-11）与基础设施批（S-22~S-25）按需启动 |
| 7 期 | generator 插件 + archetype×2 + demo-gateway + Swagger 聚合 + 全量 ArchUnit/Checkstyle | ✅ 完成（319 个单测全绿，verify 卡点生效，生成器/archetype/网关聚合均已实测） |
| 8 期 | starter-tracing-otel（OTel 分布式追踪接入）+ starter-trace 协同改造 | ✅ 完成（327 个单测全绿，A→B 链路 traceId 四处一致实测通过） |

## 能力矩阵

| 构件 | 能力 | 状态 |
|---|---|---|
| `baseon-parent` | revision 统一版本、flatten 展开、enforcer 构建约束 | ✅ 可用 |
| `baseon-bom` | 依赖版本单一入口（供业务工程 import） | ✅ 可用 |
| `baseon-core` · model | `R<T>`、`PageResult<T>`、`PageQuery`、错误码与 `ErrorType`、`BizException`、实体父类、`LoginUser` | ✅ 可用 |
| `baseon-core` · tool | `StrUtil`、`MoneyUtil`、`BeanCopyUtil` | ✅ 可用 |
| `baseon-core` · context | `SecurityUtil`、`ContextWrapper`、`GrayContext`、`TenantIgnoreContext`（编程式 API） | ✅ 可用 |
| `baseon-core` · cloud | `CloudHeaders`（灰度/链路 Header 常量） | ✅ 可用 |
| `baseon-core` · spi | `CacheKeyPrefixProvider` | ✅ 可用（按需扩展） |
| `baseon-core` · annotation | `@IgnoreTenant`、`@IgnoreTenantCache`、`@RequiresPermission`、`@Idempotent`、`@Degrade`、`@OperateLog` | ✅ 可用 |
| `baseon-core` · secure | `AuthProvider`、`PermissionChecker`、`CryptoService`、`SensitiveType` 契约 | ✅ 可用（boot/db/launch 按需后补） |
| `baseon-starter-web` | 全局异常、Jackson 统一、用户上下文、请求日志脱敏、CORS | ✅ 可用 |
| `baseon-starter-trace` | TraceId、MDC、线程池/异步/定时任务透传、统一 logback 配置 | ✅ 可用 |
| `baseon-starter-cloud-nacos` | 三档注册策略、ConfigHolder 免 @RefreshScope、Jasypt 加密 | ✅ 可用 |
| `baseon-starter-cloud-feign` | traceId/用户上下文/灰度标签自动透传、超时重试 | ✅ 可用 |
| `baseon-starter-loadbalance` | 灰度标签路由（无匹配回退 + WARN）、本地实例过滤 | ✅ 可用 |
| `baseon-starter-datasource` | 动态数据源、`@DS` 显式路由、只读事务自动轮询从库 | ✅ 可用 |
| `baseon-starter-mybatis-plus` | 租户/分页插件链、公共字段填充（不依赖注解）、逻辑删除、慢 SQL 拦截（开关/预热/限流/归因） | ✅ 可用 |
| `baseon-starter-redis` | key 规范校验、`RedisLock` 分布式锁、统一序列化 | ✅ 可用 |
| `baseon-starter-cache` | L1 Caffeine + L2 Redis 两级缓存、三害防护、租户前缀 SPI | ✅ 可用 |
| `baseon-starter-tenant` | 行级 SQL 过滤、缓存租户前缀、`@IgnoreTenant`/`@IgnoreTenantCache`、`TenantOps` 编程式 API | ✅ 可用 |
| `baseon-starter-auth` | JWT 双 Token 签发/刷新/吊销（Redis 黑名单）、`@RequiresPermission` 方法级鉴权、AuthProvider 可整体替换 | ✅ 可用 |
| `baseon-starter-xss` | 表单/请求头/JSON 全链路 HTML 转义、exclude-paths 例外通道 | ✅ 可用 |
| `baseon-starter-sentinel` | BlockException 统一映射（429/1005、503/9003）、租户热点参数限流、规则 Nacos 持久化 | ✅ 可用 |
| `baseon-starter-resilience` | `@Degrade` 三策略兜底（CACHE/DEFAULT/FAIL_FAST）、熔断状态告警 SPI、复用 resilience4j 原生熔断/舱壁 | ✅ 可用 |
| `baseon-starter-idempotent` | `@Idempotent` + Redis 原子占位、key 三级来源（SpEL/请求头/方法级）、租户前缀联动 | ✅ 可用 |
| `baseon-starter-excel` | FastExcel 封装：导出自动响应头、导入逐行容错（行号+原因）、ZIP 魔数嗅探 | ✅ 可用 |
| `baseon-starter-swagger` | OpenAPI3 + Bearer scheme（联动 auth）、生产 profile 双层硬关闭 | ✅ 可用 |
| `baseon-starter-log` | `@OperateLog` 操作审计（操作人/租户/traceId/耗时/成败）、OperateLogHandler SPI | ✅ 可用 |
| `baseon-starter-debug-tools` | `/debug/context`、`/debug/profiles`，生产三层硬失效 | ✅ 可用 |
| `baseon-starter-tracing-otel` | OTel 分布式追踪接入（span 树/OTLP 上报/本地日志档），与 starter-trace 统一 traceId | ✅ 可用 |
| `baseon-generator-maven-plugin` | `mvn baseon:generate -Dtable=` 生成 CRUD 9 件套，core 纯库 + Mojo 薄壳 | ✅ 可用 |
| `baseon-archetype-clean` / `baseon-archetype-sample` | 项目原型：空结构（迁移）/ 全景示例（新建），5 分钟起服务 | ✅ 可用 |
| `samples/demo-gateway` | Spring Cloud Gateway 示例：Nacos 注册、discovery 路由、Swagger 聚合 UI | ✅ 可用（示例，不发布） |
| 全量 ArchUnit | core 包约束 + 19 个 starter「不依赖 samples / 闭包外 starter」规则 | ✅ verify 卡点 |
| Checkstyle | 精简规则集 15 条（仓库根 checkstyle.xml），存量违规已修齐 | ✅ verify 卡点 |

## 文档维护约定

1. **同一次提交内必须同步**：新增/修改能力 → 更新 `CHANGELOG.md` + 对应 `modules/*.md`；
2. **踩坑必记录**：调试中发现的非显而易见问题，沉淀到 `pitfalls.md`，编号递增；
3. **文档只写当前事实**：不写「计划要做的能力」，计划类内容归入 `CHANGELOG` 的「待办」区；
4. **示例可运行**：模块手册中的代码片段应能在 `samples/demo-service` 下直接编译通过。
