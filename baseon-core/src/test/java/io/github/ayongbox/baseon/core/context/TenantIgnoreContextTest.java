package io.github.ayongbox.baseon.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantIgnoreContextTest {

    @AfterEach
    void tearDown() {
        TenantIgnoreContext.clearSqlIgnored();
        TenantIgnoreContext.clearCacheIgnored();
    }

    @Test
    void callSqlIgnoredAppliesWithinBlockAndReturns() {
        String result = TenantIgnoreContext.callSqlIgnored(() -> {
            assertTrue(TenantIgnoreContext.isSqlIgnored());
            return "ok";
        });
        assertEquals("ok", result);
        assertFalse(TenantIgnoreContext.isSqlIgnored());
    }

    @Test
    void runSqlIgnoredAppliesWithinBlock() {
        TenantIgnoreContext.runSqlIgnored(
                () -> assertTrue(TenantIgnoreContext.isSqlIgnored()));
        assertFalse(TenantIgnoreContext.isSqlIgnored());
    }

    @Test
    void nestedSqlIgnoredKeepsOuterMark() {
        TenantIgnoreContext.markSqlIgnored();
        try {
            TenantIgnoreContext.runSqlIgnored(
                    () -> assertTrue(TenantIgnoreContext.isSqlIgnored()));
            assertTrue(TenantIgnoreContext.isSqlIgnored());
        } finally {
            TenantIgnoreContext.clearSqlIgnored();
        }
    }

    @Test
    void callCacheIgnoredAppliesAndIsOrthogonalToSql() {
        String result = TenantIgnoreContext.callCacheIgnored(() -> {
            assertTrue(TenantIgnoreContext.isCacheIgnored());
            assertFalse(TenantIgnoreContext.isSqlIgnored());
            return "ok";
        });
        assertEquals("ok", result);
        assertFalse(TenantIgnoreContext.isCacheIgnored());
    }

    @Test
    void nestedCacheIgnoredKeepsOuterMark() {
        TenantIgnoreContext.markCacheIgnored();
        try {
            TenantIgnoreContext.runCacheIgnored(
                    () -> assertTrue(TenantIgnoreContext.isCacheIgnored()));
            assertTrue(TenantIgnoreContext.isCacheIgnored());
        } finally {
            TenantIgnoreContext.clearCacheIgnored();
        }
    }
}
