package io.github.ayongbox.baseon.starter.tenant.support;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import io.github.ayongbox.baseon.core.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantCacheKeyPrefixProviderTest {

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
        TenantIgnoreContext.clearCacheIgnored();
    }

    @Test
    void prefixesWithTenantIdWhenPresent() {
        LoginUser user = new LoginUser();
        user.setTenantId(1001L);
        SecurityUtil.set(user);

        assertThat(new TenantCacheKeyPrefixProvider().prefix()).isEqualTo("t1001:");
    }

    @Test
    void noPrefixWhenNoTenant() {
        assertThat(new TenantCacheKeyPrefixProvider().prefix()).isEmpty();
    }

    @Test
    void noPrefixWhenCacheIgnored() {
        LoginUser user = new LoginUser();
        user.setTenantId(1001L);
        SecurityUtil.set(user);
        TenantIgnoreContext.markCacheIgnored();

        assertThat(new TenantCacheKeyPrefixProvider().prefix()).isEmpty();
    }
}
