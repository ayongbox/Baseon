package io.github.ayongbox.baseon.starter.datascope.support;

import java.util.Set;

/**
 * 当前请求的数据范围上下文：由 DataScopeAspect 在 @DataScope 方法执行期间写入，
 * DataScopeLineInnerInterceptor 在 SQL 改写时读取；方法退出必须清理。
 *
 * <p>设计上一次请求只允许一个生效的数据范围（不支持嵌套覆盖）：嵌套场景下
 * 内层注解的 column/alias 覆盖外层，由切面负责进出栈管理。
 */
public final class DataScopeContext {

    private static final ThreadLocal<DataScopeContext> CURRENT = new ThreadLocal<>();

    private final String column;
    private final String tableAlias;
    private final Set<Long> visibleIds;

    private DataScopeContext(String column, String tableAlias, Set<Long> visibleIds) {
        this.column = column;
        this.tableAlias = tableAlias;
        this.visibleIds = visibleIds;
    }

    public static DataScopeContext of(String column, String tableAlias, Set<Long> visibleIds) {
        return new DataScopeContext(column, tableAlias, visibleIds);
    }

    public static void set(DataScopeContext context) {
        CURRENT.set(context);
    }

    public static DataScopeContext get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public String column() {
        return column;
    }

    public String tableAlias() {
        return tableAlias;
    }

    /** 可见 ID 集合；null 表示不限制（放行），空集表示查不到数据 */
    public Set<Long> visibleIds() {
        return visibleIds;
    }
}
