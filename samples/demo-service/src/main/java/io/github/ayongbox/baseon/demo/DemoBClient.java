package io.github.ayongbox.baseon.demo;

import io.github.ayongbox.baseon.core.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * demo-service-b 的 Feign 客户端，服务名即 Nacos 注册名。
 */
@FeignClient("demo-service-b")
public interface DemoBClient {

    @GetMapping("/demo/ping")
    R<Map<String, Object>> ping();
}
