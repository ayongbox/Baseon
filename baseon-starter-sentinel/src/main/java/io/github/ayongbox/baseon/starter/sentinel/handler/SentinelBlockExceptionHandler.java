package io.github.ayongbox.baseon.starter.sentinel.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.ErrorType;
import io.github.ayongbox.baseon.core.model.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sentinel 限流/熔断异常统一处理：把 {@link BlockException} 收敛为统一的 {@link R}。
 *
 * <p>{@code BlockException} 不是 {@code BizException}，starter-web 的全局异常处理不会接管，
 * 必须由本 Advice 分型映射：
 * <ul>
 *   <li>{@link FlowException} / {@link ParamFlowException} / {@link SystemBlockException}
 *   / {@link AuthorityException} → 429 + 1005（请求过于频繁）</li>
 *   <li>{@link DegradeException} → 503 + 9003（服务已降级）</li>
 *   <li>其余未知 {@link BlockException} 子类按流量防护兜底 → 429 + 1005</li>
 * </ul>
 *
 * <p>限流熔断属预期内的高并发保护行为，只记 WARN 简版日志、不打印堆栈，
 * 响应体不暴露规则详情与内部实现。
 */
@RestControllerAdvice
@ConditionalOnProperty(prefix = "baseon.sentinel", name = "block-handler-enabled",
        havingValue = "true", matchIfMissing = true)
public class SentinelBlockExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SentinelBlockExceptionHandler.class);

    @ExceptionHandler(BlockException.class)
    public ResponseEntity<R<Void>> handleBlockException(BlockException exception) {
        if (exception instanceof DegradeException) {
            log.warn("Sentinel 熔断降级触发 resource={}", exception.getRuleLimitApp());
            return build(ErrorType.SERVICE_UNAVAILABLE, CommonErrorCode.SERVICE_DEGRADED.getCode(),
                    CommonErrorCode.SERVICE_DEGRADED.getMessage());
        }
        // FlowException / ParamFlowException / SystemBlockException / AuthorityException
        // 以及未知子类统一按流量防护处理
        log.warn("Sentinel 流量防护触发 type={} resource={}",
                exception.getClass().getSimpleName(), exception.getRuleLimitApp());
        return build(ErrorType.TOO_MANY_REQUESTS, CommonErrorCode.TOO_MANY_REQUESTS.getCode(),
                CommonErrorCode.TOO_MANY_REQUESTS.getMessage());
    }

    private ResponseEntity<R<Void>> build(ErrorType type, int code, String message) {
        return ResponseEntity.status(type.getHttpStatus()).body(R.fail(code, message));
    }
}
