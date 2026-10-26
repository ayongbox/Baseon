package io.github.ayongbox.baseon.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解：标注在 Controller 方法上，要求当前登录用户拥有指定权限串。
 *
 * <p>校验逻辑由 starter-auth 的切面提供；未引入 starter-auth 时本注解不生效。
 * 未登录调用抛「未认证」，已登录但权限不足抛「无访问权限」。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 权限标识，如 {@code "user:create"}。
     */
    String value();
}
