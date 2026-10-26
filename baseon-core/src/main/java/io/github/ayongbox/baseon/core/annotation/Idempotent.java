package io.github.ayongbox.baseon.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等控制注解：标注在 Controller 方法上，防止重复提交产生重复数据。
 *
 * <p>占位逻辑由 starter-idempotent 的切面提供（Redis SET NX EX）；
 * 未引入 starter-idempotent 时本注解不生效。占位窗口内重复请求抛「重复请求」，
 * 窗口过期后自动放行，无后台清理任务。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等 key 的 SpEL 表达式，基于方法参数求值，如 {@code "#name"}、{@code "#req.orderNo"}。
     * 留空时改从请求头读取幂等令牌（header 名由 {@code baseon.idempotent.token-header} 配置）。
     */
    String key() default "";

    /**
     * 占位窗口（秒）。0 表示使用 {@code baseon.idempotent.default-ttl-seconds}。
     */
    long ttlSeconds() default 0;

    /**
     * 重复请求时的提示语，留空使用统一错误码默认文案。
     */
    String message() default "";
}
