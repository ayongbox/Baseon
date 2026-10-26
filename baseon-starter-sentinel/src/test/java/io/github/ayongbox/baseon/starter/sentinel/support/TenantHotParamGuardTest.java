package io.github.ayongbox.baseon.starter.sentinel.support;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TenantHotParamGuard} 用例：未配置热点参数规则时正常放行，
 * 有无租户上下文两种形态均不抛异常。
 */
class TenantHotParamGuardTest {

    private final TenantHotParamGuard guard = new TenantHotParamGuard();

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    void acquirePassesWithTenantContextWhenNoRulesConfigured() throws BlockException {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setTenantId(1001L);
        SecurityUtil.set(user);

        assertThat(guard.acquire("test:tenant-qps")).isTrue();
    }

    @Test
    void acquirePassesWithoutContextUsingFallbackTenant() throws BlockException {
        // 无上下文时以 -1 兜底，无规则场景正常放行
        assertThat(guard.acquire("test:tenant-qps-anonymous")).isTrue();
    }
}
