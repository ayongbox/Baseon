package io.github.ayongbox.baseon.starter.cloud.feign.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feign 调用治理配置，前缀 {@code baseon.cloud.feign}。
 */
@ConfigurationProperties(prefix = "baseon.cloud.feign")
public class BaseonCloudFeignProperties {

    /** 是否透传 traceId / 用户上下文 / 灰度标签到下游 */
    private boolean propagateEnabled = true;

    /** 默认连接超时（毫秒），写入 feign.client.config.default.connect-timeout */
    private int connectTimeout = 3000;

    /** 默认读超时（毫秒），写入 feign.client.config.default.read-timeout */
    private int readTimeout = 5000;

    private final Retry retry = new Retry();

    public boolean isPropagateEnabled() {
        return propagateEnabled;
    }

    public void setPropagateEnabled(boolean propagateEnabled) {
        this.propagateEnabled = propagateEnabled;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public static class Retry {

        /**
         * 默认关闭重试：非幂等接口（POST 写操作）自动重试可能产生重复数据，
         * 确需重试的读接口建议显式开启并确认接口幂等。
         */
        private boolean enabled = false;

        /** 首次重试间隔（毫秒） */
        private long period = 100;

        /** 最大重试间隔（毫秒），间隔按指数退避增长但不超过该值 */
        private long maxPeriod = 1000;

        /** 最大尝试次数（含首次调用），2 表示失败后重试 1 次 */
        private int maxAttempts = 2;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getPeriod() {
            return period;
        }

        public void setPeriod(long period) {
            this.period = period;
        }

        public long getMaxPeriod() {
            return maxPeriod;
        }

        public void setMaxPeriod(long maxPeriod) {
            this.maxPeriod = maxPeriod;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
