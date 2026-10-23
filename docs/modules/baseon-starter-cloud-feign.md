# baseon-starter-cloud-feign · 声明式调用 + 上下文透传

## 定位

需要跨服务调用的微服务引入。解决「调用下游时，traceId / 登录用户 / 灰度标签怎么自动带到下游请求头」的问题，同时收敛 Feign 的超时与重试默认值。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-cloud-feign</artifactId>
</dependency>
```

依赖 `baseon-starter-web` + `baseon-starter-trace`：透传要读 `BaseonWebProperties` 的 Header 名与 `TraceIdUtil`，保证全链路 Header 名一致。

## 自动装配清单

| Bean | 作用 | 默认行为 |
|---|---|---|
| `ContextPropagateRequestInterceptor` | 把 traceId / 登录用户 / 灰度标签写入下游请求头 | 默认开启 |
| Feign 超时默认值 | 写入 `feign.client.config.default.*` | connect 3000ms / read 5000ms |
| `Retryer` | 默认 `NEVER_RETRY` | 不重试，显式配置后才开启 |

## 配置项

```yaml
baseon:
  cloud:
    feign:
      propagate-enabled: true    # 上下文透传总开关
      connect-timeout: 3000      # 连接超时 ms
      read-timeout: 5000         # 读超时 ms
      retry:
        enabled: false           # 默认不重试；开启前确认下游接口幂等
        period: 100              # 初始重试间隔 ms
        max-period: 1000         # 最大重试间隔 ms
        max-attempts: 2          # 最大尝试次数（含首次）
```

## 透传的请求头

| Header | 来源 | 说明 |
|---|---|---|
| `X-Trace-Id` | `TraceIdUtil.get()` | Header 名由 `baseon.trace.trace-id-header` 控制 |
| `X-User-Id` / `X-Tenant-Id` / `X-Username` | `SecurityUtil.get()` | Header 名由 `baseon.web.user-context.*` 控制 |
| `X-Gray-Tag` | `GrayContext.get()` | 名称固定于 `CloudHeaders`，不可配置 |

**不覆盖原则**：调用方已显式设置的同名请求头不会被拦截器覆盖。

## 使用场景

```java
@FeignClient("demo-service-b")
public interface DemoBClient {

    @GetMapping("/demo/ping")
    R<Map<String, Object>> ping();
}
```

当前请求的 traceId、登录用户、灰度标签自动带入下游，下游服务的 starter-web / starter-trace 会从请求头恢复上下文，两服务日志 traceId 一致。

## 注意事项

1. **默认不重试**：超时即失败。开启 `retry.enabled=true` 前必须确认下游接口幂等，否则可能重复下单。
2. **超时是全局默认值**：写入 `feign.client.config.default.*`，业务对单个 client 的显式配置优先。
3. **网关信任前提同 starter-web**：用户信息 Header 由网关注入，本模块只负责在服务间继续传递。
