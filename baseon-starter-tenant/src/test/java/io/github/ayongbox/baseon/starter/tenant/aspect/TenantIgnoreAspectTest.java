package io.github.ayongbox.baseon.starter.tenant.aspect;

import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantIgnoreAspectTest {

    @AfterEach
    void tearDown() {
        TenantIgnoreContext.clearSqlIgnored();
        TenantIgnoreContext.clearCacheIgnored();
    }

    @Test
    void sqlIgnoreAspectMarksDuringMethodAndClearsAfter() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenAnswer(inv -> {
            assertThat(TenantIgnoreContext.isSqlIgnored()).isTrue();
            return "ok";
        });

        Object result = new TenantSqlIgnoreAspect().around(pjp);

        assertThat(result).isEqualTo("ok");
        verify(pjp).proceed();
        assertThat(TenantIgnoreContext.isSqlIgnored()).isFalse();
    }

    @Test
    void sqlIgnoreAspectClearsOnException() {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        try {
            when(pjp.proceed()).thenThrow(new RuntimeException("boom"));
        } catch (Throwable ignored) {
        }

        try {
            new TenantSqlIgnoreAspect().around(pjp);
        } catch (Throwable ignored) {
        }

        assertThat(TenantIgnoreContext.isSqlIgnored()).isFalse();
    }

    @Test
    void cacheIgnoreAspectMarksDuringMethodAndClearsAfter() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenAnswer(inv -> {
            assertThat(TenantIgnoreContext.isCacheIgnored()).isTrue();
            return "ok";
        });

        new TenantCacheIgnoreAspect().around(pjp);

        assertThat(TenantIgnoreContext.isCacheIgnored()).isFalse();
    }
}
