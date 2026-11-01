package io.github.ayongbox.baseon.core.spi;

/**
 * 消费结果：{@link MqConsumerListener} 的返回契约，实现侧映射为对应 MQ 的确认语义。
 */
public enum ConsumeResult {

    /** 消费成功，提交位点 */
    SUCCESS,

    /** 消费失败，要求 MQ 服务端稍后重投（直至进入死信） */
    RETRY
}
