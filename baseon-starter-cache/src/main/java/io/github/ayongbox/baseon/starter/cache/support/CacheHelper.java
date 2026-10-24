package io.github.ayongbox.baseon.starter.cache.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;
import io.github.ayongbox.baseon.starter.cache.config.BaseonCacheProperties;
import io.github.ayongbox.baseon.starter.redis.support.RedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 两级缓存助手：L1 Caffeine（本地）+ L2 Redis（共享）。
 *
 * <p>缓存三害防护策略：
 * <ul>
 *   <li>穿透：loader 返回 null 时在 L2 写短 TTL 空值标记，重复查询不再回源</li>
 *   <li>击穿：回源前抢 Redis 互斥锁，未抢到锁的请求短暂等待后读 L2，仍无则降级直接回源（不写缓存）</li>
 *   <li>雪崩：实际 TTL 在基准值上叠加随机抖动，避免大批 key 同刻过期</li>
 * </ul>
 *
 * <p>L1 不做主动失效广播，靠短 TTL 收敛一致性；对一致性敏感的场景请调 {@link #evict}。
 */
public class CacheHelper {

    private static final Logger log = LoggerFactory.getLogger(CacheHelper.class);

    /** L2 空值标记。业务值统一经 JSON 序列化（String 产物带引号），与该裸串不会冲突 */
    static final String NULL_MARKER = "__BASEON_NULL__";

    /** L1 空值占位对象 */
    private static final Object NULL_HOLDER = new Object();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Cache<String, Object> l1Cache;
    private final RedisLock redisLock;
    private final BaseonCacheProperties properties;
    private final CacheKeyPrefixProvider prefixProvider;

    public CacheHelper(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                       Cache<String, Object> l1Cache, RedisLock redisLock,
                       BaseonCacheProperties properties, CacheKeyPrefixProvider prefixProvider) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.l1Cache = l1Cache;
        this.redisLock = redisLock;
        this.properties = properties;
        this.prefixProvider = prefixProvider;
    }

    /**
     * 读缓存，未命中则回源并回填。key 会自动拼接当前租户前缀（若引入 starter-tenant）。
     *
     * @param key        逻辑 key（不含前缀），如 {@code "user:1001"}
     * @param type       值类型，用于 L2 JSON 反序列化
     * @param ttlSeconds L2 基准有效期（秒），实际值叠加随机抖动
     * @param loader     回源函数，返回 null 会触发空值缓存
     */
    public <T> T getOrLoad(String key, Class<T> type, long ttlSeconds, Supplier<T> loader) {
        String fullKey = prefix() + key;

        Object local = l1Cache.getIfPresent(fullKey);
        if (local != null) {
            return local == NULL_HOLDER ? null : type.cast(local);
        }

        String json = redisTemplate.opsForValue().get(fullKey);
        if (json != null) {
            T value = NULL_MARKER.equals(json) ? null : deserialize(json, type);
            l1Cache.put(fullKey, value == null ? NULL_HOLDER : value);
            return value;
        }

        return loadWithLock(fullKey, type, ttlSeconds, loader);
    }

    /** 双删 L1 + L2 */
    public void evict(String key) {
        String fullKey = prefix() + key;
        l1Cache.invalidate(fullKey);
        redisTemplate.delete(fullKey);
    }

    private <T> T loadWithLock(String fullKey, Class<T> type, long ttlSeconds, Supplier<T> loader) {
        String lockKey = fullKey + ":lock";
        if (redisLock.tryLock(lockKey, properties.getLockWaitSeconds(), properties.getLockLeaseSeconds())) {
            try {
                // double check：等锁期间其他线程可能已回填
                String json = redisTemplate.opsForValue().get(fullKey);
                if (json != null) {
                    T value = NULL_MARKER.equals(json) ? null : deserialize(json, type);
                    l1Cache.put(fullKey, value == null ? NULL_HOLDER : value);
                    return value;
                }
                T loaded = loader.get();
                store(fullKey, loaded, ttlSeconds);
                return loaded;
            } finally {
                redisLock.unlock(lockKey);
            }
        }

        // 未抢到锁：等一拍读 L2，仍无则降级直接回源（不写缓存，避免并发写放大）
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String json = redisTemplate.opsForValue().get(fullKey);
        if (json != null) {
            return NULL_MARKER.equals(json) ? null : deserialize(json, type);
        }
        log.warn("[baseon] 缓存回源未抢到互斥锁，降级直接回源：key={}", fullKey);
        return loader.get();
    }

    private <T> void store(String fullKey, T value, long ttlSeconds) {
        if (value == null) {
            redisTemplate.opsForValue().set(fullKey, NULL_MARKER, properties.getNullTtlSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            l1Cache.put(fullKey, NULL_HOLDER);
            return;
        }
        long ttl = jitteredTtl(ttlSeconds, properties.getTtlJitterRatio());
        redisTemplate.opsForValue().set(fullKey, serialize(value), ttl, java.util.concurrent.TimeUnit.SECONDS);
        l1Cache.put(fullKey, value);
    }

    /**
     * TTL 叠加 ±ratio 随机抖动，包私有便于单测。
     */
    static long jitteredTtl(long baseSeconds, double ratio) {
        double factor = 1 + ThreadLocalRandom.current().nextDouble(-ratio, ratio);
        return Math.max(1, Math.round(baseSeconds * factor));
    }

    private String prefix() {
        return prefixProvider == null ? "" : prefixProvider.prefix();
    }

    private <T> String serialize(T value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("缓存值 JSON 序列化失败", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("缓存值 JSON 反序列化失败: " + json, e);
        }
    }
}
