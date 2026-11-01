package io.github.ayongbox.baseon.starter.mq.rocketmq.support;

import io.github.ayongbox.baseon.core.annotation.MqListener;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;
import io.github.ayongbox.baseon.core.spi.ConsumeResult;
import io.github.ayongbox.baseon.core.spi.MqConsumerListener;
import io.github.ayongbox.baseon.core.spi.MqMessage;
import io.github.ayongbox.baseon.core.tool.StrUtil;
import io.github.ayongbox.baseon.starter.mq.rocketmq.config.BaseonMqProperties;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 消费者注册器：启动期扫描 {@link MqConsumerListener} Bean，按 {@link MqListener}
 * 声明创建并启动 RocketMQ PushConsumer；容器关闭时统一 shutdown。
 *
 * <p>单条消费语义（batchMaxSize=1）：框架依次完成「恢复登录上下文 → 幂等占位 →
 * 业务监听 → 清理上下文」，监听器抛异常或返回 RETRY 即要求重投。
 *
 * <p>幂等语义：key（为空时用 msgId）在窗口期内重复到达直接跳过并返回成功；
 * 占位 key 为 {@code mq:idem:} + [租户前缀] + 业务 key，与 starter-idempotent 同一套
 * RedisUtil（全局前缀与 key 校验在其内部完成）。未引入 starter-redis 时幂等自动跳过，
 * 消费方需自行保证幂等。
 */
public class MqConsumerRegistrar implements SmartInitializingSingleton, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MqConsumerRegistrar.class);

    /** 消费幂等占位的命名空间前缀 */
    private static final String IDEM_KEY_PREFIX = "mq:idem:";

    /** 占位值无业务含义，固定 "1" */
    private static final String IDEM_PLACEHOLDER = "1";

    private final ApplicationContext applicationContext;
    private final BaseonMqProperties properties;
    private final ObjectProvider<RedisUtil> redisUtilProvider;
    private final ObjectProvider<CacheKeyPrefixProvider> prefixProvider;
    private final List<DefaultMQPushConsumer> consumers = new CopyOnWriteArrayList<>();

    public MqConsumerRegistrar(ApplicationContext applicationContext, BaseonMqProperties properties,
                               ObjectProvider<RedisUtil> redisUtilProvider,
                               ObjectProvider<CacheKeyPrefixProvider> prefixProvider) {
        this.applicationContext = applicationContext;
        this.properties = properties;
        this.redisUtilProvider = redisUtilProvider;
        this.prefixProvider = prefixProvider;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, MqConsumerListener> listeners = applicationContext.getBeansOfType(MqConsumerListener.class);
        listeners.forEach((name, listener) -> register(name, listener));
        if (!listeners.isEmpty()) {
            log.info("RocketMQ 消费者注册完成，共 {} 个监听器", listeners.size());
        }
    }

    private void register(String beanName, MqConsumerListener listener) {
        Class<?> targetClass = AopUtils.getTargetClass(listener);
        MqListener annotation = AnnotationUtils.findAnnotation(targetClass, MqListener.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "MqConsumerListener 实现类缺少 @MqListener 注解: " + targetClass.getName());
        }
        DefaultMQPushConsumer consumer = createConsumer(annotation);
        try {
            consumer.subscribe(annotation.topic(), annotation.tags());
            consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> dispatch(listener, msgs));
            consumer.start();
        } catch (MQClientException e) {
            throw new IllegalStateException(
                    "RocketMQ 消费者启动失败: bean=" + beanName + ", topic=" + annotation.topic(), e);
        }
        consumers.add(consumer);
        log.info("RocketMQ 消费者已启动: group={}, topic={}, tags={}",
                annotation.consumerGroup(), annotation.topic(), annotation.tags());
    }

    /**
     * 创建 PushConsumer（未 start）；protected 便于测试替换为 mock 实例。
     */
    protected DefaultMQPushConsumer createConsumer(MqListener annotation) {
        DefaultMQPushConsumer consumer;
        if (StrUtil.isNotBlank(properties.getAccessKey())) {
            // 4.9.x 的 PushConsumer 没有 (group, RPCHook) 两参构造器，ACL 必须走含分配策略的三参版本
            consumer = new DefaultMQPushConsumer(annotation.consumerGroup(),
                    new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(), properties.getSecretKey())),
                    new AllocateMessageQueueAveragely());
        } else {
            consumer = new DefaultMQPushConsumer(annotation.consumerGroup());
        }
        consumer.setNamesrvAddr(properties.getNameServer());
        // 单条消费：批量重投语义复杂（任一失败整批重投），固定 1 保持「一条一结果」
        consumer.setConsumeMessageBatchMaxSize(1);
        consumer.setConsumeTimeout(properties.getConsumeTimeoutMinutes());
        return consumer;
    }

    private ConsumeConcurrentlyStatus dispatch(MqConsumerListener listener, List<MessageExt> msgs) {
        for (MessageExt ext : msgs) {
            MqMessage message = RocketMqMessageConverter.toMqMessage(ext);
            restoreContext(message);
            try {
                if (isDuplicate(message)) {
                    log.warn("MQ 消息重复到达已跳过: topic={}, key={}, msgId={}",
                            message.getTopic(), message.getKey(), message.getMsgId());
                    continue;
                }
                ConsumeResult result = listener.onMessage(message);
                if (ConsumeResult.RETRY.equals(result)) {
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            } catch (Exception e) {
                log.error("MQ 消费异常将重投: topic={}, key={}, msgId={}",
                        message.getTopic(), message.getKey(), message.getMsgId(), e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            } finally {
                SecurityUtil.clear();
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    /** 从消息头恢复登录上下文（仅当透传开启且头存在时） */
    private void restoreContext(MqMessage message) {
        if (!properties.isContextPropagate()) {
            return;
        }
        String userId = message.getHeaders().get(MqMessage.HEADER_USER_ID);
        if (StrUtil.isBlank(userId)) {
            return;
        }
        LoginUser user = new LoginUser();
        user.setUserId(Long.valueOf(userId));
        String tenantId = message.getHeaders().get(MqMessage.HEADER_TENANT_ID);
        if (StrUtil.isNotBlank(tenantId)) {
            user.setTenantId(Long.valueOf(tenantId));
        }
        user.setUsername(message.getHeaders().get(MqMessage.HEADER_USERNAME));
        // 权限集合不随消息透传，消费场景不做接口级鉴权，置空集避免下游 NPE
        user.setPermissions(Collections.emptySet());
        SecurityUtil.set(user);
    }

    /** 幂等占位：返回 true 表示窗口期内重复，应跳过 */
    private boolean isDuplicate(MqMessage message) {
        if (!properties.isIdempotentConsume()) {
            return false;
        }
        RedisUtil redisUtil = redisUtilProvider.getIfAvailable();
        if (redisUtil == null) {
            return false;
        }
        String idemKey = StrUtil.isNotBlank(message.getKey()) ? message.getKey() : message.getMsgId();
        if (StrUtil.isBlank(idemKey)) {
            return false;
        }
        StringBuilder key = new StringBuilder(IDEM_KEY_PREFIX);
        CacheKeyPrefixProvider prefix = prefixProvider.getIfAvailable();
        if (prefix != null) {
            key.append(prefix.prefix());
        }
        key.append(idemKey);
        Boolean acquired = redisUtil.setIfAbsent(key.toString(), IDEM_PLACEHOLDER,
                properties.getIdempotentTtlSeconds());
        return Boolean.FALSE.equals(acquired);
    }

    @Override
    public void destroy() {
        consumers.forEach(DefaultMQPushConsumer::shutdown);
        consumers.clear();
    }
}
