# baseon-starter-log · 操作审计（@OperateLog）

## 定位

业务语义级审计底座。`@OperateLog` 注解标注在关键操作入口方法上，切面环绕采集「谁、在哪个模块、做了什么操作、耗时多少、成功还是失败」，组装成 `OperateLogRecord` 交给 `OperateLogHandler` SPI 消费——默认实现打单行日志，业务声明自己的实现（写库 / MQ）即整体替换。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-log</artifactId>
</dependency>
```

## 三者分工：starter-web 请求日志 / starter-trace / 本模块

| 维度 | starter-web 请求日志 | starter-trace | baseon-starter-log |
|---|---|---|---|
| 语义 | 访问流水：每个 HTTP 请求一条 | 全链路关联：traceId 生成与透传 | 业务审计：只记标注 `@OperateLog` 的关键操作 |
| 触发 | 每个请求自动记录 | Filter 入口生成，线程池/异步/定时任务透传 | 注解驱动，按需标注 |
| 内容 | 方法、路径、状态码、耗时、脱敏出入参 | traceId（写 MDC，log pattern `%X{traceId}` 输出） | 模块/操作/操作人/租户/耗时/结果/入参摘要 |
| 面向读者 | 运维排查请求级问题 | 运维串联一次调用链的全部日志 | 审计与安全：谁干了什么 |
| 配置归属 | 请求日志开关/脱敏在 starter-web | 全局 logback 配置在 starter-trace | 本模块只管审计切面，零 logback 配置 |

三者互补不重叠：本模块从 MDC **读取** traceId 做关联（不生成、不管 logback），记录里也**不重复**请求流水已有的路径/状态码信息。

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `OperateLogAspect` | `@OperateLog` 切面：环绕采集审计字段，成功记 success、异常记 failed 后**原样上抛**（绝不吞异常） | 默认装配（Bean 无条件注册，受模块总开关控制） |
| `LoggingOperateLogHandler` | 默认审计处理器：成功 INFO 一行、失败 WARN 一行（含异常简要消息） | `@ConditionalOnMissingBean`，业务声明即被替换 |

注册方式：`AutoConfiguration.imports`，`baseon.log.operate.enabled=false` 时整体不装配，`@OperateLog` 注解不生效（注解在 core，未引入本 starter 时同样不生效）。

## 配置项

```yaml
baseon:
  log:
    operate:
      enabled: true       # 总开关：false 时审计切面不装配
      arg-max-length: 500 # 入参摘要最大长度，deepToString 序列化后超出即截断（追加 ...）
```

## 用法

```java
@OperateLog(module = "user", action = "创建用户")
@PostMapping
public Result<Void> create(@RequestBody @Valid UserCreateReq req) { ... }
// 成功日志：操作审计 | module=user action=创建用户 user=1/admin tenant=1001 trace=xxx
//           method=TenantUserController.create cost=12ms result=success args=[UserCreateReq(...)]

@OperateLog(module = "user", action = "重置口令", recordArgs = false)
@PostMapping("/password/reset")
public Result<Void> resetPassword(@RequestBody @Valid PasswordResetReq req) { ... }
// recordArgs=false：入参摘要不采集（args 为 null），敏感入参（口令/密钥）方法必须显式关闭
```

## 审计记录字段（OperateLogRecord）

不可变记录（final 字段 + 全参构造器），字段与注解/上下文一一对应：

| 字段 | 来源 | 允许为空 |
|---|---|---|
| `module` / `action` | 注解 `module()` / `action()` | 否 |
| `userId` / `username` / `tenantId` | core 的 `SecurityUtil`（无登录上下文时为 null） | 是 |
| `traceId` | `MDC.get("traceId")`（starter-trace 写入；未引入或非请求线程为 null） | 是 |
| `method` | 方法签名：`类简单名.方法名` | 否 |
| `args` | `Arrays.deepToString(入参)` 按 `arg-max-length` 截断；`recordArgs=false` 时为 null | 是 |
| `costMs` | 方法执行耗时（毫秒） | 否 |
| `success` | true 成功 / false 抛异常 | 否 |
| `error` | 异常简要消息：`异常类简单名: message`，整体截断 200；成功时为 null | 是 |

## SPI 替换：写库 / MQ 的自定义处理器

声明一个 `OperateLogHandler` Bean 即整体替换默认日志实现，切面零改动：

```java
@Component
public class DbOperateLogHandler implements OperateLogHandler {

    private final OperateLogMapper mapper;

    @Override
    public void handle(OperateLogRecord record) {
        mapper.insert(toEntity(record));
    }
}
```

`@ConditionalOnMissingBean` 保证容器中始终只有一个 handler：有自定义用自定义，没有则用默认日志实现兜底。

## 注意事项

1. **切面绝不吞异常**：失败路径记完 failed 后原样上抛原异常实例，业务异常语义、全局异常处理（starter-web）不受影响。
2. **handler 在业务调用链上同步执行**：重活（落库、外发 MQ）请在自定义实现内异步化（线程池/MQ 转发），避免拖慢或打断业务主流程；默认日志实现天然轻量。
3. **敏感入参必须关采集**：涉及口令、密钥、证件号的方法显式 `recordArgs = false`，切面不强扫描内容，敏感控制责任在业务标注方。
4. **审计与请求日志可能同时出现**：标注了 `@OperateLog` 的接口会同时产生一条访问流水（starter-web）和一条审计记录——这是设计使然，语义不同、读者不同，不要为去重而关闭任一方。
5. **异步方法注意上下文**：`@Async`/线程池场景需用 core 的 `ContextWrapper` 包装，否则切面读不到操作人与租户（字段为 null），traceId 透传由 starter-trace 的异步装饰器负责。
