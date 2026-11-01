package io.github.ayongbox.baseon.starter.mq.rocketmq.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.spi.MqMessage;
import io.github.ayongbox.baseon.core.spi.SendCallback;
import io.github.ayongbox.baseon.starter.mq.rocketmq.config.BaseonMqProperties;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 生产者适配器：消息转换委托、异常包装、异步回调透传。
 */
class RocketMqProducerAdapterTest {

    private final MQProducer producer = mock(MQProducer.class);
    private final RocketMqProducerAdapter adapter = new RocketMqProducerAdapter(producer, new BaseonMqProperties());

    @Test
    void sendShouldDelegateToRocketClient() throws Exception {
        adapter.send(MqMessage.of("topic-a", "tag-b", "key-c", "body"));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo("topic-a");
        assertThat(captor.getValue().getKeys()).isEqualTo("key-c");
    }

    @Test
    void sendShouldWrapClientFailureAsBizException() throws Exception {
        doThrow(new MQClientException("broker down", null)).when(producer).send(any(Message.class));

        assertThatThrownBy(() -> adapter.send(MqMessage.of("t", "b")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(CommonErrorCode.REMOTE_CALL_FAILED.getCode()));
    }

    @Test
    void sendAsyncShouldBridgeCallbackResult() throws Exception {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<MqMessage> messageRef = new AtomicReference<>();
        SendCallback callback = (message, error) -> {
            messageRef.set(message);
            errorRef.set(error);
        };

        adapter.sendAsync(MqMessage.of("t", "b"), callback);

        ArgumentCaptor<org.apache.rocketmq.client.producer.SendCallback> captor =
                ArgumentCaptor.forClass(org.apache.rocketmq.client.producer.SendCallback.class);
        verify(producer).send(any(Message.class), captor.capture());

        captor.getValue().onSuccess(new SendResult());
        assertThat(messageRef.get()).isNotNull();
        assertThat(errorRef.get()).isNull();

        RuntimeException failure = new RuntimeException("timeout");
        captor.getValue().onException(failure);
        assertThat(errorRef.get()).isSameAs(failure);
    }

    @Test
    void sendOnewayShouldDelegate() throws Exception {
        adapter.sendOneway(MqMessage.of("t", "b"));
        verify(producer).sendOneway(any(Message.class));
    }

    @Test
    void sendDelayShouldMapToNearestLevel() throws Exception {
        adapter.sendDelay(MqMessage.of("t", "b"), 7000L);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        // 7s 就近向上映射到 10s（第 3 级）
        assertThat(captor.getValue().getDelayTimeLevel()).isEqualTo(3);
    }
}
