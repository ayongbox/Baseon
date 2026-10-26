package io.github.ayongbox.baseon.starter.resilience.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * {@code @Degrade(CACHE)} 策略的本地结果缓存：保存各方法最近一次成功返回值。
 *
 * <p>本地内存缓存，带容量上限与写入过期：容量防内存膨胀，过期防长期返回陈旧数据。
 * 多实例部署时各节点缓存独立，不保证跨节点一致——CACHE 策略本就用于可容忍短时不一致的场景。
 */
public class DegradeResultCache {

    private final Cache<String, Object> cache;

    public DegradeResultCache(long maxSize, long ttlSeconds) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .build();
    }

    /** 未命中或已过期返回 null，由调用方决定后续兜底动作 */
    public Object get(String key) {
        return cache.getIfPresent(key);
    }

    /** null 结果不入缓存：null 多为异常边界返回，缓存它会让降级语义含混 */
    public void put(String key, Object value) {
        if (value != null) {
            cache.put(key, value);
        }
    }
}
