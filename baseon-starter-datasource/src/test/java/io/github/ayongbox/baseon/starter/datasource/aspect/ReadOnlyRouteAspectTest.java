package io.github.ayongbox.baseon.starter.datasource.aspect;

import io.github.ayongbox.baseon.starter.datasource.support.DataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReadOnlyRouteAspectTest {

    private final ReadOnlyRouteAspect aspect = new ReadOnlyRouteAspect();

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clear();
    }

    @SuppressWarnings("unused")
    static class DemoService {

        @Transactional(readOnly = true)
        public String readOnlyMethod() {
            return "ro";
        }

        @Transactional
        public String writeMethod() {
            return "w";
        }

        public String noAnnotation() {
            return "plain";
        }
    }

    private ProceedingJoinPoint pjpFor(String methodName) throws Exception {
        Method method = DemoService.class.getMethod(methodName);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getTarget()).thenReturn(new DemoService());
        return pjp;
    }

    @Test
    void marksReadOnlyBeforeTransactionAndClearsAfter() throws Throwable {
        ProceedingJoinPoint pjp = pjpFor("readOnlyMethod");
        when(pjp.proceed()).thenAnswer(inv -> {
            assertThat(DataSourceContextHolder.get()).isEqualTo(DataSourceContextHolder.READ_ONLY);
            return "ro";
        });

        Object result = aspect.around(pjp);

        assertThat(result).isEqualTo("ro");
        assertThat(DataSourceContextHolder.get()).isNull();
    }

    @Test
    void writeTransactionIsNotMarked() throws Throwable {
        ProceedingJoinPoint pjp = pjpFor("writeMethod");
        when(pjp.proceed()).thenAnswer(inv -> {
            assertThat(DataSourceContextHolder.get()).isNull();
            return "w";
        });

        aspect.around(pjp);

        verify(pjp).proceed();
    }

    @Test
    void explicitDsTakesPrecedenceOverReadOnly() throws Throwable {
        DataSourceContextHolder.set("slave1");
        ProceedingJoinPoint pjp = pjpFor("readOnlyMethod");
        when(pjp.proceed()).thenAnswer(inv -> {
            assertThat(DataSourceContextHolder.get()).isEqualTo("slave1");
            return "ro";
        });

        aspect.around(pjp);

        // 切面不应覆盖也不应清理 @DS 写入的上下文
        assertThat(DataSourceContextHolder.get()).isEqualTo("slave1");
    }

    @Test
    void noAnnotationIsNotMarked() throws Throwable {
        ProceedingJoinPoint pjp = pjpFor("noAnnotation");
        when(pjp.proceed()).thenAnswer(inv -> {
            assertThat(DataSourceContextHolder.get()).isNull();
            return "plain";
        });

        aspect.around(pjp);

        verify(pjp).proceed();
    }
}
