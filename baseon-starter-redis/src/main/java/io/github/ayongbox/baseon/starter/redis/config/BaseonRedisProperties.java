package io.github.ayongbox.baseon.starter.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 增强能力配置，前缀 {@code baseon.redis}。
 *
 * <p>连接参数（地址、密码、连接池等）仍走 Boot 原生 {@code spring.data.redis.*}，
 * 本类只承载脚手架增强能力：全局 key 前缀、key 规范校验策略、分布式锁默认超时。
 */
@ConfigurationProperties(prefix = "baseon.redis")
public class BaseonRedisProperties {

    /**
     * 全局 key 前缀，非空时会拼到所有写入 key 前（含本模块封装的读写与删除），
     * 默认空串表示不启用。典型用法：按环境或应用名隔离，如 {@code order-service:}。
     */
    private String keyPrefix = "";

    /**
     * key 不规范时是否抛 {@link IllegalArgumentException}（快速失败，防止脏 key 扩散）；
     * false 仅打印 WARN 日志放行，用于存量系统平滑接入。默认 true。
     */
    private boolean strictKeyCheck = true;

    /** 分布式锁默认等待秒数：拿不到锁时在该时长内重试，超时返回失败 */
    private long lockWaitSeconds = 3;

    /** 分布式锁默认租约秒数：到期自动释放防死锁，长任务需通过重载方法显式估足 */
    private long lockLeaseSeconds = 30;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public boolean isStrictKeyCheck() {
        return strictKeyCheck;
    }

    public void setStrictKeyCheck(boolean strictKeyCheck) {
        this.strictKeyCheck = strictKeyCheck;
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
