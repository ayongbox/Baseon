# baseon-starter-redis · Redis 工具 + key 规范 + 分布式锁

## 定位

缓存与协调底座。在 Boot 的 RedisAutoConfiguration 之上提供：统一序列化的 `redisTemplate`、带全局前缀与规范校验的 `RedisUtil`、防误删的轻量分布式锁 `RedisLock`。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-redis</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `redisTemplate` | `RedisTemplate<String, Object>`：key/hashKey 用 String 序列化（可读、可命令行排查），value/hashValue 用 GenericJackson2Json 序列化 | `@ConditionalOnMissingBean(name = "redisTemplate")`，Boot 默认退避 |
| `RedisUtil` | 常用操作封装，自动拼全局 key 前缀并做规范校验 | 默认装配，Bean 形式注入 |
| `RedisLock` | 基于 SET NX EX 的轻量分布式锁 | 默认装配，Bean 形式注入 |

注册方式：`AutoConfiguration.imports`。连接工厂与 `StringRedisTemplate` 本体由 Boot 装配，连接参数（地址、密码、连接池等）仍走 `spring.data.redis.*`。

## 配置项

```yaml
baseon:
  redis:
    key-prefix: order-service:      # 全局 key 前缀，拼到所有写入 key 前，默认空串不启用
    strict-key-check: true          # true 违规抛异常（快速失败），false 仅打 WARN 放行
    lock-wait-seconds: 3            # 分布式锁默认等待秒数：拿不到锁时在该时长内重试
    lock-lease-seconds: 30          # 分布式锁默认租约秒数：到期自动释放防死锁
```

## 使用场景

### 场景 1：RedisUtil 读写

```java
@Service
public class TokenService {

    private final RedisUtil redisUtil;

    public TokenService(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    public void saveToken(String userId, String token) {
        redisUtil.set("token:" + userId, token, 7200);   // 实际写入 order-service:token:1001
    }
}
```

提供 `get` / `set` / `set(ttlSeconds)` / `delete` / `hasKey` / `expire`。所有方法先拼接 `key-prefix`，再按 `RedisKeyValidator` 校验完整 key（前缀本身也受同一套规范约束）。刻意做成 Bean 而非静态工具类：便于 Mock，也允许同名 Bean 覆盖。

### 场景 2：key 规范校验

`RedisKeyValidator` 约束：非空白、长度 ≤ 128（拼接前缀后的完整 key 一并受限）、字符仅允许 `[a-zA-Z0-9:_.-]`、禁止连续两个及以上冒号、禁止以冒号开头或结尾。统一约束保证 key 可读、可治理（按冒号分段做命名空间统计与扫描）。

`strict-key-check=true`（默认）违规抛 `IllegalArgumentException` 快速失败；存量系统平滑接入可先设 `false`，违规仅打 WARN 放行。

### 场景 3：分布式锁

```java
if (redisLock.tryLock("order:pay:1001")) {
    try {
        // 临界区
    } finally {
        redisLock.unlock("order:pay:1001");
    }
}

// 长任务显式估足等待与租约
redisLock.tryLock("order:pay:1001", 5, 120);
```

设计要点：

- **防误删**：锁 value 为随机 token，解锁走 Lua compare-and-del 服务端原子执行，只有持锁者本人能删，避免锁超期自动释放后误删他人新加的锁；
- **持有关系用 ThreadLocal 记录**（一线程一把锁的常规用法），`unlock(key)` 释放当前线程持有的锁，解锁后清理 ThreadLocal 防泄漏；跨线程移交场景用 `unlock(key, token)`；
- **无 watchdog 自动续期**，租约到期自动释放防死锁，长任务必须通过 `tryLock(key, waitSeconds, leaseSeconds)` 显式估足租约；
- `isHeldByCurrentThread(key)` 以 ThreadLocal token 与 Redis 实际 value 比对为准，覆盖锁已超期被自动释放的场景。

## 注意事项

1. **无自动续期**：租约到期锁即释放，不要在临界区执行超过租约的任务；估不准就宁大勿小。
2. **未持有锁时 unlock 仅打 WARN**：不会抛异常，但不会删除任何 key，便于排查重复解锁问题。
3. **key-prefix 建议按环境或应用名隔离**（如 `order-service:`），同一 Redis 实例多应用共用时防串 key。
4. **自定义 redisTemplate 与 Boot 默认互斥**：双方条件相同，本自动配置在 imports 中先注册即先生效，Boot 的 `RedisTemplate<Object, Object>` 退避；业务自定义同名 Bean 可整体覆盖。
