package io.github.ayongbox.baseon.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法/类不参与租户 SQL 过滤（MyBatis-Plus 租户插件忽略）。
 *
 * <p>典型场景：跨租户统计、字典表、系统级配置查询。由 starter-tenant 的切面识别，
 * 未引入 starter-tenant 时该注解不生效。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreTenant {
}
