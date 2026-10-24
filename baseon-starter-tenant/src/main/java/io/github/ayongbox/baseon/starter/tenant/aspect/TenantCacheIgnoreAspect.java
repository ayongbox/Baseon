package io.github.ayongbox.baseon.starter.tenant.aspect;

import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * {@code @IgnoreTenantCache} 切面：方法执行期间缓存 key 不追加租户前缀。
 */
@Aspect
public class TenantCacheIgnoreAspect {

    @Around("@annotation(io.github.ayongbox.baseon.core.annotation.IgnoreTenantCache)"
            + " || @within(io.github.ayongbox.baseon.core.annotation.IgnoreTenantCache)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        TenantIgnoreContext.markCacheIgnored();
        try {
            return pjp.proceed();
        } finally {
            TenantIgnoreContext.clearCacheIgnored();
        }
    }
}
