package io.github.ayongbox.baseon.demo.controller;

import io.github.ayongbox.baseon.core.model.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 5 期验证接口：XSS 入参转义。
 * 提交 {@code <script>alert(1)</script>} 观察回显已被转义为 HTML 实体。
 */
@RestController
@RequestMapping("/demo/xss")
public class XssDemoController {

    /** 表单参数回显：验证 query/form 参数经 XssFilter 包装后转义 */
    @PostMapping("/echo")
    public R<String> echo(@RequestParam String content) {
        return R.ok(content);
    }

    /** JSON 请求体回显：验证字符串字段在 Jackson 反序列化阶段完成转义 */
    @PostMapping("/echo-json")
    public R<EchoRequest> echoJson(@RequestBody EchoRequest request) {
        return R.ok(request);
    }

    /** 单字段回显请求体 */
    public static class EchoRequest {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
