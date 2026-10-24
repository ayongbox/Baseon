package io.github.ayongbox.baseon.starter.redis.support;

import io.github.ayongbox.baseon.starter.redis.config.BaseonRedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisLock} 单测：mock StringRedisTemplate/ValueOperations，
 * 覆盖加锁成功、等待重试、超时失败、compare-and-del 防误删、ThreadLocal 清理。
 */
class RedisLockTest {

    private static final String KEY = "order:lock:1";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private BaseonRedisProperties properties;
    private RedisLock redisLock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        properties = new BaseonRedisProperties();
        redisLock = new RedisLock(redisTemplate, properties);
    }

    @Test
    void tryLockSucceedsWithDefaultWaitAndLease() {
        when(valueOperations.setIfAbsent(eq(KEY), anyString(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        assertThat(redisLock.tryLock(KEY)).isTrue();
        // 默认 lease=30s，value 为随机 token
        verify(valueOperations).setIfAbsent(eq(KEY), anyString(), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    void tryLockRetriesUntilAcquired() {
        // 第一次被占，100ms 后重试成功
        when(valueOperations.setIfAbsent(eq(KEY), anyString(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(false, true);

        assertThat(redisLock.tryLock(KEY, 1, 30)).isTrue();
        verify(valueOperations, times(2)).setIfAbsent(eq(KEY), anyString(), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    void tryLockFailsAfterWaitExpires() {
        // wait=0 语义为只尝试一次，避免单测真实睡眠
        when(valueOperations.setIfAbsent(eq(KEY), anyString(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        assertThat(redisLock.tryLock(KEY, 0, 30)).isFalse();
        verify(valueOperations, times(1)).setIfAbsent(eq(KEY), anyString(), eq(30L), eq(TimeUnit.SECONDS));
        // 加锁失败不应留下持有标记
        assertThat(redisLock.isHeldByCurrentThread(KEY)).isFalse();
    }

    @Test
    void unlockUsesCompareAndDelScriptWithCurrentThreadToken() {
        when(valueOperations.setIfAbsent(eq(KEY), anyString(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        redisLock.tryLock(KEY);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(eq(KEY), tokenCaptor.capture(), eq(30L), eq(TimeUnit.SECONDS));
        String token = tokenCaptor.getValue();

        redisLock.unlock(KEY);

        // 必须用持锁时的同一 token 走 compare-and-del 脚本，保证只删自己的锁
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        ArgumentCaptor<Object> argsCaptor = ArgumentCaptor.forClass(Object.class);
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(Collections.singletonList(KEY)), argsCaptor.capture());
        assertThat(scriptCaptor.getValue().getScriptAsString())
                .contains("redis.call('get'")
                .contains("redis.call('del'");
        assertThat(argsCaptor.getValue()).isEqualTo(token);
    }

    @Test
    void unlockWithMismatchedTokenDoesNotDelete() {
        // 模拟服务端 compare-and-del：value 不匹配返回 0，不删除
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("other-token"))).thenReturn(0L);

        redisLock.unlock(KEY, "other-token");

        ArgumentCaptor<Object> argsCaptor = ArgumentCaptor.forClass(Object.class);
        verify(redisTemplate).execute(any(RedisScript.class), eq(Collections.singletonList(KEY)), argsCaptor.capture());
        assertThat(argsCaptor.getValue()).isEqualTo("other-token");
    }

    @Test
    void unlockCleansUpThreadLocal() {
        when(valueOperations.setIfAbsent(eq(KEY), anyString(), eq(30L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        redisLock.tryLock(KEY);
        // 持锁期间：ThreadLocal token 与 Redis value 一致
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(eq(KEY), tokenCaptor.capture(), eq(30L), eq(TimeUnit.SECONDS));
        when(valueOperations.get(KEY)).thenReturn(tokenCaptor.getValue());
        assertThat(redisLock.isHeldByCurrentThread(KEY)).isTrue();

        redisLock.unlock(KEY);

        // 解锁后 ThreadLocal 已清理：不再访问 Redis 直接返回 false
        assertThat(redisLock.isHeldByCurrentThread(KEY)).isFalse();
        verify(valueOperations, times(1)).get(KEY);
    }

    @Test
    void unlockWithoutHoldingDoesNotTouchRedis() {
        redisLock.unlock(KEY);

        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }
}
