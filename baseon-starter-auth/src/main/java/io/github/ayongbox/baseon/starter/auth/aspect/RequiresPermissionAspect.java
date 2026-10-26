package io.github.ayongbox.baseon.starter.auth.aspect;

import io.github.ayongbox.baseon.core.annotation.RequiresPermission;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.secure.PermissionChecker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * {@code @RequiresPermission} 切面：先登录后鉴权。
 *
 * <p>未登录抛「未认证」（1002），已登录但权限不足抛「无访问权限」（1003）。
 * 切面运行在 Controller 调用链上，抛出的 BizException 由 starter-web 的
 * GlobalExceptionHandler 统一映射为 401/403。
 */
@Aspect
public class RequiresPermissionAspect {

    private final PermissionChecker permissionChecker;

    public RequiresPermissionAspect(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @Around("@annotation(requiresPermission)")
    public Object around(ProceedingJoinPoint pjp, RequiresPermission requiresPermission) throws Throwable {
        if (!SecurityUtil.isLogin()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        if (!permissionChecker.hasPermission(requiresPermission.value())) {
            throw new BizException(CommonErrorCode.FORBIDDEN);
        }
        return pjp.proceed();
    }
}
