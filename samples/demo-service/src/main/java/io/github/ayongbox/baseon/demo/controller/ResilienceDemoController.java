package io.github.ayongbox.baseon.demo.controller;

import io.github.ayongbox.baseon.core.annotation.Degrade;
import io.github.ayongbox.baseon.core.annotation.DegradeStrategy;
import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.demo.DemoBClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 5 期验证接口：下游调用熔断 + 降级取值兜底。
 */
@RestController
@RequestMapping("/demo/resilience")
public class ResilienceDemoController {

    private final DemoBClient demoBClient;

    public ResilienceDemoController(DemoBClient demoBClient) {
        this.demoBClient = demoBClient;
    }

    /**
     * 熔断 + CACHE 降级演示：先正常调用一次让缓存留下成功值，
     * 再停掉 demo-service-b——失败率超阈值后熔断器 demoB 打开，调用快速失败，
     * @Degrade 切面按 CACHE 策略返回该方法最近一次成功的缓存值。
     */
    @GetMapping("/call-b")
    @CircuitBreaker(name = "demoB")
    @Degrade(strategy = DegradeStrategy.CACHE)
    public R<Map<String, Object>> callB() {
        return demoBClient.ping();
    }
}
