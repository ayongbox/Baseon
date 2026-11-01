package io.github.ayongbox.baseon.core.spi;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MQ 消息模型：统一 Producer/Consumer 抽象的传输载体，与具体 MQ 实现解耦。
 *
 * <p>业务侧只与 {@link MqProducer} / {@link MqConsumerListener} 交互，不感知底层
 * RocketMQ / Kafka 等实现的消息结构。body 固定为字符串（实现侧按 UTF-8 编解码）。
 *
 * <p>消息头约定：生产者发送时自动注入当前登录上下文（{@link #HEADER_USER_ID} 等），
 * 消费端据此恢复 {@code SecurityUtil} 上下文；业务自定义 header 通过 {@link #putHeader}
 * 透传。消费端重建消息时 headers 可能包含 MQ 实现的系统属性键，以各实现手册为准。
 */
public class MqMessage {

    /** 消息头：发送时自动注入的当前登录用户 ID，消费端用于恢复登录上下文 */
    public static final String HEADER_USER_ID = "baseonUserId";

    /** 消息头：发送时自动注入的当前租户 ID */
    public static final String HEADER_TENANT_ID = "baseonTenantId";

    /** 消息头：发送时自动注入的当前登录用户名 */
    public static final String HEADER_USERNAME = "baseonUsername";

    /** 主题，必填 */
    private String topic;

    /** 过滤标签，可选；消费端按 tag 订阅过滤 */
    private String tag;

    /** 业务唯一键，可选但强烈建议填写：消费幂等、消息轨迹均以此为准 */
    private String key;

    /** 消息体，必填；实现侧按 UTF-8 编解码 */
    private String body;

    /** 消息 ID：发送端为空，消费端由实现填充（重投去重的兜底键） */
    private String msgId;

    /** 透传消息头 */
    private Map<String, String> headers = new LinkedHashMap<>();

    public static MqMessage of(String topic, String body) {
        MqMessage message = new MqMessage();
        message.setTopic(topic);
        message.setBody(body);
        return message;
    }

    public static MqMessage of(String topic, String tag, String key, String body) {
        MqMessage message = of(topic, body);
        message.setTag(tag);
        message.setKey(key);
        return message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    }

    /** 追加透传头；value 为 null 时忽略（部分 MQ 实现不允许 null 值属性） */
    public void putHeader(String name, String value) {
        if (value != null) {
            this.headers.put(name, value);
        }
    }
}
