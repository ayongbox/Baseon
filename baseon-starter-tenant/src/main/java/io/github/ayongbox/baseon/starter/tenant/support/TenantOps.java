package io.github.ayongbox.baseon.starter.tenant.support;

import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;

import java.util.function.Supplier;

/**
 * 租户忽略编程式入口：为定时任务、消息消费、跨租户批处理等<b>非 Web 场景</b>
 * 提供与 {@code @IgnoreTenant} / {@code @IgnoreTenantCache} 注解等价的代码块级能力。
 *
 * <p>{@code callWithSqlIgnored} 与注解切面保持同一套"双保险"：
 * MP 官方 {@link InterceptorIgnoreHelper} 让租户插件跳过 SQL 改写，
 * {@link TenantIgnoreContext} 标记让填充器暂停 tenantId 自动填充。
 *
 * <p>嵌套安全：外层已处于 SQL 忽略态（如外层方法标了 {@code @IgnoreTenant}）时，
 * 内层退出不会误清外层状态——判断依据正是 {@link TenantIgnoreContext#isSqlIgnored()}，
 * 因为所有进入忽略态的路径（注解切面与本类）都会同时写入该标记。
 */
public final class TenantOps {

    private TenantOps() {
    }

    /**
     * 代码块内关闭租户 SQL 过滤与 tenantId 自动填充，返回执行结果。
     * 适用于跨租户统计、全量数据同步等场景。
     */
    public static <T> T callWithSqlIgnored(Supplier<T> action) {
        if (TenantIgnoreContext.isSqlIgnored()) {
            // 外层已在忽略态（注解切面或外层 TenantOps），直接执行，不动外层状态
            return action.get();
        }
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
        TenantIgnoreContext.markSqlIgnored();
        try {
            return action.get();
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
            TenantIgnoreContext.clearSqlIgnored();
        }
    }

    /** {@link #callWithSqlIgnored(Supplier)} 的无返回值形态。 */
    public static void runWithSqlIgnored(Runnable action) {
        callWithSqlIgnored(() -> {
            action.run();
            return null;
        });
    }

    /**
     * 代码块内缓存 key 不加租户前缀（字典、配置等全租户共享缓存场景），返回执行结果。
     * 纯标记操作，直接委托 core。
     */
    public static <T> T callWithCacheIgnored(Supplier<T> action) {
        return TenantIgnoreContext.callCacheIgnored(action);
    }

    /** {@link #callWithCacheIgnored(Supplier)} 的无返回值形态。 */
    public static void runWithCacheIgnored(Runnable action) {
        TenantIgnoreContext.runCacheIgnored(action);
    }
}
