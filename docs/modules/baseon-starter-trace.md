# baseon-starter-trace · 链路追踪

## 定位

所有微服务都必须引入（与 `starter-web` 同级必选）。解决「一次请求在多个服务、多个线程里产生的日志，如何串成一条线」的问题。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-trace</artifactId>
</dependency>
```

## 自动装配清单

| Bean | 作用 | 默认行为 |
|---|---|---|
| `TraceIdFilter` | 请求入口生成/继承 TraceId，写入 MDC，响应头返回 | 默认开启 |
| `BeanPostProcessor` | 给所有 `ThreadPoolTaskExecutor` 注入 `MdcTaskDecorator` | 默认开启 |
| `ScheduledTraceAspect` | 定时任务执行周期内生成独立 TraceId | 默认开启 |

装配入口：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

## 配置项

```yaml
baseon:
  trace:
    enabled: true                    # 总开关
    trace-id-header: X-Trace-Id      # 请求头名称，同时作为 MDC key
    response-header-enabled: true    # 是否在响应头返回 TraceId
    async:
      executor-enabled: true         # 是否自动包装 ThreadPoolTaskExecutor
      annotation-enabled: true       # 是否自动包装 @Async（预留）
      scheduled-enabled: true        # 是否自动包装 @Scheduled
```

## 使用场景

### 场景 1：HTTP 请求自动携带 TraceId

```bash
curl -H "X-Trace-Id: abc123" http://localhost:8080/demo/hello
```

服务会继承 `abc123`，所有日志都带这个 ID：

```
2026-10-22 23:32:48 [nio-8081-exec-1] [abc123] INFO  DemoController - hello called
2026-10-22 23:32:48 [nio-8081-exec-1] [abc123] INFO  RequestLogFilter - http method=GET ...
```

响应头自动返回 `X-Trace-Id: abc123`，前端/网关可据此追踪。

### 场景 2：异步任务透传

```java
@Async
public void sendEmail(String to) {
    // 子线程自动携带父线程的 traceId，无需手动包装
    log.info("send email to {}", to);
}
```

`MdcTaskDecorator` 在任务提交时捕获 MDC，在任务执行时注入，结束后还原。

### 场景 3：定时任务独立 TraceId

```java
@Scheduled(cron = "0 0 2 * * ?")
public void dailyReport() {
    // 每次执行生成新的 32 位 traceId，执行结束自动清理
    log.info("generate daily report");
}
```

定时任务没有 HTTP 入口，MDC 为空；切面为其生成独立 ID，便于排查单次执行。

### 场景 4：手动获取当前 TraceId

```java
String traceId = TraceIdUtil.get();
if (traceId != null) {
    // 写入业务日志、发送 MQ 消息头、调用第三方接口时透传
}
```

## 注意事项

1. **与 starter-web 的分工**：trace 只管 traceId，userId/tenantId 由 web 拦截器负责；两者可独立使用。
2. **MDC 清理必须成对**：`TraceIdFilter` 和 `ScheduledTraceAspect` 都在 finally 中清理，业务代码不得自行 `MDC.clear()`，否则会导致后续日志丢失 traceId。
3. **线程池必须被 Spring 管理**：只有 `ThreadPoolTaskExecutor` Bean 会被自动装饰；手写 `new ThreadPoolExecutor()` 不会生效，需手动 `MdcTaskDecorator` 包装。
4. **@Async 的局限**：Spring 的 `AsyncConfigurer` 不支持 `TaskDecorator`，`@Async` 的 MDC 透传依赖其底层 executor 是 `ThreadPoolTaskExecutor` Bean。
5. **Feign/HTTP 客户端透传**：当前版本未实现，需要额外在 Feign 拦截器或 RestTemplate 拦截器中读取 `TraceIdUtil.get()` 并写入请求头。
6. **MQ 消息透传**：当前版本未实现，需要 Producer 发送时写入消息头、Consumer 消费时写入 MDC。

## 内置统一 logback 配置

模块自带 `logback-spring.xml`（classpath 根），业务**不提供自己的同名文件时自动生效**。
格式：

```
2026-10-23 08:00:55.778  INFO [baseon-demo-service] [nio-8080-exec-1] [de67f3f8697242ff8f08a984110eb1c3] i.g.a.b.s.web.log.RequestLogFilter : http method=GET ...
```

字段顺序：时间 → 级别 → 应用名（`spring.application.name`）→ 线程名 → **traceId** → logger → 消息。
非请求线程（启动、定时任务触发前）traceId 位置为空 `[]`。

### 业务自定义

在业务 `src/main/resources/` 放置自己的 `logback-spring.xml` 即整体覆盖（classes 目录优先于依赖 jar）。
**自定义时必须保留 `%X{traceId:-}` 占位**，否则全链路日志失去关联键。推荐复用 Spring Boot 的官方片段：

```xml
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <property name="CONSOLE_LOG_PATTERN" value="...你自己的 pattern，保留 %X{traceId:-}..."/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### 生产建议

容器化部署只输出 console，由采集 Agent（Filebeat/Fluent Bit）收集，**不要配置文件 appender**；
本地文件滚动策略各异，交给平台统一处理。
