package io.github.ayongbox.baseon.starter.mq.rocketmq.support;

import io.github.ayongbox.baseon.core.annotation.MqListener;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.spi.ConsumeResult;
import io.github.ayongbox.baseon.core.spi.MqConsumerListener;
import io.github.ayongbox.baseon.core.spi.MqMessage;
import io.github.ayongbox.baseon.starter.mq.rocketmq.config.BaseonMqProperties;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.StaticApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消费者注册器：扫描注册、上下文恢复、幂等跳过、异常重投。
 */
class MqConsumerRegistrarTest {

    private final BaseonMqProperties properties = new BaseonMqProperties();

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    void registerShouldSubscribeAndStartConsumer() throws Exception {
        DefaultMQPushConsumer consumer = mock(DefaultMQPushConsumer.class);
        MqConsumerRegistrar registrar = registrar(new RecordingListener(), consumer, null);

        registrar.afterSingletonsInstantiated();

        verify(consumer).subscribe("topic-order", "paid");
        verify(consumer).registerMessageListener(any(MessageListenerConcurrently.class));
        verify(consumer).start();

        registrar.destroy();
        verify(consumer).shutdown();
    }

    @Test
    void registerShouldFailFastWhenAnnotationMissing() {
        MqConsumerRegistrar registrar = registrar(new BareListener(), mock(DefaultMQPushConsumer.class), null);

        assertThatThrownBy(registrar::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@MqListener");
    }

    @Test
    void dispatchShouldRestoreContextAndClearAfterConsume() throws Exception {
        AtomicReference<Long> seenUser = new AtomicReference<>();
        MessageListenerConcurrently rocketListener = registerAndCapture(
                message -> {
                    seenUser.set(SecurityUtil.getUserId());
                    return ConsumeResult.SUCCESS;
                }, null);

        MessageExt ext = ext("order-1");
        ext.putUserProperty(MqMessage.HEADER_USER_ID, "1001");
        ext.putUserProperty(MqMessage.HEADER_TENANT_ID, "88");

        ConsumeConcurrentlyStatus status = rocketListener.consumeMessage(List.of(ext), null);

        assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
        assertThat(seenUser.get()).isEqualTo(1001L);
        assertThat(SecurityUtil.get()).as("消费结束后上下文必须清理").isNull();
    }

    @Test
    void dispatchShouldReturnRetryWhenListenerReturnsRetryOrThrows() throws Exception {
        MessageListenerConcurrently retryListener = registerAndCapture(message -> ConsumeResult.RETRY, null);
        assertThat(retryListener.consumeMessage(List.of(ext("k")), null))
                .isEqualTo(ConsumeConcurrentlyStatus.RECONSUME_LATER);

        MessageListenerConcurrently failingListener = registerAndCapture(message -> {
            throw new IllegalStateException("db down");
        }, null);
        assertThat(failingListener.consumeMessage(List.of(ext("k")), null))
                .isEqualTo(ConsumeConcurrentlyStatus.RECONSUME_LATER);
    }

    @Test
    void dispatchShouldSkipDuplicatedMessageWithinWindow() throws Exception {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(false);

        AtomicReference<MqMessage> consumed = new AtomicReference<>();
        MessageListenerConcurrently rocketListener = registerAndCapture(message -> {
            consumed.set(message);
            return ConsumeResult.SUCCESS;
        }, redisUtil);

        ConsumeConcurrentlyStatus status = rocketListener.consumeMessage(List.of(ext("order-1")), null);

        assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
        assertThat(consumed.get()).as("窗口期内重复消息不应进入业务监听").isNull();
    }

    @Test
    void dispatchShouldConsumeWhenIdempotentSlotAcquired() throws Exception {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);

        AtomicReference<MqMessage> consumed = new AtomicReference<>();
        MessageListenerConcurrently rocketListener = registerAndCapture(message -> {
            consumed.set(message);
            return ConsumeResult.SUCCESS;
        }, redisUtil);

        rocketListener.consumeMessage(List.of(ext("order-1")), null);

        assertThat(consumed.get()).isNotNull();
        verify(redisUtil).setIfAbsent("mq:idem:order-1", "1", properties.getIdempotentTtlSeconds());
    }

    /**
     * 把监听器放进独立容器并完成注册，返回捕获到的 RocketMQ 侧监听器。
     * lambda 没有类级注解，统一包装进带 @MqListener 的委托类。
     */
    private MessageListenerConcurrently registerAndCapture(MqConsumerListener listener, RedisUtil redisUtil) {
        DefaultMQPushConsumer consumer = mock(DefaultMQPushConsumer.class);
        MqConsumerRegistrar registrar = registrar(new DelegatingListener(listener), consumer, redisUtil);
        registrar.afterSingletonsInstantiated();

        ArgumentCaptor<MessageListenerConcurrently> captor = ArgumentCaptor.forClass(MessageListenerConcurrently.class);
        try {
            verify(consumer).registerMessageListener(captor.capture());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return captor.getValue();
    }

    /** 构造注册器：监听器注入独立容器，consumer 创建替换为 mock */
    private MqConsumerRegistrar registrar(MqConsumerListener listener, DefaultMQPushConsumer consumer,
                                          RedisUtil redisUtil) {
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("listener", listener);
        return new MqConsumerRegistrar(context, properties, providerOf(redisUtil), emptyProvider()) {
            @Override
            protected DefaultMQPushConsumer createConsumer(MqListener annotation) {
                return consumer;
            }
        };
    }

    private static MessageExt ext(String key) {
        MessageExt ext = new MessageExt();
        ext.setTopic("topic-order");
        ext.setKeys(key);
        ext.setMsgId("msg-" + key);
        ext.setBody("{}".getBytes(StandardCharsets.UTF_8));
        return ext;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> providerOf(T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        if (bean != null) {
            when(provider.getIfAvailable()).thenReturn(bean);
        }
        return provider;
    }

    @MqListener(topic = "topic-order", consumerGroup = "group-order", tags = "paid")
    static class RecordingListener implements MqConsumerListener {
        @Override
        public ConsumeResult onMessage(MqMessage message) {
            return ConsumeResult.SUCCESS;
        }
    }

    static class BareListener implements MqConsumerListener {
        @Override
        public ConsumeResult onMessage(MqMessage message) {
            return ConsumeResult.SUCCESS;
        }
    }

    @MqListener(topic = "topic-order", consumerGroup = "group-order")
    static class DelegatingListener implements MqConsumerListener {
        private final MqConsumerListener delegate;

        DelegatingListener(MqConsumerListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public ConsumeResult onMessage(MqMessage message) {
            return delegate.onMessage(message);
        }
    }
}
