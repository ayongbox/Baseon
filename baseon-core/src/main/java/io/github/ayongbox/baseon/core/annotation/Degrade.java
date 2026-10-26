package io.github.ayongbox.baseon.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 降级策略注解：标注在远程调用/外部依赖方法上，方法抛异常时按策略兜底。
 *
 * <p>执行逻辑由 starter-resilience 的切面提供；未引入 starter-resilience 时本注解不生效。
 * 与 resilience4j 的 {@code @CircuitBreaker} 组合使用：熔断打开后调用快速失败，
 * 本注解负责失败后的取值兜底。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Degrade {

    /**
     * 降级策略，默认快速失败（抛「服务已降级」业务异常）。
     */
    DegradeStrategy strategy() default DegradeStrategy.FAIL_FAST;
}
