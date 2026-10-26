package io.github.ayongbox.baseon.starter.xss.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * XssStringDeserializer 的 JSON 字符串字段转义测试。
 */
class XssStringDeserializerTest {

    @Test
    void escapesStringFieldWhenDeserializing() throws Exception {
        // 模拟自动装配：SimpleModule 注册 String 反序列化器后纳入 ObjectMapper
        SimpleModule module = new SimpleModule("baseon-xss");
        module.addDeserializer(String.class, new XssStringDeserializer());
        ObjectMapper mapper = new ObjectMapper().registerModule(module);

        EchoBean bean = mapper.readValue("{\"content\":\"<script>alert(1)</script>\"}", EchoBean.class);

        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", bean.content);
    }

    /**
     * 反序列化目标对象，字段公开便于断言。
     */
    static class EchoBean {

        public String content;
    }
}
