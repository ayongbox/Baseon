package io.github.ayongbox.baseon.starter.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdUtilTest {

    @AfterEach
    void tearDown() {
        TraceIdUtil.clear();
    }

    @Test
    void should_generate_32_char_hex() {
        String traceId = TraceIdUtil.generate();
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        assertTrue(traceId.matches("[0-9a-f]{32}"));
    }

    @Test
    void should_set_and_get_trace_id() {
        String traceId = TraceIdUtil.set("abc123");
        assertEquals("abc123", traceId);
        assertEquals("abc123", TraceIdUtil.get());
    }

    @Test
    void should_generate_new_id_when_value_blank() {
        String traceId = TraceIdUtil.set("");
        assertNotNull(traceId);
        assertEquals(32, traceId.length());
        assertEquals(traceId, TraceIdUtil.get());
    }

    @Test
    void should_clear_mdc() {
        TraceIdUtil.set("abc123");
        TraceIdUtil.clear();
        assertNull(TraceIdUtil.get());
        assertNull(MDC.get(TraceIdUtil.TRACE_ID_KEY));
    }
}
