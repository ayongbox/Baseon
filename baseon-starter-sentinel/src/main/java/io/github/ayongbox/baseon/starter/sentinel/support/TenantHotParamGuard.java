package io.github.ayongbox.baseon.starter.sentinel.support;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.github.ayongbox.baseon.core.context.SecurityUtil;

/**
 * 租户维度热点参数限流门面。
 *
 * <p>以当前租户 ID 作为热点参数进入 Sentinel 资源，配合热点参数规则
 * （{@code ParamFlowRule}，参数索引固定为 0）即可实现「按租户独立计数的 QPS 限流」——
 * 普通流控规则管资源总量，热点参数规则管单个租户的配额，两者可叠加。
 *
 * <p>热点参数规则需按租户参数索引配置：{@code paramIdx=0}、{@code count=单租户 QPS 上限}，
 * 规则持久化走 SCA 原生 {@code spring.cloud.sentinel.datasource.*}（dataId 约定见模块手册），
 * demo-service 中有完整演示。
 *
 * <p>无租户上下文（登录前接口、系统任务）时以约定值 {@code -1} 兜底，
 * 与 starter-tenant 的「宁可查不到也不跨租户」约定一致：无上下文流量共用一个独立配额，
 * 不会挤占任何真实租户的配额。
 */
public class TenantHotParamGuard {

    /** 无租户上下文时的兜底热点参数值：约定不存在的租户，与 starter-tenant 口径一致 */
    private static final long NO_TENANT_FALLBACK = -1L;

    /**
     * 尝试以当前租户为热点参数进入资源。
     *
     * @param resource Sentinel 资源名
     * @return 放行时固定返回 true
     * @throws BlockException 触发限流时抛出（实际类型为 ParamFlowException），
     *                        交由 {@code SentinelBlockExceptionHandler} 统一映射 429 + 1005
     */
    public boolean acquire(String resource) throws BlockException {
        Long tenantId = SecurityUtil.getTenantId();
        long hotParam = tenantId == null ? NO_TENANT_FALLBACK : tenantId;
        try (Entry entry = SphU.entry(resource, EntryType.IN, 1, hotParam)) {
            return true;
        }
    }
}
