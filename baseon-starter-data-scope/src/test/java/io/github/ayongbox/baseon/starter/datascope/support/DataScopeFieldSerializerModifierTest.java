package io.github.ayongbox.baseon.starter.datascope.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ayongbox.baseon.core.spi.DataScopeProvider;
import io.github.ayongbox.baseon.starter.datascope.support.DataScopeFieldSerializerModifier.DataScopeFilteringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataScopeFieldSerializerModifier} 序列化过滤验证：
 * 不同 DataScopeContext / visibleFields 组合下输出字段是否符合预期。
 */
class DataScopeFieldSerializerModifierTest {

    private final DataScopeProvider provider = new DataScopeProvider() {
        @Override
        public Set<Long> visibleIds() {
            return null;
        }

        @Override
        public Set<String> visibleFields() {
            return Set.of("id", "name");
        }
    };
    private final DataScopeFieldSerializerModifier modifier = new DataScopeFieldSerializerModifier(provider);

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
    }

    @Test
    void shouldReturnOriginalSerializerWhenNotBeanSerializer() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonSerializerStub stub = new JsonSerializerStub();
        var result = modifier.modifySerializer(null, null, stub);
        assertThat(result).isSameAs(stub);
    }

    @Test
    void shouldWrapBeanSerializerAndFilterFieldsWhenContextActive() throws Exception {
        DataScopeContext.set(DataScopeContext.of("dept_id", null, Set.of(1L)));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
                .setSerializerModifier(modifier));

        DemoPojo pojo = new DemoPojo();
        pojo.id = 1L;
        pojo.name = "alice";
        pojo.secret = "s3cr3t";

        String json = mapper.writeValueAsString(pojo);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"name\":\"alice\"");
        assertThat(json).doesNotContain("secret");
        assertThat(json).doesNotContain("s3cr3t");
    }

    @Test
    void shouldSerializeAllFieldsWhenNoDataScopeContext() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
                .setSerializerModifier(modifier));

        DemoPojo pojo = new DemoPojo();
        pojo.id = 1L;
        pojo.name = "alice";
        pojo.secret = "s3cr3t";

        String json = mapper.writeValueAsString(pojo);
        assertThat(json).contains("\"secret\":\"s3cr3t\"");
    }

    @Test
    void shouldSerializeAllFieldsWhenVisibleFieldsNull() throws Exception {
        DataScopeContext.set(DataScopeContext.of("dept_id", null, Set.of(1L)));
        DataScopeProvider nullFieldsProvider = new DataScopeProvider() {
            @Override
            public Set<Long> visibleIds() {
                return null;
            }

            @Override
            public Set<String> visibleFields() {
                return null;
            }
        };
        DataScopeFieldSerializerModifier nullModifier = new DataScopeFieldSerializerModifier(nullFieldsProvider);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
                .setSerializerModifier(nullModifier));

        DemoPojo pojo = new DemoPojo();
        pojo.secret = "s3cr3t";

        String json = mapper.writeValueAsString(pojo);
        assertThat(json).contains("\"secret\":\"s3cr3t\"");
    }

    /** 测试用 POJO：仅 public 字段即可 */
    static class DemoPojo {
        public Long id;
        public String name;
        public String secret;
    }

    /** 非 BeanSerializer 的桩实现：验证 modifySerializer 直接返回原样 */
    static class JsonSerializerStub extends com.fasterxml.jackson.databind.JsonSerializer<Object> {
        @Override
        public void serialize(Object value, com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider serializers) {
        }
    }
}
