package io.github.ayongbox.baseon.starter.datasource.support;

/**
 * 当前线程显式指定的数据源名，纯 ThreadLocal，零 Spring 依赖。
 *
 * <p>写入与清理由 {@code DataSourceSwitchAspect} 负责。由于线程池会复用线程，
 * <b>任何手写 set 的场景都必须在 finally 中调用 {@link #clear()}</b>，
 * 否则残留的数据源名会串号到复用该线程的后续任务，造成读写打错库。
 */
public final class DataSourceContextHolder {

    /**
     * 只读路由标记：由 {@code ReadOnlyRouteAspect} 在只读事务开启前写入，
     * 路由数据源读到该标记后轮询从库。与具体数据源名共存于同一 ThreadLocal，
     * 保证「@DS 显式指定」天然优先于只读标记（同一线程只会写入一个值）。
     */
    public static final String READ_ONLY = "__READ_ONLY__";

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    /**
     * 写入当前线程的数据源名（如 master / slave1）；null 或空白等价于 {@link #clear()}。
     */
    public static void set(String name) {
        if (name == null || name.isBlank()) {
            CONTEXT.remove();
        } else {
            CONTEXT.set(name.trim());
        }
    }

    /**
     * 读取当前线程的数据源名，未设置时返回 null。
     */
    public static String get() {
        return CONTEXT.get();
    }

    /**
     * 清除当前线程的数据源名，路由逻辑随后回退到读写分离 / 主库规则。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
