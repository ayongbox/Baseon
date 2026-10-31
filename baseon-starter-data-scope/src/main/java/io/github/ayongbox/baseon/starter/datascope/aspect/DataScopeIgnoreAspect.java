package io.github.ayongbox.baseon.starter.datascope.aspect;

import io.github.ayongbox.baseon.core.context.DataScopeIgnoreContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * {@code @IgnoreDataScope} 切面：方法执行期间标记忽略，
 * {@code DataScopeAspect} 与 SQL 拦截器读取 {@link DataScopeIgnoreContext#isIgnored()}
 * 跳过数据范围注入。退出时清理，嵌套安全由 {@link DataScopeIgnoreContext} 自身保证。
 */
@Aspect
public class DataScopeIgnoreAspect {

    @Around("@annotation(io.github.ayongbox.baseon.core.annotation.IgnoreDataScope)"
            + " || @within(io.github.ayongbox.baseon.core.annotation.IgnoreDataScope)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        boolean wasIgnored = DataScopeIgnoreContext.isIgnored();
        if (!wasIgnored) {
            DataScopeIgnoreContext.markIgnored();
        }
        try {
            return pjp.proceed();
        } finally {
            if (!wasIgnored) {
                DataScopeIgnoreContext.clearIgnored();
            }
        }
    }
}
