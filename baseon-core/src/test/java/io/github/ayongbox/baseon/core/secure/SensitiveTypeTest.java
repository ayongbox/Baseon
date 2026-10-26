package io.github.ayongbox.baseon.core.secure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveTypeTest {

    @Test
    void shouldDeclareExactlyFourLevelsInAscendingOrder() {
        SensitiveType[] levels = SensitiveType.values();
        assertEquals(4, levels.length);
        assertEquals(SensitiveType.L1_PUBLIC, levels[0]);
        assertEquals(SensitiveType.L2_INTERNAL, levels[1]);
        assertEquals(SensitiveType.L3_CONFIDENTIAL, levels[2]);
        assertEquals(SensitiveType.L4_SECRET, levels[3]);
    }

    @Test
    void shouldResolveByName() {
        assertEquals(SensitiveType.L3_CONFIDENTIAL, SensitiveType.valueOf("L3_CONFIDENTIAL"));
        assertEquals(SensitiveType.L4_SECRET, SensitiveType.valueOf("L4_SECRET"));
    }
}
