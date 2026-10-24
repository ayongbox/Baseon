package io.github.ayongbox.baseon.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法/类的缓存 key 不追加租户前缀。
 *
 * <p>典型场景：全租户共享的字典、配置缓存。由 starter-tenant 的切面识别，
 * 未引入 starter-tenant 时该注解不生效。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreTenantCache {
}
