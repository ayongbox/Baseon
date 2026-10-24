package io.github.ayongbox.baseon.starter.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存配置，前缀 {@code baseon.cache}。
 */
@ConfigurationProperties(prefix = "baseon.cache")
public class BaseonCacheProperties {

    /** 总开关：false 时 CacheHelper 不装配 */
    private boolean enabled = true;

    /** L1 本地缓存（Caffeine）有效期，秒。显著短于 L2，容忍短暂不一致换取性能 */
    private long l1TtlSeconds = 30;

    /** L1 最大条目数，防本地内存溢出 */
    private long l1MaxSize = 10_000;

    /** 空值缓存有效期，秒。防穿透标记不宜过长，避免数据补录后长期不可见 */
    private long nullTtlSeconds = 60;

    /** TTL 抖动比例：实际 TTL = base × (1 ± ratio)，防雪崩 */
    private double ttlJitterRatio = 0.1;

    /** 回源互斥锁等待时长，秒 */
    private long lockWaitSeconds = 3;

    /** 回源互斥锁租期，秒。应大于最慢的一次回源耗时 */
    private long lockLeaseSeconds = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getL1TtlSeconds() {
        return l1TtlSeconds;
    }

    public void setL1TtlSeconds(long l1TtlSeconds) {
        this.l1TtlSeconds = l1TtlSeconds;
    }

    public long getL1MaxSize() {
        return l1MaxSize;
    }

    public void setL1MaxSize(long l1MaxSize) {
        this.l1MaxSize = l1MaxSize;
    }

    public long getNullTtlSeconds() {
        return nullTtlSeconds;
    }

    public void setNullTtlSeconds(long nullTtlSeconds) {
        this.nullTtlSeconds = nullTtlSeconds;
    }

    public double getTtlJitterRatio() {
        return ttlJitterRatio;
    }

    public void setTtlJitterRatio(double ttlJitterRatio) {
        this.ttlJitterRatio = ttlJitterRatio;
    }

    public long getLockWaitSeconds() {
        return lockWaitSeconds;
    }

    public void setLockWaitSeconds(long lockWaitSeconds) {
        this.lockWaitSeconds = lockWaitSeconds;
    }

    public long getLockLeaseSeconds() {
        return lockLeaseSeconds;
    }

    public void setLockLeaseSeconds(long lockLeaseSeconds) {
        this.lockLeaseSeconds = lockLeaseSeconds;
    }
}
