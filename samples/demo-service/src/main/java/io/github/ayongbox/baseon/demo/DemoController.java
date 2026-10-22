package io.github.ayongbox.baseon.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of(
                "code", 0,
                "msg", "ok",
                "data", "baseon skeleton is running"
        );
    }
}
