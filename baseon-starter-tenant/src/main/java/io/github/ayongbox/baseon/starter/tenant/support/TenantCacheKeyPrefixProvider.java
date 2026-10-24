package io.github.ayongbox.baseon.starter.tenant.support;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;

/**
 * 缓存 key 租户前缀：格式 {@code t<tenantId>:}。
 *
 * <p>无租户上下文或被 {@code @IgnoreTenantCache} 标记时返回空前缀——
 * 前者兼容登录前接口，后者用于全租户共享缓存。
 */
public class TenantCacheKeyPrefixProvider implements CacheKeyPrefixProvider {

    @Override
    public String prefix() {
        if (TenantIgnoreContext.isCacheIgnored()) {
            return "";
        }
        Long tenantId = SecurityUtil.getTenantId();
        return tenantId == null ? "" : "t" + tenantId + ":";
    }
}
