package io.github.ayongbox.baseon.starter.trace.async;

import io.github.ayongbox.baseon.starter.trace.TraceIdUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void should_copy_mdc_to_decorated_task() {
        MDC.put(TraceIdUtil.TRACE_ID_KEY, "parent-trace");
        MDC.put("userId", "1001");

        AtomicReference<String> seenTraceId = new AtomicReference<>();
        AtomicReference<String> seenUserId = new AtomicReference<>();

        Runnable decorated = decorator.decorate(() -> {
            seenTraceId.set(MDC.get(TraceIdUtil.TRACE_ID_KEY));
            seenUserId.set(MDC.get("userId"));
        });

        // 主线程修改 MDC 不影响已捕获的快照
        MDC.put(TraceIdUtil.TRACE_ID_KEY, "changed");
        decorated.run();

        assertEquals("parent-trace", seenTraceId.get());
        assertEquals("1001", seenUserId.get());
    }

    @Test
    void should_restore_previous_mdc_after_execution() {
        MDC.put(TraceIdUtil.TRACE_ID_KEY, "outer");

        Runnable decorated = decorator.decorate(() -> MDC.put(TraceIdUtil.TRACE_ID_KEY, "inner"));
        decorated.run();

        assertEquals("outer", MDC.get(TraceIdUtil.TRACE_ID_KEY));
    }

    @Test
    void should_clear_mdc_when_no_previous_context() {
        MDC.clear();

        AtomicReference<String> seen = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> seen.set(MDC.get(TraceIdUtil.TRACE_ID_KEY)));
        decorated.run();

        assertNull(seen.get());
        assertNull(MDC.get(TraceIdUtil.TRACE_ID_KEY));
    }
}
