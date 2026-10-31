package io.github.ayongbox.baseon.test.core;

import io.github.ayongbox.baseon.core.context.DataScopeIgnoreContext;
import io.github.ayongbox.baseon.core.context.GrayContext;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit5 扩展：与 {@link BaseUnitTest} 等价的上下文清理逻辑，
 * 供无法继承基类（如已继承其它类、或偏好组合）的测试使用。
 *
 * <pre>{@code
 * @ExtendWith(ContextCleanupExtension.class)
 * class MyTest { ... }
 * }</pre>
 */
public class ContextCleanupExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) {
        SecurityUtil.clear();
        TenantIgnoreContext.clearSqlIgnored();
        TenantIgnoreContext.clearCacheIgnored();
        DataScopeIgnoreContext.clearIgnored();
        GrayContext.clear();
    }
}
