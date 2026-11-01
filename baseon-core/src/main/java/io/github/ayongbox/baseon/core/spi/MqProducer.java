package io.github.ayongbox.baseon.core.spi;

/**
 * MQ 生产者 SPI：业务侧唯一发送入口，默认实现由 starter-mq-rocketmq 装配。
 *
 * <p>发送失败（网络异常、超时、Broker 拒绝）统一抛 {@code BizException(REMOTE_CALL_FAILED)}，
 * 由调用方决定是否重试或落库补偿；同步 {@link #send} 返回即表示 Broker 已确认接收。
 *
 * <p>引入任一 starter-mq-* 实现即可注入本接口；业务可声明自己的实现 Bean 覆盖默认装配。
 */
public interface MqProducer {

    /**
     * 同步发送：阻塞至 Broker 确认，失败抛异常。可靠性最高的发送方式。
     *
     * @param message 消息（topic、body 必填）
     */
    void send(MqMessage message);

    /**
     * 异步发送：立即返回，结果经 {@link SendCallback} 通知。适用于批量、高吞吐场景。
     *
     * @param message  消息（topic、body 必填）
     * @param callback 结果回调，不得为 null
     */
    void sendAsync(MqMessage message, SendCallback callback);

    /**
     * 单向发送：不等 Broker 确认，无失败回调。仅用于日志、埋点等可容忍丢失的场景。
     *
     * @param message 消息（topic、body 必填）
     */
    void sendOneway(MqMessage message);

    /**
     * 延迟发送：消息在指定时长后才对消费者可见，失败抛异常。
     *
     * <p>RocketMQ 4.9 客户端按固定延迟级别就近向上取整（最长 2 小时），
     * 语义为「至少延迟 delayMillis」；对精度敏感的场景自行评估。
     *
     * @param message    消息（topic、body 必填）
     * @param delayMillis 延迟毫秒数，必须为正数
     */
    void sendDelay(MqMessage message, long delayMillis);

    /** 便捷发送：无 tag 与业务键 */
    default void send(String topic, String body) {
        send(MqMessage.of(topic, body));
    }
}
