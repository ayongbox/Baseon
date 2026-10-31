package io.github.ayongbox.baseon.test.core;

import io.github.ayongbox.baseon.core.context.DataScopeIgnoreContext;
import io.github.ayongbox.baseon.core.context.GrayContext;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import org.junit.jupiter.api.AfterEach;

/**
 * 单元测试基类：每个测试方法结束后统一清理线程上下文，
 * 避免 {@link SecurityUtil} / 租户忽略 / 数据权限忽略 / 灰度标签
 * 在线程复用场景下串号。
 *
 * <p>不愿继承基类的测试可改用 {@link ContextCleanupExtension}（{@code @ExtendWith}）。
 */
public abstract class BaseUnitTest {

    @AfterEach
    protected void cleanupContext() {
        SecurityUtil.clear();
        TenantIgnoreContext.clearSqlIgnored();
        TenantIgnoreContext.clearCacheIgnored();
        DataScopeIgnoreContext.clearIgnored();
        GrayContext.clear();
    }
}
