package io.github.ayongbox.baseon.starter.mq.rocketmq.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.spi.MqMessage;
import io.github.ayongbox.baseon.core.spi.MqProducer;
import io.github.ayongbox.baseon.core.spi.SendCallback;
import io.github.ayongbox.baseon.starter.mq.rocketmq.config.BaseonMqProperties;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * {@link MqProducer} 的 RocketMQ 适配：消息转换 + 异常统一包装为 {@code BizException}。
 *
 * <p>底层客户端由装配类完成 start/shutdown 生命周期管理，本类无状态。
 */
public class RocketMqProducerAdapter implements MqProducer {

    private final MQProducer producer;
    private final BaseonMqProperties properties;

    public RocketMqProducerAdapter(MQProducer producer, BaseonMqProperties properties) {
        this.producer = producer;
        this.properties = properties;
    }

    @Override
    public void send(MqMessage message) {
        try {
            producer.send(RocketMqMessageConverter.toRocketMessage(message, properties.isContextPropagate()));
        } catch (Exception e) {
            throw sendFailure(e);
        }
    }

    @Override
    public void sendAsync(MqMessage message, SendCallback callback) {
        Message rocketMessage = RocketMqMessageConverter.toRocketMessage(message, properties.isContextPropagate());
        try {
            producer.send(rocketMessage, new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    callback.onComplete(message, null);
                }

                @Override
                public void onException(Throwable e) {
                    callback.onComplete(message, e);
                }
            });
        } catch (Exception e) {
            throw sendFailure(e);
        }
    }

    @Override
    public void sendOneway(MqMessage message) {
        try {
            producer.sendOneway(RocketMqMessageConverter.toRocketMessage(message, properties.isContextPropagate()));
        } catch (Exception e) {
            throw sendFailure(e);
        }
    }

    @Override
    public void sendDelay(MqMessage message, long delayMillis) {
        Message rocketMessage = RocketMqMessageConverter.toRocketMessage(message, properties.isContextPropagate());
        rocketMessage.setDelayTimeLevel(RocketMqMessageConverter.toDelayLevel(delayMillis));
        try {
            producer.send(rocketMessage);
        } catch (Exception e) {
            throw sendFailure(e);
        }
    }

    private BizException sendFailure(Exception e) {
        return new BizException(CommonErrorCode.REMOTE_CALL_FAILED, "MQ 消息发送失败: " + e.getMessage());
    }
}
