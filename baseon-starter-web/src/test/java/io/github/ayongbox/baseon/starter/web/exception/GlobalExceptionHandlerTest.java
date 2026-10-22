package io.github.ayongbox.baseon.starter.web.exception;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void biz_exception_should_map_to_declared_http_status() throws Exception {
        mockMvc.perform(get("/probe/biz-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1004))
                .andExpect(jsonPath("$.msg").value("用户不存在"));
    }

    @Test
    void biz_exception_without_error_code_should_fall_back_to_500() throws Exception {
        mockMvc.perform(get("/probe/biz-server"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(5001));
    }

    @Test
    void unknown_exception_should_not_leak_internal_message() throws Exception {
        mockMvc.perform(get("/probe/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(1999))
                .andExpect(jsonPath("$.msg").value("系统繁忙，请稍后重试"));
    }

    @Test
    void validation_failure_should_return_400() throws Exception {
        mockMvc.perform(post("/probe/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void missing_request_param_should_return_400() throws Exception {
        mockMvc.perform(get("/probe/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/biz-not-found")
        public R<Void> bizNotFound() {
            throw new BizException(CommonErrorCode.NOT_FOUND, "用户不存在");
        }

        @GetMapping("/probe/biz-server")
        public R<Void> bizServer() {
            throw new BizException(5001, "库存扣减失败");
        }

        @GetMapping("/probe/unknown")
        public R<Void> unknown() {
            throw new IllegalStateException("数据库连接池耗尽: jdbc:mysql://internal:3306");
        }

        @PostMapping("/probe/valid")
        public R<Void> valid(@Valid @RequestBody ProbeBody body) {
            return R.ok();
        }

        @GetMapping("/probe/param")
        public R<Void> param(@RequestParam String name) {
            return R.ok();
        }
    }

    static class ProbeBody {

        @NotBlank(message = "名称不能为空")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
