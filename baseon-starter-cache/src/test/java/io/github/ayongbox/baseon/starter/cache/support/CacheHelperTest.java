package io.github.ayongbox.baseon.starter.cache.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;
import io.github.ayongbox.baseon.starter.cache.config.BaseonCacheProperties;
import io.github.ayongbox.baseon.starter.redis.support.RedisLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheHelperTest {

    /** 用内存 Map 模拟 Redis 行为，避免引入真实 Redis */
    private final Map<String, String> store = new HashMap<>();
    private StringRedisTemplate redisTemplate;
    private RedisLock redisLock;
    private BaseonCacheProperties properties;
    private com.github.benmanes.caffeine.cache.Cache<String, Object> l1;

    @BeforeEach
    void setUp() {
        store.clear();
        redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenAnswer(inv -> store.get(inv.getArgument(0)));
        doAnswer(inv -> {
            store.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(ops).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(redisTemplate.delete(anyString())).thenAnswer(inv -> store.remove(inv.getArgument(0)) != null);

        redisLock = mock(RedisLock.class);
        when(redisLock.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);

        properties = new BaseonCacheProperties();
        l1 = Caffeine.newBuilder().build();
    }

    private CacheHelper helper(CacheKeyPrefixProvider prefixProvider) {
        return new CacheHelper(redisTemplate, new ObjectMapper(), l1, redisLock, properties, prefixProvider);
    }

    @Test
    void loadsAndBackfillsThenHitsL1() {
        CacheHelper helper = helper(null);
        AtomicInteger loadCount = new AtomicInteger();

        String first = helper.getOrLoad("user:1", String.class, 100, () -> {
            loadCount.incrementAndGet();
            return "zhangsan";
        });
        String second = helper.getOrLoad("user:1", String.class, 100, () -> {
            loadCount.incrementAndGet();
            return "zhangsan";
        });

        assertThat(first).isEqualTo("zhangsan");
        assertThat(second).isEqualTo("zhangsan");
        assertThat(loadCount.get()).isEqualTo(1);
    }

    @Test
    void nullLoaderWritesNullMarkerAgainstPenetration() {
        CacheHelper helper = helper(null);
        AtomicInteger loadCount = new AtomicInteger();

        String result = helper.getOrLoad("user:404", String.class, 100, () -> {
            loadCount.incrementAndGet();
            return null;
        });
        helper.getOrLoad("user:404", String.class, 100, () -> {
            loadCount.incrementAndGet();
            return null;
        });

        assertThat(result).isNull();
        assertThat(loadCount.get()).isEqualTo(1);
        assertThat(store.get("user:404")).isEqualTo(CacheHelper.NULL_MARKER);
    }

    @Test
    void l1MissHitsL2AndRepopulatesL1() {
        CacheHelper helper = helper(null);
        store.put("user:2", "\"lisi\"");

        String value = helper.getOrLoad("user:2", String.class, 100, () -> {
            throw new IllegalStateException("不应回源");
        });

        assertThat(value).isEqualTo("lisi");
        assertThat(l1.getIfPresent("user:2")).isEqualTo("lisi");
    }

    @Test
    void prefixProviderPrependsToKey() {
        CacheHelper helper = helper(() -> "t1001:");
        helper.getOrLoad("user:1", String.class, 100, () -> "a");

        assertThat(store.containsKey("t1001:user:1")).isTrue();
    }

    @Test
    void lockMissDegradesToDirectLoad() {
        when(redisLock.tryLock(anyString(), anyLong(), anyLong())).thenReturn(false);
        CacheHelper helper = helper(null);
        AtomicInteger loadCount = new AtomicInteger();

        String value = helper.getOrLoad("hot:key", String.class, 100, () -> {
            loadCount.incrementAndGet();
            return "hot-value";
        });

        assertThat(value).isEqualTo("hot-value");
        assertThat(loadCount.get()).isEqualTo(1);
        assertThat(store.containsKey("hot:key")).isFalse();
    }

    @Test
    void ttlJitterWithinBounds() {
        for (int i = 0; i < 200; i++) {
            long ttl = CacheHelper.jitteredTtl(100, 0.1);
            assertThat(ttl).isBetween(90L, 110L);
        }
    }

    @Test
    void evictClearsBothLevels() {
        CacheHelper helper = helper(null);
        helper.getOrLoad("user:1", String.class, 100, () -> "a");
        helper.evict("user:1");

        assertThat(store.containsKey("user:1")).isFalse();
        assertThat(l1.getIfPresent("user:1")).isNull();
    }
}
