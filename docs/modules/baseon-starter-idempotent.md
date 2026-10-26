# baseon-starter-idempotent · 幂等控制（Redis 原子占位）

## 定位

防重复提交能力底座。`@Idempotent` 注解标注在 Controller 方法上，切面用 Redis `SET NX EX` 原子占位：占位窗口内重复请求抛「1006 重复请求」，窗口过期自动放行，无后台清理任务。

引入方式（版本由 BOM 管控，依赖 baseon-starter-redis 与 baseon-starter-web）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-idempotent</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `IdempotentAspect` | `@Idempotent` 切面：组装幂等 key → `RedisUtil.setIfAbsent` 原子占位 → 失败抛 `BizException(1006)` | 默认装配 |

注册方式：`AutoConfiguration.imports`，`baseon.idempotent.enabled=false` 时整体不装配，`@Idempotent` 注解不生效（注解在 core，未引入本 starter 时同样不生效）。

## 配置项

```yaml
baseon:
  idempotent:
    enabled: true                        # 总开关：false 时幂等切面不装配
    default-ttl-seconds: 10              # 默认占位窗口（秒），注解未指定 ttlSeconds 时使用
    token-header: X-Idempotent-Token     # 注解未配置 key 表达式时，从该请求头读幂等令牌
```

## 幂等 key 的三种来源

最终 key 形如 `idem:[租户前缀]类简单名.方法名:求值结果`，求值结果按三级来源取：

### 来源 1：注解 SpEL 表达式（推荐）

`key()` 基于方法参数求值（SpEL，参数名绑定，父 POM 已开 `-parameters` 编译参数）：

```java
@Idempotent(key = "#name")
public Result<Void> create(@RequestParam String name) { ... }
// name=orderA 时 key = idem:DemoController.create:orderA

@Idempotent(key = "#req.orderNo", ttlSeconds = 30)
public Result<Void> submit(@RequestBody OrderReq req) { ... }
// key = idem:OrderController.submit:NO-123，占位窗口 30 秒
```

同一业务标识在窗口内重复提交即被拦截——适合「创建订单」这类有天然业务键的接口。

### 来源 2：请求头幂等令牌

注解不配 `key` 时，从 `token-header` 配置的请求头读令牌：

```java
@Idempotent
public Result<Void> pay() { ... }
// 请求头 X-Idempotent-Token: tok123 → key = idem:PayController.pay:tok123
```

适合「前端先调令牌接口拿一次性 token，再提交表单」的通用防重方案；非 Web 场景（定时任务、消息消费）无请求上下文，自动落到来源 3。

### 来源 3：方法级幂等（兜底退化）

SpEL 与请求头都缺省时，退化为**方法级幂等**：key 不含后缀（`idem:PayController.pay`），同一方法在窗口内的所有调用整体互斥。语义较重，只应在明确知道后果时使用——典型如「同一时刻只允许一个结算任务执行」的入口。

## 与 starter-tenant 的联动

key 组装时通过 `ObjectProvider<CacheKeyPrefixProvider>` 可选注入前缀 SPI：引入 starter-tenant 后其 `TenantCacheKeyPrefixProvider` 生效，幂等 key 自动变为 `idem:t1001:...`，同一业务键在不同租户下落到不同物理 key，跨租户互不拦截；未引入 starter-tenant 则无前缀。`@IgnoreTenantCache` 标记的方法同样不加租户前缀（SPI 内部判断，幂等切面零感知）。

两级能力解耦：本 starter 不依赖 starter-tenant，联动由 core 的 SPI 契约完成。

## 注意事项

1. **集群部署依赖共享 Redis**：占位状态存 Redis，多实例必须连同一 Redis（或同一逻辑库），否则同一请求打到不同实例会绕过拦截。本地缓存实现（Caffeine 等）不满足集群语义，不要替换。
2. **占位成功不删 key**：执行失败也占用窗口——失败重试与恶意重复提交无法区分，统一按「窗口内只放行一次」处理。需要「失败立即放行重试」语义的接口不要标本注解。
3. **TTL 即去重窗口**：默认 10 秒，超过窗口的重复提交会被放行。窗口应大于客户端超时重试周期，而不是越长越好（过长会误伤正常变更）。
4. **key 受 RedisKeyValidator 约束**：求值结果拼进 key 后走统一校验（长度 ≤128、字符集 `字母数字 : _ - .`、禁止连续冒号）。业务值含非法字符时 strict 模式直接抛异常，设计 SpEL 表达式时注意取「标识类」字段而非自由文本。
