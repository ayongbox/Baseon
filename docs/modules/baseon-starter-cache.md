# baseon-starter-cache · 两级缓存 + 三害防护

## 定位

业务缓存底座。一个 `CacheHelper` 提供 L1 Caffeine（本地）+ L2 Redis（共享）两级缓存，内置防穿透 / 防击穿 / 防雪崩三害防护，key 前缀通过 SPI 与租户体系联动。

引入方式（版本由 BOM 管控，依赖 starter-redis 提供防击穿互斥锁）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-cache</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `baseonL1Cache` | Caffeine 本地缓存：30s 过期、10000 条上限 | 默认装配，`@ConditionalOnMissingBean(name = "baseonL1Cache")` 可覆盖 |
| `CacheHelper` | 两级缓存读写 + 三害防护 | 默认装配，`baseon.cache.enabled=false` 时不装配 |

注册方式：`AutoConfiguration.imports`。`CacheKeyPrefixProvider` 为可选 SPI：未引入 starter-tenant 时无前缀，引入后自动获得租户隔离前缀（`t<tenantId>:`）。

## 配置项

```yaml
baseon:
  cache:
    enabled: true              # 总开关：false 时 CacheHelper 不装配
    l1-ttl-seconds: 30         # L1 本地缓存有效期，显著短于 L2，容忍短暂不一致换取性能
    l1-max-size: 10000         # L1 最大条目数，防本地内存溢出
    null-ttl-seconds: 60       # 空值缓存有效期，防穿透标记不宜过长，避免数据补录后长期不可见
    ttl-jitter-ratio: 0.1      # TTL 抖动比例：实际 TTL = base × (1 ± ratio)，防雪崩
    lock-wait-seconds: 3       # 回源互斥锁等待时长
    lock-lease-seconds: 10     # 回源互斥锁租期，应大于最慢的一次回源耗时
```

## 使用场景

### 场景 1：读缓存，未命中回源

```java
@Service
public class UserService {

    private final CacheHelper cacheHelper;

    public UserDTO getById(Long id) {
        return cacheHelper.getOrLoad("user:" + id, UserDTO.class, 3600,
                () -> userMapper.selectById(id));
    }
}
```

读取顺序：L1 → L2 → 回源。L2 命中后回填 L1；回源成功同时写 L1 + L2。key 传逻辑 key（如 `user:1001`），前缀由 SPI 自动拼接。

### 场景 2：主动失效

```java
cacheHelper.evict("user:" + id);   // 双删 L1 + L2
```

L1 不做主动失效广播，集群内其他节点的 L1 靠短 TTL 收敛；对一致性敏感的场景在写操作后调 `evict`。

### 场景 3：三害防护（内置，无需编码）

- **防穿透**：loader 返回 null 时在 L2 写短 TTL 空值标记 `__BASEON_NULL__`（默认 60s），重复查询不再回源；业务值统一经 JSON 序列化，与该裸串不会冲突；
- **防击穿**：回源前抢 Redis 互斥锁（key 为 `<fullKey>:lock`），抢到锁的线程回源回填；未抢到锁的请求等 100ms 后重读 L2，仍无则降级直接回源（不写缓存，避免并发写放大）并打 WARN；
- **防雪崩**：实际 TTL 在基准值上叠加 ±10% 随机抖动，避免大批 key 同刻过期。

### 场景 4：租户隔离前缀（SPI 联动）

默认无前缀。引入 starter-tenant 后 `TenantCacheKeyPrefixProvider` 自动生效，缓存 key 拼上 `t<tenantId>:` 前缀，不同租户同 key 互不干扰；无租户上下文或被 `@IgnoreTenantCache` 标记时无前缀（详见 baseon-starter-tenant 手册）。

## 扩展方向：binlog 自动失效

手动 `evict` 的根本弱点是**会漏**：DAO 层有十个写入口，一处忘调就是脏数据。企业级做法是订阅 MySQL binlog，表变更自动触发缓存失效，业务代码零侵入。当前脚手架刻意不做这一层（机制可见性优先），但演进路径如下，供规模化时参考。

### 目标架构

```
业务写 DB（业务代码不调 evict）
   │
   ▼ binlog
Canal / Debezium（解析表名 + 主键 + tenant_id）
   │
   ▼ 消息队列（削峰 + 重试）
缓存失效服务
   ├─ 删 L2：redisTemplate.delete("t1001:user:42")
   └─ Redis pub/sub 广播失效事件 ──▶ 各应用实例清本地 L1
```

### 与现有组件的衔接点

| 环节 | 做法 |
|---|---|
| key 还原 | binlog 里有 `tenant_id` 与主键列，按 `t<tenant_id>:<逻辑前缀>:<id>` 规则拼回完整 key——逻辑前缀需维护"表 → 缓存前缀"映射配置 |
| L2 删除 | 直接复用 starter-redis 的 `redisTemplate` |
| L1 广播 | 失效服务向固定 channel 发消息，各实例监听后调 `baseonL1Cache.invalidate(fullKey)`，取代现在的"短 TTL 收敛" |
| 兜底 | 失效消息丢失时仍靠 TTL 最终一致，现有 TTL + 抖动机制保持不变 |

### 三条铁律仍然适用

1. **失效用删不用更新**：binlog 方案同样是删缓存，并发写写的顺序不可控问题不变；
2. **先写库后删缓存**：binlog 天然满足（写完才产生日志），比手动 evict 更不容易踩时序坑；
3. **TTL 必须兜底**：失效链路再多一层，丢消息的可能性就始终存在。

### 什么时候不需要上这套

单库单表量不大、写入口收敛（统一 Service 层收口）、秒级不一致可接受的系统，手动 evict + 短 TTL 已足够。binlog 链路引入 Canal 集群 + MQ，运维成本本身不低——它是"写入口多到管不住"时的解药，不是默认配置。

## 注意事项

1. **L1 一致性靠短 TTL 收敛**：L1 默认 30s，集群内节点间不做失效广播，写后及时调 `evict`；不能容忍秒级不一致的数据不要走 L1。
2. **空值标记有 TTL**：数据补录后最长 `null-ttl-seconds`（默认 60s）内读到的仍是空，或调 `evict` 立即生效。
3. **回源互斥锁租约要估足**：`lock-lease-seconds` 应大于最慢的一次回源耗时，否则回源未完锁已释放，失去击穿保护。
4. **L2 值走 JSON 序列化**：缓存对象需可被 Jackson 序列化/反序列化，`Class<T>` 用于反序列化，泛型容器类型不适用。
