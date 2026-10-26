package io.github.ayongbox.baseon.demo.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.starter.sentinel.support.TenantHotParamGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 5 期验证接口：Sentinel 限流。
 * 规则发布在 Nacos（dataId = baseon-demo-service-sentinel-flow-rules，groupId = SENTINEL_GROUP），
 * 触发限流后由 SentinelBlockExceptionHandler 统一映射 429 + 1005，业务无需 catch。
 */
@RestController
@RequestMapping("/demo/sentinel")
public class SentinelDemoController {

    private final TenantHotParamGuard tenantHotParamGuard;

    public SentinelDemoController(TenantHotParamGuard tenantHotParamGuard) {
        this.tenantHotParamGuard = tenantHotParamGuard;
    }

    /** QPS 流控演示：对资源 demo-qps 发布 flow 规则即可触发限流 */
    @GetMapping("/qps")
    @SentinelResource("demo-qps")
    public R<String> qps() {
        return R.ok("ok");
    }

    /**
     * 租户维度热点限流演示：以当前租户 ID 为热点参数进入资源，
     * 配套 param-flow 规则（paramIdx 固定为 0）实现按租户独立计数。
     */
    @GetMapping("/tenant-qps")
    public R<Long> tenantQps() throws BlockException {
        tenantHotParamGuard.acquire("demo-tenant-qps");
        return R.ok(SecurityUtil.getTenantId());
    }
}
