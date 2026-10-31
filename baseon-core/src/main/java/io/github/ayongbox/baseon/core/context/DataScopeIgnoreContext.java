package io.github.ayongbox.baseon.core.context;

import java.util.function.Supplier;

/**
 * 数据范围忽略标记，由 starter-data-scope 的 AOP 切面在 {@code @IgnoreDataScope}
 * 方法执行期间写入，请求结束或方法退出时必须清理。
 *
 * <p>嵌套安全：外层已处于忽略态时，内层退出不会误清外层标记。
 * 本类在 baseon-core，零第三方依赖；仅承载线程内标记，
 * SQL 拦截器的实际跳过逻辑由 starter-data-scope 的切面完成。
 */
public final class DataScopeIgnoreContext {

    private static final ThreadLocal<Boolean> IGNORED = new ThreadLocal<>();

    private DataScopeIgnoreContext() {
    }

    public static void markIgnored() {
        IGNORED.set(Boolean.TRUE);
    }

    public static boolean isIgnored() {
        return Boolean.TRUE.equals(IGNORED.get());
    }

    public static void clearIgnored() {
        IGNORED.remove();
    }

    /**
     * 代码块内标记忽略并返回结果。
     *
     * <p>与 {@link #runIgnored(Runnable)} 不同名：避免无参 lambda 在
     * Supplier / Runnable 之间的类型推导歧义（同 pitfalls P-005）。
     */
    public static <T> T callIgnored(Supplier<T> action) {
        boolean wasIgnored = isIgnored();
        if (!wasIgnored) {
            markIgnored();
        }
        try {
            return action.get();
        } finally {
            if (!wasIgnored) {
                clearIgnored();
            }
        }
    }

    /** {@link #callIgnored(Supplier)} 的无返回值形态。 */
    public static void runIgnored(Runnable action) {
        callIgnored(() -> {
            action.run();
            return null;
        });
    }
}
