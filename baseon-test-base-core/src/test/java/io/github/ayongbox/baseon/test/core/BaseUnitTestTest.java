package io.github.ayongbox.baseon.test.core;

import io.github.ayongbox.baseon.core.context.DataScopeIgnoreContext;
import io.github.ayongbox.baseon.core.context.GrayContext;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import io.github.ayongbox.baseon.core.model.LoginUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link BaseUnitTest} 的 {@code @AfterEach} 清理逻辑确实清空全部线程上下文。
 */
class BaseUnitTestTest {

    @Test
    void cleanupContextClearsAllThreadLocals() {
        SecurityUtil.set(new LoginUser());
        TenantIgnoreContext.markSqlIgnored();
        TenantIgnoreContext.markCacheIgnored();
        DataScopeIgnoreContext.markIgnored();
        GrayContext.set("tag");

        new BaseUnitTest() {
        }.cleanupContext();

        assertThat(SecurityUtil.get()).isNull();
        assertThat(TenantIgnoreContext.isSqlIgnored()).isFalse();
        assertThat(TenantIgnoreContext.isCacheIgnored()).isFalse();
        assertThat(DataScopeIgnoreContext.isIgnored()).isFalse();
        assertThat(GrayContext.hasTag()).isFalse();
    }
}
