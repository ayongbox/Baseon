package io.github.ayongbox.baseon.starter.redis.support;

import io.github.ayongbox.baseon.starter.redis.config.BaseonRedisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 SET NX EX 的轻量分布式锁。
 *
 * <p>设计要点：
 * <ul>
 *   <li>防误删：锁 value 为随机 token，解锁走 Lua compare-and-del，
 *       只有持锁者本人能删，避免锁超期自动释放后误删他人新加的锁；</li>
 *   <li>持有关系用 {@link ThreadLocal} 记录（一线程一把锁的常规用法），
 *       {@link #unlock(String)} 释放当前线程持有的锁，解锁后清理 ThreadLocal 防泄漏；</li>
 *   <li>无 watchdog 自动续期，租约到期自动释放防死锁，
 *       长任务需通过 {@link #tryLock(String, long, long)} 显式估足租约。</li>
 * </ul>
 */
public class RedisLock {

    private static final Logger log = LoggerFactory.getLogger(RedisLock.class);

    /** compare-and-del：value 匹配才删除并返回 1，否则返回 0；脚本在服务端原子执行 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    /** 等待拿锁时的重试间隔 */
    private static final long RETRY_INTERVAL_MILLIS = 100L;

    private final StringRedisTemplate redisTemplate;
    private final BaseonRedisProperties properties;

    /** 当前线程持有的锁 token，tryLock 成功时写入，unlock 后清理 */
    private final ThreadLocal<String> heldToken = new ThreadLocal<>();

    public RedisLock(StringRedisTemplate redisTemplate, BaseonRedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 用默认等待/租约秒数（{@code baseon.redis.lock-wait-seconds} / {@code lock-lease-seconds}）尝试加锁。
     */
    public boolean tryLock(String key) {
        return tryLock(key, properties.getLockWaitSeconds(), properties.getLockLeaseSeconds());
    }

    /**
     * 尝试加锁：立即尝试一次，失败后在 waitSeconds 内每 100ms 重试，超时返回 false。
     * 实现为 {@code SET key token NX EX leaseSeconds}，原子完成"不存在才写入 + 过期时间"。
     *
     * @param waitSeconds  最长等待秒数，0 表示只尝试一次
     * @param leaseSeconds 锁租约秒数，到期自动释放
     */
    public boolean tryLock(String key, long waitSeconds, long leaseSeconds) {
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitSeconds * 1000L;
        while (true) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, token, leaseSeconds, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(acquired)) {
                heldToken.set(token);
                return true;
            }
            // 下一次重试会越过等待截止时间则直接失败，保证总耗时不超 waitSeconds
            if (System.currentTimeMillis() + RETRY_INTERVAL_MILLIS > deadline) {
                return false;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * 显式指定 token 解锁（跨线程移交等场景）。compare-and-del 保证只删自己的锁，
     * token 不匹配时服务端返回 0、不删除。
     */
    public void unlock(String key, String token) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
    }

    /**
     * 释放当前线程持有的锁：取 ThreadLocal 中的 token 走 compare-and-del，
     * 无论解锁结果如何都清理 ThreadLocal，防止线程复用时残留旧 token。
     */
    public void unlock(String key) {
        String token = heldToken.get();
        try {
            if (token == null) {
                log.warn("[baseon-redis] 当前线程未持有锁，忽略解锁：{}", key);
                return;
            }
            unlock(key, token);
        } finally {
            heldToken.remove();
        }
    }

    /**
     * 当前线程是否持有该 key 的锁：以 ThreadLocal 中的 token 与 Redis 实际 value 比对为准，
     * 覆盖锁已超期被自动释放的场景。
     */
    public boolean isHeldByCurrentThread(String key) {
        String token = heldToken.get();
        if (token == null) {
            return false;
        }
        return token.equals(redisTemplate.opsForValue().get(key));
    }
}
