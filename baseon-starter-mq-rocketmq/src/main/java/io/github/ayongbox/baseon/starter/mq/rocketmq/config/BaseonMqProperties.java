package io.github.ayongbox.baseon.starter.mq.rocketmq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RocketMQ 实现配置，前缀 {@code baseon.mq.rocketmq}。
 *
 * <p>{@code name-server} 是唯一必填项：未配置时生产者与消费者均不装配
 * （视为未启用 MQ），其余配置项全部有默认值。
 */
@ConfigurationProperties(prefix = "baseon.mq.rocketmq")
public class BaseonMqProperties {

    /** 总开关：false 时本模块所有 Bean 不装配 */
    private boolean enabled = true;

    /** NameServer 地址，多个用分号分隔（如 {@code "host1:9876;host2:9876"}）；为空则不装配 */
    private String nameServer;

    /** ACL 接入 Key，与 secretKey 成对配置；集群未开启 ACL 时留空 */
    private String accessKey;

    /** ACL 秘密 Key */
    private String secretKey;

    /** 生产者组名 */
    private String producerGroup = "baseon-producer";

    /** 同步发送超时（毫秒） */
    private int sendTimeoutMillis = 3000;

    /**
     * 消费幂等开关：开启且容器中存在 RedisUtil（引入 starter-redis）时，
     * 同一业务 key（为空时用 msgId）在窗口期内重复到达直接跳过。
     */
    private boolean idempotentConsume = true;

    /** 消费幂等窗口（秒）：默认 24 小时，覆盖 RocketMQ 默认 16 次重投的全周期 */
    private long idempotentTtlSeconds = 86400;

    /**
     * 登录上下文透传开关：开启时发送方自动注入 userId/tenantId/username 到消息头，
     * 消费方执行监听器前恢复到 {@code SecurityUtil}。
     */
    private boolean contextPropagate = true;

    /** 单条消息消费超时（分钟），超时按失败重投 */
    private long consumeTimeoutMinutes = 15;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getProducerGroup() {
        return producerGroup;
    }

    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
    }

    public int getSendTimeoutMillis() {
        return sendTimeoutMillis;
    }

    public void setSendTimeoutMillis(int sendTimeoutMillis) {
        this.sendTimeoutMillis = sendTimeoutMillis;
    }

    public boolean isIdempotentConsume() {
        return idempotentConsume;
    }

    public void setIdempotentConsume(boolean idempotentConsume) {
        this.idempotentConsume = idempotentConsume;
    }

    public long getIdempotentTtlSeconds() {
        return idempotentTtlSeconds;
    }

    public void setIdempotentTtlSeconds(long idempotentTtlSeconds) {
        this.idempotentTtlSeconds = idempotentTtlSeconds;
    }

    public boolean isContextPropagate() {
        return contextPropagate;
    }

    public void setContextPropagate(boolean contextPropagate) {
        this.contextPropagate = contextPropagate;
    }

    public long getConsumeTimeoutMinutes() {
        return consumeTimeoutMinutes;
    }

    public void setConsumeTimeoutMinutes(long consumeTimeoutMinutes) {
        this.consumeTimeoutMinutes = consumeTimeoutMinutes;
    }
}
