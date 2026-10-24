package io.github.ayongbox.baseon.core.context;

import java.util.function.Supplier;

/**
 * 租户忽略标记，由 starter-tenant 的 AOP 切面在 {@code @IgnoreTenant} /
 * {@code @IgnoreTenantCache} 方法执行期间写入，请求结束或方法退出时必须清理。
 *
 * <p>拆成两个独立标记的原因：SQL 过滤与缓存前缀是两个正交能力，
 * 忽略其一不应影响另一个。
 *
 * <p>除切面写入外，也提供编程式 API 供定时任务、消息消费等<b>非 Web 场景</b>使用。
 * 编程式入口嵌套安全：外层已处于忽略态时，内层退出不会误清外层标记。
 * 注意本类在 baseon-core，零第三方依赖——SQL 忽略若需让 MP 租户插件同步跳过，
 * 请使用 starter-tenant 的 {@code TenantOps}，不要直接调用本类。
 */
public final class TenantIgnoreContext {

    private static final ThreadLocal<Boolean> SQL_IGNORED = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CACHE_IGNORED = new ThreadLocal<>();

    private TenantIgnoreContext() {
    }

    public static void markSqlIgnored() {
        SQL_IGNORED.set(Boolean.TRUE);
    }

    public static boolean isSqlIgnored() {
        return Boolean.TRUE.equals(SQL_IGNORED.get());
    }

    public static void clearSqlIgnored() {
        SQL_IGNORED.remove();
    }

    /**
     * 代码块内标记 SQL 忽略并返回结果。仅设置本标记；MP 插件跳过需走
     * starter-tenant 的 {@code TenantOps.callWithSqlIgnored}。
     *
     * <p>与 {@link #runSqlIgnored(Runnable)} 不同名：避免无参 lambda 在
     * Supplier / Runnable 之间的类型推导歧义（见 pitfalls P-005）。
     */
    public static <T> T callSqlIgnored(Supplier<T> action) {
        boolean wasIgnored = isSqlIgnored();
        if (!wasIgnored) {
            markSqlIgnored();
        }
        try {
            return action.get();
        } finally {
            if (!wasIgnored) {
                clearSqlIgnored();
            }
        }
    }

    /** {@link #callSqlIgnored(Supplier)} 的无返回值形态。 */
    public static void runSqlIgnored(Runnable action) {
        callSqlIgnored(() -> {
            action.run();
            return null;
        });
    }

    public static void markCacheIgnored() {
        CACHE_IGNORED.set(Boolean.TRUE);
    }

    public static boolean isCacheIgnored() {
        return Boolean.TRUE.equals(CACHE_IGNORED.get());
    }

    public static void clearCacheIgnored() {
        CACHE_IGNORED.remove();
    }

    /**
     * 代码块内缓存 key 不加租户前缀（全租户共享缓存场景），返回结果。
     * 嵌套安全：外层已忽略时，内层退出不影响外层标记。
     */
    public static <T> T callCacheIgnored(Supplier<T> action) {
        boolean wasIgnored = isCacheIgnored();
        if (!wasIgnored) {
            markCacheIgnored();
        }
        try {
            return action.get();
        } finally {
            if (!wasIgnored) {
                clearCacheIgnored();
            }
        }
    }

    /** {@link #callCacheIgnored(Supplier)} 的无返回值形态。 */
    public static void runCacheIgnored(Runnable action) {
        callCacheIgnored(() -> {
            action.run();
            return null;
        });
    }
}
