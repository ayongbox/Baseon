package io.github.ayongbox.baseon.starter.datasource.aspect;

import io.github.ayongbox.baseon.starter.datasource.annotation.DS;
import io.github.ayongbox.baseon.starter.datasource.support.DataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataSourceSwitchAspectTest {

    private final DataSourceSwitchAspect aspect = new DataSourceSwitchAspect();

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clear();
    }

    @Test
    void setsDataSourceAroundMethodLevelAnnotationAndClearsAfter() throws Throwable {
        AtomicReference<String> duringProceed = new AtomicReference<>();
        ProceedingJoinPoint pjp = pjp(MethodLevelTarget.class.getMethod("slaveMethod"),
                MethodLevelTarget.class, duringProceed);

        Object result = aspect.around(pjp);

        assertThat(result).isEqualTo("ok");
        assertThat(duringProceed.get()).isEqualTo("slave1");
        assertThat(DataSourceContextHolder.get()).isNull();
    }

    @Test
    void fallsBackToClassLevelAnnotation() throws Throwable {
        AtomicReference<String> duringProceed = new AtomicReference<>();
        ProceedingJoinPoint pjp = pjp(ClassLevelTarget.class.getMethod("doSomething"),
                ClassLevelTarget.class, duringProceed);

        aspect.around(pjp);

        assertThat(duringProceed.get()).isEqualTo("slave2");
        assertThat(DataSourceContextHolder.get()).isNull();
    }

    @Test
    void methodLevelAnnotationWinsOverClassLevel() throws Throwable {
        AtomicReference<String> duringProceed = new AtomicReference<>();
        ProceedingJoinPoint pjp = pjp(BothLevelTarget.class.getMethod("masterMethod"),
                BothLevelTarget.class, duringProceed);

        aspect.around(pjp);

        assertThat(duringProceed.get()).isEqualTo("master");
    }

    @Test
    void clearsContextEvenWhenProceedThrows() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(MethodLevelTarget.class.getMethod("slaveMethod"));
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.around(pjp)).isInstanceOf(IllegalStateException.class);
        assertThat(DataSourceContextHolder.get()).isNull();
    }

    /**
     * 构造一个 proceed 时捕获当前线程数据源名的 mock 连接点。
     */
    private ProceedingJoinPoint pjp(java.lang.reflect.Method method, Class<?> declaringType,
                                    AtomicReference<String> duringProceed) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringType()).thenAnswer(invocation -> declaringType);
        when(pjp.proceed()).thenAnswer(invocation -> {
            duringProceed.set(DataSourceContextHolder.get());
            return "ok";
        });
        return pjp;
    }

    static class MethodLevelTarget {

        @DS("slave1")
        public String slaveMethod() {
            return "slave";
        }
    }

    @DS("slave2")
    static class ClassLevelTarget {

        public String doSomething() {
            return "class";
        }
    }

    @DS("slave2")
    static class BothLevelTarget {

        @DS("master")
        public String masterMethod() {
            return "master";
        }
    }
}
