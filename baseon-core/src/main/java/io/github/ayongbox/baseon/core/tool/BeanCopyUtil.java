package io.github.ayongbox.baseon.core.tool;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 轻量 Bean 拷贝工具，仅用于内核层无框架场景。
 *
 * <p>业务层的对象转换统一使用 MapStruct（编译期生成，无反射开销），
 * 本工具只解决框架内部少量属性拷贝，不对外推荐使用。
 */
public final class BeanCopyUtil {

    private BeanCopyUtil() {
    }

    public static <T> T copy(Object source, Class<T> targetType) {
        Objects.requireNonNull(targetType, "targetType");
        try {
            T target = targetType.getDeclaredConstructor().newInstance();
            copy(source, target);
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法实例化目标类型: " + targetType.getName(), e);
        }
    }

    /**
     * 按字段名拷贝，要求同名且目标字段类型可赋值；getter/setter 缺失的场景也能工作。
     */
    public static void copy(Object source, Object target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");

        Map<String, Field> targetFields = collectFields(target.getClass());
        for (Field sourceField : collectFields(source.getClass()).values()) {
            Field targetField = targetFields.get(sourceField.getName());
            if (targetField == null || !targetField.getType().isAssignableFrom(sourceField.getType())) {
                continue;
            }
            try {
                targetField.set(target, sourceField.get(source));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("字段拷贝失败: " + sourceField.getName(), e);
            }
        }
    }

    private static Map<String, Field> collectFields(Class<?> type) {
        Map<String, Field> fields = new LinkedHashMap<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                fields.putIfAbsent(field.getName(), field);
            }
        }
        return fields;
    }
}
