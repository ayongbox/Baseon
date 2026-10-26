# baseon-starter-sentinel · 限流熔断统一异常 + 租户维度热点限流

## 定位

流量防护底座。基于 SCA 原生 Sentinel（规则 Nacos 持久化走 `spring.cloud.sentinel.datasource.*`，本模块不重复造轮子），在其上补两件脚手架能力：`BlockException` 统一异常映射、租户维度热点参数限流门面。

引入方式（版本由 BOM 管控，依赖 baseon-starter-web）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-sentinel</artifactId>
</dependency>
```

## 自动装配清单

| 组件                              | 作用                                                            | 默认行为                                                  |
| ------------------------------- | ------------------------------------------------------------- | ----------------------------------------------------- |
| `SentinelBlockExceptionHandler` | `@RestControllerAdvice`：`BlockException` 分型映射为统一响应体           | 默认装配，`baseon.sentinel.block-handler-enabled=false` 可关 |
| `TenantHotParamGuard`           | 租户维度热点参数限流门面：`acquire(resource)` 以当前租户 ID 为热点参数进入 Sentinel 资源 | 默认装配                                                  |

注册方式：`AutoConfiguration.imports`，`baseon.sentinel.enabled=false` 时整体不装配（Sentinel 本体不受本开关影响）。异常分型规则：

| 异常类型                                                                                                              | HTTP 状态码 | 错误码         |
| ----------------------------------------------------------------------------------------------------------------- | -------- | ----------- |
| `FlowException` / `ParamFlowException` / `SystemBlockException` / `AuthorityException`（含未知 `BlockException` 子类兜底） | 429      | 1005 请求过于频繁 |
| `DegradeException`                                                                                                | 503      | 9003 服务已降级  |

`BlockException` 不是 `BizException`，starter-web 的 `GlobalExceptionHandler` 不会接管，必须由本 Advice 处理。限流熔断属预期内的高并发保护行为，只记 WARN 简版日志、不打印堆栈，响应体不暴露规则详情。

## 配置项

```yaml
baseon:
  sentinel:
    enabled: true                # 总开关：false 时本模块增强能力均不装配
    block-handler-enabled: true  # BlockException 统一异常处理开关
```

Sentinel 本体（transport 端口、dashboard 地址、懒加载等）仍走 SCA 原生 `spring.cloud.sentinel.*`，不在本配置类重复声明。

## Nacos 规则持久化

规则持久化不写代码，走 SCA 原生 datasource 配置。dataId 命名约定：`{spring.application.name}-sentinel-{规则类型}-rules`，groupId 统一 `SENTINEL_GROUP`：

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        flow:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr}
            dataId: ${spring.application.name}-sentinel-flow-rules
            groupId: SENTINEL_GROUP
            data-type: json
            rule-type: flow
        degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr}
            dataId: ${spring.application.name}-sentinel-degrade-rules
            groupId: SENTINEL_GROUP
            data-type: json
            rule-type: degrade
        param-flow:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr}
            dataId: ${spring.application.name}-sentinel-param-flow-rules
            groupId: SENTINEL_GROUP
            data-type: json
            rule-type: param-flow
```

Nacos 上发布流控规则示例（改规则不重启即生效）：

```json
[
  {
    "resource": "/demo/sentinel/qps",
    "limitApp": "default",
    "grade": 1,
    "count": 2,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

## 租户维度热点限流

普通流控规则管资源总量，`TenantHotParamGuard` 以当前租户 ID 为热点参数进入资源，配合热点参数规则实现「按租户独立计数」——单租户突发流量只限自己，不拖垮其他租户：

```java
@GetMapping("/tenant-qps")
public R<String> tenantQps() throws BlockException {
    tenantHotParamGuard.acquire("demo:tenant-qps");   // 热点参数 = SecurityUtil.getTenantId()
    return R.ok("ok");
}
```

配套热点参数规则（`paramIdx` 固定为 0，即 acquire 传入的租户 ID）：

```json
[
  {
    "resource": "demo:tenant-qps",
    "limitApp": "default",
    "grade": 1,
    "paramIdx": 0,
    "count": 5,
    "durationInSec": 1
  }
]
```

无租户上下文（登录前接口、系统任务）时以约定值 `-1` 兜底，与 starter-tenant 口径一致：匿名流量共用一个独立配额，不挤占任何真实租户。触发限流时抛 `ParamFlowException`，由 `SentinelBlockExceptionHandler` 统一映射 429 + 1005，业务无需 catch。

## 与 starter-resilience 的职责分界

| <br /> | starter-sentinel                | starter-resilience                                                |
| ------ | ------------------------------- | ----------------------------------------------------------------- |
| 管什么    | 流量防护与熔断**规则**：限流、系统保护、热点参数、熔断触发 | 调用韧性与**取值兜底**：`@Degrade` 降级策略（CACHE/DEFAULT/FAIL\_FAST）、舱壁隔离、熔断告警 |
| 触发位置   | 入口流量（Controller 资源、热点参数）        | 出向调用（Feign/远程方法）                                                  |
| 失败表现   | 429/1005、503/9003 统一响应          | 缓存值、默认值或 BizException(9003)                                       |

一句话：Sentinel 决定「这个请求该不该放行」，resilience 决定「放行之后调用失败拿什么兜」。两者可同链路叠加，互不替代。

## 注意事项

1. **热点参数索引固定为 0**：`TenantHotParamGuard.acquire` 只传一个热点参数（租户 ID），配置 `ParamFlowRule` 时 `paramIdx` 必须为 0，否则规则不命中。
2. **规则不配置即不限流**：未发布对应 dataId 时 `acquire` 永远放行，Sentinel 对无规则资源零干预。
3. **网关层限流不在本模块范围**：本模块管应用内 Sentinel；如引入 Spring Cloud Gateway 限流需另接 `sentinel-spring-cloud-gateway-adapter`。
4. **block-handler 只覆盖 Web 层**：`@RestControllerAdvice` 只在 MVC 请求链生效；定时任务、MQ 消费里触发的 `BlockException` 需业务自行处理。

