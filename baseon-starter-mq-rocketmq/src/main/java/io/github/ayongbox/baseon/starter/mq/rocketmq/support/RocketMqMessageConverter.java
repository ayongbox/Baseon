package io.github.ayongbox.baseon.starter.mq.rocketmq.support;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.spi.MqMessage;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * {@link MqMessage} 与 RocketMQ 消息模型的双向转换。
 *
 * <p>body 固定 UTF-8 编解码；业务 header 与登录上下文头均走 userProperty 透传。
 * 消费端 headers 全量保留 RocketMQ 属性表（含系统键，如 KEYS/TAGS/UNIQ_KEY），
 * 业务取值时只应关心自己写入的键与 {@link MqMessage#HEADER_USER_ID} 等框架键。
 */
public final class RocketMqMessageConverter {

    /**
     * RocketMQ 4.9.x 固定延迟级别表（毫秒），级别 n 对应数组第 n-1 项：
     * 1s 5s 10s 30s 1m 2m ... 10m 20m 30m 1h 2h。
     */
    static final long[] DELAY_LEVEL_MILLIS = {
        1000L, 5000L, 10000L, 30000L,
        60000L, 120000L, 180000L, 240000L, 300000L, 360000L, 420000L, 480000L, 540000L, 600000L,
        1200000L, 1800000L, 3600000L, 7200000L
    };

    private RocketMqMessageConverter() {
    }

    /** 业务消息 → RocketMQ 消息；contextPropagate 为 true 时注入当前登录上下文头 */
    public static Message toRocketMessage(MqMessage message, boolean contextPropagate) {
        Message rocketMessage = new Message(message.getTopic(), message.getTag(), message.getKey(),
                message.getBody() == null ? new byte[0] : message.getBody().getBytes(StandardCharsets.UTF_8));
        message.getHeaders().forEach(rocketMessage::putUserProperty);
        if (contextPropagate) {
            LoginUser user = SecurityUtil.get();
            if (user != null) {
                putIfNotNull(rocketMessage, MqMessage.HEADER_USER_ID, user.getUserId());
                putIfNotNull(rocketMessage, MqMessage.HEADER_TENANT_ID, user.getTenantId());
                if (user.getUsername() != null) {
                    rocketMessage.putUserProperty(MqMessage.HEADER_USERNAME, user.getUsername());
                }
            }
        }
        return rocketMessage;
    }

    /** RocketMQ 消费消息 → 业务消息 */
    public static MqMessage toMqMessage(MessageExt ext) {
        MqMessage message = new MqMessage();
        message.setTopic(ext.getTopic());
        message.setTag(ext.getTags());
        message.setKey(ext.getKeys());
        message.setMsgId(ext.getMsgId());
        message.setBody(ext.getBody() == null ? "" : new String(ext.getBody(), StandardCharsets.UTF_8));
        Map<String, String> properties = ext.getProperties();
        if (properties != null) {
            message.setHeaders(properties);
        }
        return message;
    }

    /**
     * 延迟毫秒就近向上映射到 4.9.x 固定级别（语义为「至少延迟这么久」）；
     * 超过最长级别（2h）按最长级别处理。
     */
    public static int toDelayLevel(long delayMillis) {
        for (int i = 0; i < DELAY_LEVEL_MILLIS.length; i++) {
            if (delayMillis <= DELAY_LEVEL_MILLIS[i]) {
                return i + 1;
            }
        }
        return DELAY_LEVEL_MILLIS.length;
    }

    private static void putIfNotNull(Message rocketMessage, String name, Object value) {
        if (value != null) {
            rocketMessage.putUserProperty(name, String.valueOf(value));
        }
    }
}
