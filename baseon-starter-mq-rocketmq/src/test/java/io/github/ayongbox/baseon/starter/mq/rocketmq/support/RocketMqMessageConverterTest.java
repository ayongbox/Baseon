package io.github.ayongbox.baseon.starter.mq.rocketmq.support;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.spi.MqMessage;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 消息双向转换与延迟级别映射的纯单元测试。
 */
class RocketMqMessageConverterTest {

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    void toRocketMessageShouldCopyFieldsAndHeaders() {
        MqMessage message = MqMessage.of("topic-a", "tag-b", "key-c", "你好");
        message.putHeader("biz", "v1");

        Message rocketMessage = RocketMqMessageConverter.toRocketMessage(message, false);

        assertThat(rocketMessage.getTopic()).isEqualTo("topic-a");
        assertThat(rocketMessage.getTags()).isEqualTo("tag-b");
        assertThat(rocketMessage.getKeys()).isEqualTo("key-c");
        assertThat(new String(rocketMessage.getBody(), StandardCharsets.UTF_8)).isEqualTo("你好");
        assertThat(rocketMessage.getUserProperty("biz")).isEqualTo("v1");
    }

    @Test
    void toRocketMessageShouldPropagateLoginContextWhenEnabled() {
        LoginUser user = new LoginUser();
        user.setUserId(1001L);
        user.setTenantId(88L);
        user.setUsername("ayong");
        SecurityUtil.set(user);

        Message rocketMessage = RocketMqMessageConverter.toRocketMessage(MqMessage.of("t", "b"), true);

        assertThat(rocketMessage.getUserProperty(MqMessage.HEADER_USER_ID)).isEqualTo("1001");
        assertThat(rocketMessage.getUserProperty(MqMessage.HEADER_TENANT_ID)).isEqualTo("88");
        assertThat(rocketMessage.getUserProperty(MqMessage.HEADER_USERNAME)).isEqualTo("ayong");
    }

    @Test
    void toRocketMessageShouldNotPropagateContextWhenDisabledOrAnonymous() {
        Message disabled = RocketMqMessageConverter.toRocketMessage(MqMessage.of("t", "b"), false);
        assertThat(disabled.getUserProperty(MqMessage.HEADER_USER_ID)).isNull();

        Message anonymous = RocketMqMessageConverter.toRocketMessage(MqMessage.of("t", "b"), true);
        assertThat(anonymous.getUserProperty(MqMessage.HEADER_USER_ID)).isNull();
    }

    @Test
    void toMqMessageShouldRebuildMessageWithMsgIdAndProperties() {
        MessageExt ext = new MessageExt();
        ext.setTopic("topic-a");
        ext.putUserProperty("biz", "v1");
        ext.putUserProperty(MqMessage.HEADER_USER_ID, "1001");
        ext.setBody("内容".getBytes(StandardCharsets.UTF_8));

        MqMessage message = RocketMqMessageConverter.toMqMessage(ext);

        assertThat(message.getTopic()).isEqualTo("topic-a");
        assertThat(message.getBody()).isEqualTo("内容");
        assertThat(message.getHeaders()).containsEntry("biz", "v1");
        assertThat(message.getHeaders()).containsEntry(MqMessage.HEADER_USER_ID, "1001");
    }

    @Test
    void toDelayLevelShouldRoundUpToNearestLevel() {
        // 各级别边界精确命中
        assertThat(RocketMqMessageConverter.toDelayLevel(1000L)).isEqualTo(1);
        assertThat(RocketMqMessageConverter.toDelayLevel(5000L)).isEqualTo(2);
        // 就近向上：500ms 按 1s，7s 按 10s
        assertThat(RocketMqMessageConverter.toDelayLevel(500L)).isEqualTo(1);
        assertThat(RocketMqMessageConverter.toDelayLevel(7000L)).isEqualTo(3);
        // 最长级别 2h，超出按最长处理
        assertThat(RocketMqMessageConverter.toDelayLevel(7200000L)).isEqualTo(18);
        assertThat(RocketMqMessageConverter.toDelayLevel(99999999L)).isEqualTo(18);
    }

    @Test
    void headersShouldIgnoreNullValue() {
        MqMessage message = MqMessage.of("t", "b");
        message.putHeader("k", null);
        assertThat(message.getHeaders()).isEmpty();

        Map<String, String> replaced = Map.of("a", "1");
        message.setHeaders(replaced);
        assertThat(message.getHeaders()).containsExactly(Map.entry("a", "1"));
        message.setHeaders(null);
        assertThat(message.getHeaders()).isEmpty();
    }
}
