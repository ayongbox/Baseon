# baseon-starter-resilience · 熔断舱壁 + 降级取值兜底

## 定位

下游调用韧性底座。熔断/舱壁直接复用 resilience4j 的 `@CircuitBreaker` / `@Bulkhead` 注解（由 resilience4j-spring-boot3 自动配置提供，本 starter 不重封装），本 starter 自研两件事：方法抛异常后的**取值兜底**（`@Degrade` 三策略）与**熔断状态变更告警**（`AlarmHook` SPI）。

引入方式（版本由 BOM 管控，依赖 baseon-starter-cloud-feign）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-resilience</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `DegradeAspect` | `@Degrade` 切面：方法抛异常时按 CACHE / DEFAULT / FAIL_FAST 策略兜底 | 默认装配 |
| `DegradeResultCache` | CACHE 策略的本地结果缓存（Caffeine，容量上限 + 写入过期），保存各方法最近一次成功返回值 | 默认装配 |
| `LoggingAlarmHook` | `AlarmHook` 默认实现：熔断器打开打 ERROR、半开/恢复打 WARN | 默认装配，业务提供同类型 Bean 即可覆盖接告警渠道 |
| `CircuitBreakerAlarmRegistrar` | 桥接 resilience4j 的 `CircuitBreakerRegistry` 状态转换事件 → `AlarmHook`；注册表不存在时静默跳过 | 默认装配 |

注册方式：`AutoConfiguration.imports`，`baseon.resilience.enabled=false` 时整体不装配。`@CircuitBreaker` / `@Bulkhead` 的注解织入、规则配置（`resilience4j.circuitbreaker.*`）全部走 resilience4j 原生能力，引入本 starter 即开箱可用。

## 配置项

```yaml
baseon:
  resilience:
    enabled: true                 # 总开关：false 时降级切面、结果缓存与熔断告警均不装配
    degrade-cache-max-size: 1000  # 降级结果缓存容量上限（方法 × 参数组合的条目数），超出按 LRU 淘汰
    degrade-cache-ttl-seconds: 300 # 降级结果缓存写入过期秒数，防长期返回陈旧数据

# 熔断/舱壁规则走 resilience4j 原生配置，示例：
resilience4j:
  circuitbreaker:
    instances:
      demoB:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

## 使用场景

### 场景 1：@Degrade 三策略

标注在远程调用/外部依赖方法上（通常是 Feign 调用封装），方法抛异常时按策略兜底：

```java
// FAIL_FAST（默认）：不兜底，直接抛 BizException(9003 服务已降级)，原异常保留为 cause
@Degrade
public OrderDTO getOrder(Long id) { return orderClient.getById(id); }

// CACHE：返回该方法最近一次成功结果（本地缓存），缓存未命中或已过期才抛 9003
@Degrade(strategy = DegradeStrategy.CACHE)
public List<DictDTO> listDict(String type) { return dictClient.list(type); }

// DEFAULT：返回类型默认值——引用类型 null，数值 0，布尔 false
@Degrade(strategy = DegradeStrategy.DEFAULT)
public long countUnread(Long userId) { return msgClient.countUnread(userId); }
```

策略选择口径：CACHE 适合读多写少、可容忍短时不一致的数据；DEFAULT 适合「查不到就当没有」；FAIL_FAST 适合调用方必须感知失败的场景。CACHE 的 key 为「类全名.方法名 + 参数摘要」，同方法不同参数互不污染；方法返回 null 不入缓存。

### 场景 2：与 @CircuitBreaker / @Bulkhead 组合

`@Degrade` 管「失败后取什么值」，resilience4j 注解管「何时切断/隔离」，两者叠加形成完整闭环：

```java
@CircuitBreaker(name = "demoB")              // 失败率超阈值后熔断，调用快速失败
@Bulkhead(name = "demoB", type = Bulkhead.Type.THREADPOOL)  // 舱壁隔离线程池
@Degrade(strategy = DegradeStrategy.CACHE)   // 熔断打开/隔离拒绝后的取值兜底
public String callB(String param) {
    return demoBClient.call(param);
}
```

执行顺序：舱壁拒绝或熔断打开 → 调用快速失败抛异常 → `@Degrade` 切面按策略兜底。熔断器名称与 `resilience4j.circuitbreaker.instances.*` 配置对应。

**切面顺序约定（正确性关键）**：`DegradeAspect` 固定 `@Order(Ordered.LOWEST_PRECEDENCE - 5)`，处于 `CircuitBreakerAspect`（resilience4j-spring6 2.2.x 默认 `LOWEST_PRECEDENCE - 4`，可经 `circuitBreakerAspectOrder` 修改）**外层**——真实异常先被熔断器计数、再被降级兜底；OPEN 后的 `CallNotPermittedException` 同样被兜底返回缓存。若顺序反转，兜底返回值会被熔断器误判为成功，熔断器永远不打开（见 pitfalls P-015）。业务自定义 `circuitBreakerAspectOrder` 时必须保证其大于 `LOWEST_PRECEDENCE - 5`。

### 场景 3：自定义 AlarmHook 接告警渠道

熔断器状态转换（→OPEN / →HALF_OPEN / →CLOSED）默认打 ERROR/WARN 日志。生产接短信/IM 告警渠道时，提供同类型 Bean 覆盖：

```java
@Component
public class ImAlarmHook implements AlarmHook {
    @Override
    public void onStateChange(String name, String fromState, String toState) {
        if ("OPEN".equals(toState)) {
            imClient.sendToOpsGroup("熔断器打开：" + name);
        }
    }
}
```

`@ConditionalOnMissingBean` 退避，自定义 Bean 生效后日志实现不再装配。

## 与 starter-sentinel 的职责分界

| | starter-resilience（本模块） | starter-sentinel |
|---|---|---|
| 防护方向 | **下游调用**（出站：Feign/外部依赖） | **入口流量**（入站：HTTP 接口被调） |
| 底层能力 | resilience4j：熔断 + 舱壁 + 自研降级取值 | Sentinel：流量整形 + 热点参数限流 |
| 降级语义 | 失败后取缓存/默认值/抛 9003 | 触发规则抛 BlockException，统一映射 1005/429 或 9003/503 |
| 规则配置 | `resilience4j.*` 本地配置 | Nacos 持久化，动态生效 |

一句话：Sentinel 守「别人调我」，resilience 守「我调别人」。同一条链路两者可同时存在，互不替代。

## 注意事项

1. **@Degrade 依赖 AOP 代理**：同类自调用不经过切面，降级不生效；标注方法需经 Spring Bean 调用。
2. **CACHE 策略是多实例独立的本地缓存**：各节点各自保存最近成功结果，不保证跨节点一致——选用该策略即接受这一点。
3. **缓存兜底不等于业务正确**：CACHE 返回的是历史成功值，DEFAULT 返回的是类型零值，接口契约上需让调用方知晓数据可能为兜底值（如对时敏数据加时间戳字段）。
4. **FAIL_FAST 抛出的 BizException(9003) 由 starter-web 的全局异常处理器映射为 503**，原始异常保留在 cause 中，排障看 WARN 日志（方法 + 策略 + 原异常 message）。
