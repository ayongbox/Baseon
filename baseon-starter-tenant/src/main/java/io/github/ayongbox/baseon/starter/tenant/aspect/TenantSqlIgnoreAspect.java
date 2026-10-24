package io.github.ayongbox.baseon.starter.tenant.aspect;

import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * {@code @IgnoreTenant} 切面：方法执行期间关闭 MP 租户插件。
 *
 * <p>双保险：MP 官方 {@link InterceptorIgnoreHelper} 负责让 TenantLineInnerInterceptor
 * 跳过条件拼接；{@link TenantIgnoreContext#markSqlIgnored()} 供填充器（MetaObjectHandler）
 * 识别，避免 @IgnoreTenant 的写入操作仍被自动填充 tenantId。
 */
@Aspect
public class TenantSqlIgnoreAspect {

    @Around("@annotation(io.github.ayongbox.baseon.core.annotation.IgnoreTenant)"
            + " || @within(io.github.ayongbox.baseon.core.annotation.IgnoreTenant)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
        TenantIgnoreContext.markSqlIgnored();
        try {
            return pjp.proceed();
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
            TenantIgnoreContext.clearSqlIgnored();
        }
    }
}
