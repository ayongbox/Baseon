package io.github.ayongbox.baseon.core.spi;

/**
 * 异步发送回调：{@link MqProducer#sendAsync(MqMessage, SendCallback)} 的结果通知。
 *
 * <p>回调在 MQ 客户端的 IO 线程上执行，实现内禁止阻塞与重业务逻辑；
 * 需要上下文的处理请自行用 {@code ContextWrapper} 包装。
 */
public interface SendCallback {

    /**
     * 发送结果通知。
     *
     * @param message 原始消息
     * @param error   {@code null} 表示发送成功；非 null 表示发送失败（含超时）
     */
    void onComplete(MqMessage message, Throwable error);
}
