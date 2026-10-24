package io.github.ayongbox.baseon.starter.redis.support;

import io.github.ayongbox.baseon.starter.redis.config.BaseonRedisProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 常用操作封装。刻意做成 Bean 而非静态工具类：
 * 便于业务侧 Mock，也允许用户用同名 Bean 覆盖默认实现。
 *
 * <p>所有方法统一先做两件事（见 {@link #completeKey(String)}）：
 * <ol>
 *   <li>拼接全局 key 前缀（{@code baseon.redis.key-prefix}，默认空串）；</li>
 *   <li>按 {@link RedisKeyValidator} 校验完整 key，strict 模式违规抛异常，非 strict 仅 WARN 放行。</li>
 * </ol>
 */
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;
    private final BaseonRedisProperties properties;

    public RedisUtil(StringRedisTemplate redisTemplate, BaseonRedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(completeKey(key));
    }

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(completeKey(key), value);
    }

    public void set(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(completeKey(key), value, ttlSeconds, TimeUnit.SECONDS);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(completeKey(key));
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(completeKey(key));
    }

    public Boolean expire(String key, long ttlSeconds) {
        return redisTemplate.expire(completeKey(key), ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 前缀拼接 + 规范校验，返回最终写入 Redis 的完整 key。
     * 校验放在拼接之后：前缀本身也受同一套规范约束，避免配置出非法前缀绕过检查。
     */
    private String completeKey(String key) {
        String fullKey = properties.getKeyPrefix() + key;
        RedisKeyValidator.validate(fullKey, properties.isStrictKeyCheck());
        return fullKey;
    }
}
