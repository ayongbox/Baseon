package io.github.ayongbox.baseon.starter.auth.aspect;

import io.github.ayongbox.baseon.core.annotation.RequiresPermission;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.secure.PermissionChecker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequiresPermissionAspectTest {

    private PermissionChecker permissionChecker;
    private RequiresPermissionAspect aspect;

    @BeforeEach
    void setUp() {
        permissionChecker = mock(PermissionChecker.class);
        aspect = new RequiresPermissionAspect(permissionChecker);
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    private static RequiresPermission annotation(String value) {
        RequiresPermission annotation = mock(RequiresPermission.class);
        when(annotation.value()).thenReturn(value);
        return annotation;
    }

    @Test
    void proceedsWhenLoggedInAndPermitted() throws Throwable {
        SecurityUtil.set(new LoginUser());
        when(permissionChecker.hasPermission("user:create")).thenReturn(true);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("ok");

        Object result = aspect.around(pjp, annotation("user:create"));

        assertThat(result).isEqualTo("ok");
        verify(pjp).proceed();
    }

    @Test
    void throwsUnauthorizedWhenNotLogin() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

        assertThatThrownBy(() -> aspect.around(pjp, annotation("user:create")))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getCode()));
        verify(pjp, never()).proceed();
    }

    @Test
    void throwsForbiddenWhenPermissionDenied() throws Throwable {
        SecurityUtil.set(new LoginUser());
        when(permissionChecker.hasPermission("user:create")).thenReturn(false);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

        assertThatThrownBy(() -> aspect.around(pjp, annotation("user:create")))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.FORBIDDEN.getCode()));
        verify(pjp, never()).proceed();
    }
}
