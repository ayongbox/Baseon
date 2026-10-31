package io.github.ayongbox.baseon.starter.datascope.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import io.github.ayongbox.baseon.core.spi.DataScopeProvider;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * 字段级数据权限序列化修改器：把默认 BeanSerializer 包装为动态裁剪版本，
 * 每次序列化时根据当前线程的 {@link DataScopeContext} 与
 * {@link DataScopeProvider#visibleFields()} 决定输出哪些字段。
 *
 * <p>生效条件（同时满足）：
 * <ul>
 *   <li>{@code baseon.data-scope.field-filter-enabled=true}</li>
 *   <li>当前线程处于 @DataScope 方法执行期间（{@code DataScopeContext != null}）</li>
 *   <li>{@link DataScopeProvider#visibleFields()} 返回非 null 集合</li>
 * </ul>
 *
 * <p>关键点：Jackson 会缓存 BeanSerializer，因此字段裁剪必须放在每次
 * 序列化调用时动态求值（包装一层 JsonSerializer），不能在 changeProperties
 * 阶段裁剪（那样只会在首次构建时生效一次）。
 */
public class DataScopeFieldSerializerModifier extends BeanSerializerModifier {

    private final DataScopeProvider dataScopeProvider;

    public DataScopeFieldSerializerModifier(DataScopeProvider dataScopeProvider) {
        this.dataScopeProvider = dataScopeProvider;
    }

    @Override
    public JsonSerializer<?> modifySerializer(
            SerializationConfig config,
            BeanDescription beanDesc,
            JsonSerializer<?> serializer) {
        if (!(serializer instanceof BeanSerializerBase)) {
            return serializer;
        }
        return new DataScopeFilteringSerializer((BeanSerializerBase) serializer, dataScopeProvider);
    }

    /**
     * 动态字段过滤序列化器：serialize 调用时按当前 DataScopeContext 决定输出字段。
     */
    static final class DataScopeFilteringSerializer extends JsonSerializer<Object> {

        private final BeanSerializerBase delegate;
        private final DataScopeProvider dataScopeProvider;

        DataScopeFilteringSerializer(BeanSerializerBase delegate, DataScopeProvider dataScopeProvider) {
            this.delegate = delegate;
            this.dataScopeProvider = dataScopeProvider;
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            Set<String> visibleFields = currentVisibleFields();
            if (visibleFields == null) {
                delegate.serialize(value, gen, serializers);
                return;
            }
            gen.writeStartObject();
            Iterator<PropertyWriter> it = delegate.properties();
            while (it.hasNext()) {
                PropertyWriter writer = it.next();
                if (writer instanceof BeanPropertyWriter && visibleFields.contains(writer.getName())) {
                    try {
                        ((BeanPropertyWriter) writer).serializeAsField(value, gen, serializers);
                    } catch (Exception e) {
                        throw new IOException("字段序列化失败: " + writer.getName(), e);
                    }
                }
            }
            gen.writeEndObject();
        }

        /** 当前请求需要过滤时返回可见字段集；不需要过滤时返回 null */
        private Set<String> currentVisibleFields() {
            if (DataScopeContext.get() == null) {
                return null;
            }
            return dataScopeProvider.visibleFields();
        }
    }
}
