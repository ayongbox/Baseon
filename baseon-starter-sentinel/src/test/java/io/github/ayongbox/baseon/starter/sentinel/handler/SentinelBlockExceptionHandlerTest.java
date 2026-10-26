package io.github.ayongbox.baseon.starter.sentinel.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.R;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SentinelBlockExceptionHandler} 分型映射用例：
 * 流控/热点/系统/授权类 → 429 + 1005；熔断降级类 → 503 + 9003；未知子类兜底 429。
 */
class SentinelBlockExceptionHandlerTest {

    private final SentinelBlockExceptionHandler handler = new SentinelBlockExceptionHandler();

    @Test
    void flowExceptionMapsTo429() {
        ResponseEntity<R<Void>> response = handler.handleBlockException(new FlowException("order:create"));
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS.getCode());
    }

    @Test
    void degradeExceptionMapsTo503() {
        ResponseEntity<R<Void>> response = handler.handleBlockException(new DegradeException("remote:call"));
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonErrorCode.SERVICE_DEGRADED.getCode());
    }

    @Test
    void paramFlowSystemAndAuthorityExceptionsMapTo429() {
        assertThat(handler.handleBlockException(new ParamFlowException("tenant:qps", "param flow blocked"))
                .getStatusCode().value()).isEqualTo(429);
        assertThat(handler.handleBlockException(new SystemBlockException("high:load", "system block"))
                .getStatusCode().value()).isEqualTo(429);
        ResponseEntity<R<Void>> response = handler.handleBlockException(new AuthorityException("admin:only"));
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS.getCode());
    }

    @Test
    void unknownBlockExceptionSubclassFallsBackTo429() {
        // BlockException 为抽象类，匿名子类验证兜底分型路径
        BlockException unknown = new BlockException("custom:resource") {
        };
        ResponseEntity<R<Void>> response = handler.handleBlockException(unknown);
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonErrorCode.TOO_MANY_REQUESTS.getCode());
    }
}
