package io.github.ayongbox.baseon.starter.tenant.support;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.starter.tenant.config.BaseonTenantProperties;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

/**
 * 租户过滤值提供者：从登录上下文读取 tenantId，由 starter-mybatis-plus 装配进
 * {@code MybatisPlusInterceptor}（位置固定在分页插件之前）。
 *
 * <p>无租户上下文时的行为由 {@code baseon.tenant.strict} 决定：
 * 严格模式抛异常；默认模式返回不可能命中的 -1，宁可查不到数据也不跨租户泄露。
 */
public class BaseonTenantLineHandler implements TenantLineHandler {

    /** 无租户上下文时的兜底租户值：约定不存在的租户，保证查询结果为空 */
    private static final long NO_TENANT_FALLBACK = -1L;

    private final BaseonTenantProperties properties;

    public BaseonTenantLineHandler(BaseonTenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public Expression getTenantId() {
        Long tenantId = SecurityUtil.getTenantId();
        if (tenantId != null) {
            return new LongValue(tenantId);
        }
        if (properties.isStrict()) {
            throw new IllegalStateException("租户上下文缺失：请在请求入口传入租户标识，或为该方法添加 @IgnoreTenant");
        }
        return new LongValue(NO_TENANT_FALLBACK);
    }

    @Override
    public String getTenantIdColumn() {
        return properties.getColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return properties.getIgnoreTables().contains(tableName);
    }
}
