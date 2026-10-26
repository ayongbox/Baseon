package io.github.ayongbox.baseon.starter.resilience.aspect;

import io.github.ayongbox.baseon.core.annotation.Degrade;
import io.github.ayongbox.baseon.core.annotation.DegradeStrategy;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.starter.resilience.support.DegradeResultCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DegradeAspectTest {

    private static final String DECLARING_TYPE = "com.example.DemoClient";
    private static final String METHOD_NAME = "call";

    private final DegradeResultCache cache = new DegradeResultCache(100, 300);
    private final DegradeAspect aspect = new DegradeAspect(cache);

    /** FAIL_FAST：抛 9003 且原异常保留为 cause */
    @Test
    void failFastThrowsServiceDegradedWithCause() {
        RuntimeException original = new RuntimeException("boom");
        ProceedingJoinPoint pjp = mockFailingPjp(String.class, new Object[]{"a"}, original);

        assertThatThrownBy(() -> aspect.around(pjp, degrade(DegradeStrategy.FAIL_FAST)))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(9003);
                    assertThat(ex.getCause()).isSameAs(original);
                });
    }

    /** CACHE：首次失败且缓存未命中，抛 9003 */
    @Test
    void cacheMissOnFirstFailureThrowsServiceDegraded() {
        RuntimeException original = new RuntimeException("boom");
        ProceedingJoinPoint pjp = mockFailingPjp(String.class, new Object[]{"a"}, original);

        assertThatThrownBy(() -> aspect.around(pjp, degrade(DegradeStrategy.CACHE)))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCode()).isEqualTo(9003));
    }

    /** CACHE：成功写入缓存后失败，返回缓存的最近一次成功结果 */
    @Test
    void cacheReturnsLastSuccessResultOnFailure() throws Throwable {
        Degrade degrade = degrade(DegradeStrategy.CACHE);
        Object[] args = new Object[]{"a"};

        ProceedingJoinPoint success = mockSuccessPjp(String.class, args, "ok");
        assertThat(aspect.around(success, degrade)).isEqualTo("ok");

        ProceedingJoinPoint failing = mockFailingPjp(String.class, args, new RuntimeException("boom"));
        assertThat(aspect.around(failing, degrade)).isEqualTo("ok");
    }

    /** CACHE：缓存过期后失败，抛 9003 */
    @Test
    void cacheExpiredEntryLeadsToServiceDegraded() throws Throwable {
        DegradeResultCache shortLivedCache = new DegradeResultCache(100, 1);
        DegradeAspect shortLivedAspect = new DegradeAspect(shortLivedCache);
        Degrade degrade = degrade(DegradeStrategy.CACHE);
        Object[] args = new Object[]{"a"};

        ProceedingJoinPoint success = mockSuccessPjp(String.class, args, "ok");
        assertThat(shortLivedAspect.around(success, degrade)).isEqualTo("ok");

        Thread.sleep(1100);

        ProceedingJoinPoint failing = mockFailingPjp(String.class, args, new RuntimeException("boom"));
        assertThatThrownBy(() -> shortLivedAspect.around(failing, degrade))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCode()).isEqualTo(9003));
    }

    /** DEFAULT：引用类型返回 null */
    @Test
    void defaultStrategyReturnsNullForReferenceType() throws Throwable {
        ProceedingJoinPoint pjp = mockFailingPjp(String.class, new Object[]{"a"}, new RuntimeException("boom"));

        Object result = aspect.around(pjp, degrade(DegradeStrategy.DEFAULT));

        assertThat(result).isNull();
    }

    /** DEFAULT：原始类型 int 返回 0 */
    @Test
    void defaultStrategyReturnsZeroForPrimitiveInt() throws Throwable {
        ProceedingJoinPoint pjp = mockFailingPjp(int.class, new Object[]{"a"}, new RuntimeException("boom"));

        Object result = aspect.around(pjp, degrade(DegradeStrategy.DEFAULT));

        assertThat(result).isEqualTo(0);
    }

    /** DEFAULT：原始类型 boolean 返回 false */
    @Test
    void defaultStrategyReturnsFalseForPrimitiveBoolean() throws Throwable {
        ProceedingJoinPoint pjp = mockFailingPjp(boolean.class, new Object[]{"a"}, new RuntimeException("boom"));

        Object result = aspect.around(pjp, degrade(DegradeStrategy.DEFAULT));

        assertThat(result).isEqualTo(false);
    }

    private Degrade degrade(DegradeStrategy strategy) {
        Degrade degrade = mock(Degrade.class);
        when(degrade.strategy()).thenReturn(strategy);
        return degrade;
    }

    private ProceedingJoinPoint mockSuccessPjp(Class<?> returnType, Object[] args, Object result) throws Throwable {
        ProceedingJoinPoint pjp = mockPjp(returnType, args);
        when(pjp.proceed()).thenReturn(result);
        return pjp;
    }

    private ProceedingJoinPoint mockFailingPjp(Class<?> returnType, Object[] args, Throwable throwable) {
        ProceedingJoinPoint pjp = mockPjp(returnType, args);
        try {
            when(pjp.proceed()).thenThrow(throwable);
        } catch (Throwable ignored) {
            // thenThrow 的受检异常声明，mock 场景不会真正抛出
        }
        return pjp;
    }

    private ProceedingJoinPoint mockPjp(Class<?> returnType, Object[] args) {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getDeclaringTypeName()).thenReturn(DECLARING_TYPE);
        when(signature.getName()).thenReturn(METHOD_NAME);
        when(signature.getReturnType()).thenAnswer(inv -> returnType);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(args);
        return pjp;
    }
}
