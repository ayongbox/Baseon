package io.github.ayongbox.baseon.starter.resilience.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 调用韧性配置，前缀 {@code baseon.resilience}。
 *
 * <p>本类只承载脚手架自研的降级兜底配置；熔断/舱壁规则（滑窗大小、失败阈值等）
 * 走 resilience4j 原生 {@code resilience4j.circuitbreaker.*} / {@code resilience4j.bulkhead.*} 配置。
 */
@ConfigurationProperties(prefix = "baseon.resilience")
public class BaseonResilienceProperties {

    /** 总开关：false 时降级切面、结果缓存与熔断告警均不装配 */
    private boolean enabled = true;

    /** 降级结果缓存容量上限（方法 × 参数组合的条目数），超出按 LRU 淘汰 */
    private long degradeCacheMaxSize = 1000;

    /** 降级结果缓存写入过期秒数：过期后缓存视为未命中，防长期返回陈旧数据 */
    private long degradeCacheTtlSeconds = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDegradeCacheMaxSize() {
        return degradeCacheMaxSize;
    }

    public void setDegradeCacheMaxSize(long degradeCacheMaxSize) {
        this.degradeCacheMaxSize = degradeCacheMaxSize;
    }

    public long getDegradeCacheTtlSeconds() {
        return degradeCacheTtlSeconds;
    }

    public void setDegradeCacheTtlSeconds(long degradeCacheTtlSeconds) {
        this.degradeCacheTtlSeconds = degradeCacheTtlSeconds;
    }
}
