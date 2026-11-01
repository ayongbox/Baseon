package io.github.ayongbox.baseon.starter.mq.rocketmq.config;

import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;
import io.github.ayongbox.baseon.core.spi.MqProducer;
import io.github.ayongbox.baseon.core.tool.StrUtil;
import io.github.ayongbox.baseon.starter.mq.rocketmq.support.MqConsumerRegistrar;
import io.github.ayongbox.baseon.starter.mq.rocketmq.support.RocketMqProducerAdapter;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * RocketMQ 装配：生产者、生产者适配器、消费者注册器。
 *
 * <p>三层能力按需降级：未配置 {@code baseon.mq.rocketmq.name-server} 时整体不装配；
 * 未引入 starter-redis 时消费幂等自动跳过；未引入 starter-tenant 时幂等 key 无租户前缀。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.mq.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonMqProperties.class)
public class BaseonMqAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.mq.rocketmq", name = "name-server")
    public DefaultMQProducer rocketMQProducer(BaseonMqProperties properties) throws MQClientException {
        DefaultMQProducer producer;
        if (StrUtil.isNotBlank(properties.getAccessKey())) {
            producer = new DefaultMQProducer(properties.getProducerGroup(),
                    new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(), properties.getSecretKey())));
        } else {
            producer = new DefaultMQProducer(properties.getProducerGroup());
        }
        producer.setNamesrvAddr(properties.getNameServer());
        producer.setSendMsgTimeout(properties.getSendTimeoutMillis());
        producer.start();
        return producer;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.mq.rocketmq", name = "name-server")
    public MqProducer mqProducer(DefaultMQProducer rocketMQProducer, BaseonMqProperties properties) {
        return new RocketMqProducerAdapter(rocketMQProducer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.mq.rocketmq", name = "name-server")
    public MqConsumerRegistrar mqConsumerRegistrar(ApplicationContext applicationContext,
                                                   BaseonMqProperties properties,
                                                   ObjectProvider<RedisUtil> redisUtilProvider,
                                                   ObjectProvider<CacheKeyPrefixProvider> prefixProvider) {
        return new MqConsumerRegistrar(applicationContext, properties, redisUtilProvider, prefixProvider);
    }
}
