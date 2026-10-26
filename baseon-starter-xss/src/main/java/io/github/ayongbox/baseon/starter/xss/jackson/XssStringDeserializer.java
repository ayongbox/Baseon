package io.github.ayongbox.baseon.starter.xss.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.github.ayongbox.baseon.starter.xss.support.XssEscapeUtil;

import java.io.IOException;

/**
 * Jackson String 反序列化器：JSON 请求体的字符串字段在绑定到对象前完成 HTML 转义。
 *
 * <p>覆盖 @RequestBody 入口——过滤器包装不到请求体反序列化过程，
 * 必须由 Jackson 层处理。注册为 {@code com.fasterxml.jackson.databind.Module} Bean 后，
 * Spring Boot 会自动纳入全局 ObjectMapper。
 */
public class XssStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return XssEscapeUtil.escape(parser.getValueAsString());
    }
}
