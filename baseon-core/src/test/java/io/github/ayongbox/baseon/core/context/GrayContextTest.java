package io.github.ayongbox.baseon.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrayContextTest {

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Test
    void setAndGet() {
        GrayContext.set("gray");
        assertEquals("gray", GrayContext.get());
        assertTrue(GrayContext.hasTag());
    }

    @Test
    void setBlankEqualsClear() {
        GrayContext.set("gray");
        GrayContext.set(" ");
        assertNull(GrayContext.get());
        assertFalse(GrayContext.hasTag());
    }

    @Test
    void clearRemovesTag() {
        GrayContext.set("gray");
        GrayContext.clear();
        assertNull(GrayContext.get());
    }

    @Test
    void setTrimsTag() {
        GrayContext.set("  gray  ");
        assertEquals("gray", GrayContext.get());
    }
}
