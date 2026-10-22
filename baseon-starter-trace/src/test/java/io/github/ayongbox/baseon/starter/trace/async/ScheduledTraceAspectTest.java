package io.github.ayongbox.baseon.starter.trace.async;

import io.github.ayongbox.baseon.starter.trace.TraceIdUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduledTraceAspectTest {

    private final ScheduledTraceAspect aspect = new ScheduledTraceAspect();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void should_generate_trace_id_around_scheduled_method() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Scheduled scheduled = mock(Scheduled.class);

        when(joinPoint.proceed()).thenAnswer(invocation -> {
            String traceId = MDC.get(TraceIdUtil.TRACE_ID_KEY);
            assertNotNull(traceId);
            assertEquals(32, traceId.length());
            return "done";
        });

        Object result = aspect.around(joinPoint, scheduled);

        assertEquals("done", result);
        verify(joinPoint).proceed();
        assertNull(MDC.get(TraceIdUtil.TRACE_ID_KEY));
    }

    @Test
    void should_clear_trace_id_even_when_scheduled_method_throws() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Scheduled scheduled = mock(Scheduled.class);

        when(joinPoint.proceed()).thenThrow(new RuntimeException("job failed"));

        assertThrows(RuntimeException.class, () -> aspect.around(joinPoint, scheduled));
        assertNull(MDC.get(TraceIdUtil.TRACE_ID_KEY));
    }
}
