package io.github.ayongbox.baseon.starter.redis.support;

import io.github.ayongbox.baseon.starter.redis.config.BaseonRedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisUtil} 单测：重点验证"全局前缀拼接 + key 规范校验"两个横切行为。
 */
class RedisUtilTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private BaseonRedisProperties properties;
    private RedisUtil redisUtil;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        properties = new BaseonRedisProperties();
        properties.setKeyPrefix("demo:");
        redisUtil = new RedisUtil(redisTemplate, properties);
    }

    @Test
    void getConcatenatesKeyPrefix() {
        when(valueOperations.get("demo:order:1")).thenReturn("v1");

        assertThat(redisUtil.get("order:1")).isEqualTo("v1");
    }

    @Test
    void setWithTtlConcatenatesKeyPrefix() {
        redisUtil.set("order:1", "v1", 60);

        verify(valueOperations).set("demo:order:1", "v1", 60L, TimeUnit.SECONDS);
    }

    @Test
    void deleteHasKeyExpireConcatenateKeyPrefix() {
        when(redisTemplate.delete("demo:order:1")).thenReturn(true);
        when(redisTemplate.hasKey("demo:order:1")).thenReturn(true);
        when(redisTemplate.expire("demo:order:1", 60L, TimeUnit.SECONDS)).thenReturn(true);

        assertThat(redisUtil.delete("order:1")).isTrue();
        assertThat(redisUtil.hasKey("order:1")).isTrue();
        assertThat(redisUtil.expire("order:1", 60)).isTrue();
    }

    @Test
    void invalidKeyRejectedInStrictMode() {
        // 默认 strictKeyCheck=true：含空格的 key 直接抛异常，且不触及 Redis
        assertThatThrownBy(() -> redisUtil.get("bad key"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redisUtil.set("bad key", "v"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(valueOperations, never()).get(anyString());
        verify(valueOperations, never()).set(anyString(), anyString());
    }

    @Test
    void invalidKeyWarnedAndPassedInNonStrictMode() {
        properties.setStrictKeyCheck(false);

        // 非 strict 模式仅 WARN 放行，完整 key 原样写入
        assertThatCode(() -> redisUtil.set("bad key", "v")).doesNotThrowAnyException();
        verify(valueOperations).set("demo:bad key", "v");
    }

    @Test
    void emptyPrefixLeavesKeyUntouched() {
        properties.setKeyPrefix("");
        when(valueOperations.get("order:1")).thenReturn("v1");

        assertThat(redisUtil.get("order:1")).isEqualTo("v1");
    }
}
