package io.github.ayongbox.baseon.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法/类需要做行级数据权限过滤。
 *
 * <p>打在 Service/Mapper 方法或类上，由 starter-data-scope 的切面识别，
 * 在执行期间自动往 SQL 注入数据范围条件（如 {@code dept_id IN (...)}）。
 * 数据范围取值由业务侧实现 {@code DataScopeProvider} 提供；未引入
 * starter-data-scope 时该注解不生效。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 过滤列名，默认取 {@code DataScopeProvider#column()}；
     * 显式指定时优先使用（同一表存在多个范围字段时按需覆盖）。
     */
    String column() default "";

    /**
     * 过滤的表别名：多表 JOIN 时指定条件拼到哪个表上，留空表示主表。
     */
    String tableAlias() default "";
}
