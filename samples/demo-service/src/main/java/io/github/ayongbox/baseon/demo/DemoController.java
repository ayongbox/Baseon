package io.github.ayongbox.baseon.demo;

import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.starter.cloud.nacos.ConfigHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private final DemoBClient demoBClient;
    private final ConfigHolder configHolder;

    public DemoController(DemoBClient demoBClient, ConfigHolder configHolder) {
        this.demoBClient = demoBClient;
        this.configHolder = configHolder;
    }

    @GetMapping("/hello")
    public R<String> hello() {
        return R.ok("baseon skeleton is running");
    }

    /**
     * 跨服务调用验证入口：Feign → demo-service-b，观察 traceId / userId / 灰度标签透传。
     */
    @GetMapping("/call-b")
    public R<Map<String, Object>> callB() {
        return demoBClient.ping();
    }

    /**
     * 配置中心验证入口：ConfigHolder 直读 Environment，Nacos 配置刷新后无需重启生效。
     */
    @GetMapping("/config-info")
    public R<String> configInfo() {
        return R.ok(configHolder.get("demo.greeting", "default-greeting-from-local"));
    }
}
