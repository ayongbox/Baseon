package io.github.ayongbox.baseon.starter.tenant.support;

import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantOpsTest {

    @AfterEach
    void tearDown() {
        TenantIgnoreContext.clearSqlIgnored();
        TenantIgnoreContext.clearCacheIgnored();
    }

    @Test
    void callWithSqlIgnoredMarksAndClears() {
        String result = TenantOps.callWithSqlIgnored(() -> {
            assertTrue(TenantIgnoreContext.isSqlIgnored());
            return "ok";
        });
        assertEquals("ok", result);
        assertFalse(TenantIgnoreContext.isSqlIgnored());
    }

    @Test
    void nestedCallKeepsOuterMark() {
        TenantIgnoreContext.markSqlIgnored();
        try {
            // 模拟外层已处于忽略态（如 @IgnoreTenant 方法内再次调用）
            String result = TenantOps.callWithSqlIgnored(() -> {
                assertTrue(TenantIgnoreContext.isSqlIgnored());
                return "inner";
            });
            assertEquals("inner", result);
            assertTrue(TenantIgnoreContext.isSqlIgnored());
        } finally {
            TenantIgnoreContext.clearSqlIgnored();
        }
    }

    @Test
    void runWithCacheIgnoredDelegatesToCore() {
        TenantOps.runWithCacheIgnored(
                () -> assertTrue(TenantIgnoreContext.isCacheIgnored()));
        assertFalse(TenantIgnoreContext.isCacheIgnored());
    }
}
