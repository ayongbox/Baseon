# baseon-starter-tracing-otel · OpenTelemetry 分布式追踪接入

## 定位

分布式追踪的一站式接入：聚合 micrometer-tracing OTel 桥接 + OTLP 上报 + Feign 观测能力，引入后 Spring Boot 自动完成 OTel SDK、web server span、Feign client span 与 W3C `traceparent` 传播的装配，并与 starter-trace 协同——日志、响应头、追踪后端、下游服务四处 traceId 一致。

与 starter-trace 的分工：starter-trace 是「日志关联」（traceId + MDC，零外部依赖，排障够用）；本模块是「全链路分布式追踪」（span 树 + 逐跳耗时 + 采样上报，回答「哪一跳慢」）。两者不互斥，本模块依赖 starter-trace 并自动复用其 MDC 约定。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-tracing-otel</artifactId>
</dependency>
```

## 聚合的依赖与各自职责

| 依赖 | 职责 |
|---|---|
| `spring-boot-starter-actuator` | 带入 micrometer-observation，web 观测过滤器（`ServerHttpObservationFilter`）的前提 |
| `micrometer-tracing-bridge-otel` | Micrometer Tracing 门面的 OTel 实现：Tracer/Span 落到 OTel SDK |
| `opentelemetry-exporter-otlp` | 生产上报通道：span 经 OTLP 发往 collector（Tempo/Jaeger/云厂商均可） |
| `opentelemetry-exporter-logging` | 本地开发档载体：span 输出到日志，无需部署 collector |
| `feign-micrometer` | Feign 观测能力包：spring-cloud-openfeign 检测到后自动创建 client span 并注入 W3C `traceparent`（缺失会导致下游另起 trace，链路断裂，见 P-022） |
| `baseon-starter-trace` | MDC/traceId 约定协同；Tracer 在场时其过滤器自动复用全链路 traceId |

micrometer-tracing / opentelemetry 全家版本由父 POM import 的 spring-boot-dependencies 统一管理；feign-micrometer 版本由 spring-cloud BOM 经 feign-bom 管理，均无需手写。

## 配置项

```yaml
baseon:
  tracing:
    otel:
      enabled: true                # 本模块定制 Bean 总开关
      log-exporter-enabled: false  # 本地开发档：span 输出到日志

management:
  tracing:
    enabled: true                  # Boot 原生总开关（false 则 Tracer 不装配，starter-trace 回退自有 ID）
    sampling:
      probability: 0.1             # Boot 默认 10% 采样；生产按流量调，本地演示可 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4318  # OTLP collector 地址
      enabled: true                # 本地无 collector 时设 false 关掉重试噪音
```

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `baseon.tracing.otel.enabled` | boolean | `true` | 只管本模块定制 Bean，不管 Boot 的 OTel 官方装配 |
| `baseon.tracing.otel.log-exporter-enabled` | boolean | `false` | 本地档：注册 `SimpleSpanProcessor(LoggingSpanExporter)`，span 结束立即打日志。仅本地用，生产关闭 |
| `management.tracing.sampling.probability` | double | `0.1` | 采样率。1.0 全采仅适合本地/低流量 |
| `management.otlp.tracing.endpoint` | String | `http://localhost:4318` | OTLP HTTP 端点（gRPC 4317 需另配） |

## 快速上手（本地零基础设施验证）

参考 samples/demo-service 的配置：

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      enabled: false        # 本地无 collector，关掉 OTLP 重试噪音
baseon:
  tracing:
    otel:
      log-exporter-enabled: true
```

启动后任意请求，控制台可见两类输出且 traceId 一致：

```
INFO [demo] [nio-8091-exec-2] [f1d813b0b62949cd0b1a47217d22e906] RequestLogFilter : http method=GET uri=/demo/call-b ...
INFO [demo] [nio-8091-exec-2] [] LoggingSpanExporter : 'http get /demo/call-b' : f1d813b0b62949cd0b1a47217d22e906 1bbe7579463e3f5e SERVER ...
INFO [demo] [nio-8091-exec-2] [f1d813...] LoggingSpanExporter : 'HTTP GET' : f1d813b0b62949cd0b1a47217d22e906 b07bdb934d150d9a CLIENT ...clientName=DemoBClient
```

跨服务调用时下游服务的 server span 与上游同 traceId（W3C `traceparent` 续链）。

## 生产接入

1. 关本地档：`log-exporter-enabled: false`（或不配）。
2. 开 OTLP 并指向 collector：`management.otlp.tracing.endpoint: http://otel-collector:4318`。
3. 采样率按流量定：高流量服务 0.01~0.1，关键链路可配合 OTel 的 parent-based 规则。
4. 后端任选：OTLP 是厂商中立协议，Tempo / Jaeger / 阿里云 SLS / SkyWalking（经 converter）均可接收。

## 注意事项

1. **actuator 随之引入**：本模块带入 spring-boot-starter-actuator，默认仅暴露 `/actuator/health`；接入 starter-auth 的服务该端点会被认证拦截（401），如需放行加进 `baseon.auth.ignore-paths`。
2. **本地档与 OTLP 可并存**：两者都是 `SpanProcessor`/exporter，Boot 的 `OpenTelemetryAutoConfiguration` 收集所有 SpanProcessor Bean 加入 SDK；本地档开着又忘了关 OTLP 时，无 collector 环境会有连接重试日志噪音，用 `management.otlp.tracing.enabled=false` 关掉。
3. **SpanExporter 日志行的 traceId 字段为空**：LoggingSpanExporter 回调发生在 observation scope 关闭之后，MDC 已恢复，属正常现象，span 内容里的 traceId 才是准的。
4. **自定义 Filter 不要指望全局顺序**：starter-trace 的复用依赖 `ServerHttpObservationFilter`（order `HIGHEST_PRECEDENCE + 1`）先执行；业务若注册更高优先级的自定义 Filter 并在其中读 `TraceIdUtil.get()`，可能早于 span 创建而拿到 null。
5. **MQ / 定时任务无自动 span**：web 与 Feign 由 Boot 自动观测，其余入口需自行用 `@Observed` 或 Tracer API 埋点；定时任务的 traceId 仍走 starter-trace 的 ScheduledTraceAspect（自有 ID），不进追踪后端。
