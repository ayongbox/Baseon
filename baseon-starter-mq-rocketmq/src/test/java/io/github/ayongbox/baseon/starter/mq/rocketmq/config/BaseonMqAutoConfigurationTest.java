package io.github.ayongbox.baseon.starter.mq.rocketmq.config;

import io.github.ayongbox.baseon.core.spi.MqMessage;
import io.github.ayongbox.baseon.core.spi.MqProducer;
import io.github.ayongbox.baseon.core.spi.SendCallback;
import io.github.ayongbox.baseon.starter.mq.rocketmq.support.RocketMqProducerAdapter;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 装配条件：name-server 未配置不装配、enabled=false 不装配、业务 Bean 覆盖默认装配。
 */
class BaseonMqAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseonMqAutoConfiguration.class));

    @Test
    void shouldNotAssembleWhenNameServerMissing() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(DefaultMQProducer.class);
            assertThat(context).doesNotHaveBean(MqProducer.class);
        });
    }

    @Test
    void shouldNotAssembleWhenDisabled() {
        runner.withPropertyValues(
                "baseon.mq.rocketmq.enabled=false",
                "baseon.mq.rocketmq.name-server=127.0.0.1:9876")
                .run(context -> assertThat(context).doesNotHaveBean(MqProducer.class));
    }

    @Test
    void shouldAssembleAdapterOverProvidedRocketProducer() {
        runner.withPropertyValues("baseon.mq.rocketmq.name-server=127.0.0.1:9876")
                // 业务侧已提供底层客户端时，默认 DefaultMQProducer 不重复创建，
                // 避免测试环境真实连接 NameServer
                .withBean(DefaultMQProducer.class, () -> mock(DefaultMQProducer.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(MqProducer.class);
                    assertThat(context.getBean(MqProducer.class)).isInstanceOf(RocketMqProducerAdapter.class);
                });
    }

    @Test
    void shouldBackOffWhenBusinessProvidesMqProducer() {
        runner.withPropertyValues("baseon.mq.rocketmq.name-server=127.0.0.1:9876")
                .withBean(DefaultMQProducer.class, () -> mock(DefaultMQProducer.class))
                .withBean(MqProducer.class, () -> new MqProducer() {
                    @Override
                    public void send(MqMessage message) {
                    }

                    @Override
                    public void sendAsync(MqMessage message, SendCallback callback) {
                    }

                    @Override
                    public void sendOneway(MqMessage message) {
                    }

                    @Override
                    public void sendDelay(MqMessage message, long delayMillis) {
                    }
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(MqProducer.class);
                    assertThat(context.getBean(MqProducer.class)).isNotInstanceOf(RocketMqProducerAdapter.class);
                });
    }
}
