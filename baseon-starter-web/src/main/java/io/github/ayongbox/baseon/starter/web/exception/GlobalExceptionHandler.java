package io.github.ayongbox.baseon.starter.web.exception;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.ErrorType;
import io.github.ayongbox.baseon.core.model.R;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把异常收敛为统一的 {@link R}，并按 {@link ErrorType} 映射 HTTP 状态码。
 *
 * <p>原则：业务异常只记 WARN 且不打印堆栈；未预期异常记 ERROR 并保留堆栈。
 * 响应体永远不返回堆栈信息与内部实现细节。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException exception) {
        log.warn("业务异常 code={} message={}", exception.getCode(), exception.getMessage());
        return build(exception.getType(), exception.getCode(), exception.getMessage());
    }

    /**
     * 同时覆盖 MethodArgumentNotValidException（@RequestBody 校验）与 BindException（表单绑定）。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindException(BindException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return badRequest(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return badRequest(message);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<R<Void>> handleBadRequest(Exception exception) {
        log.warn("请求参数非法: {}", exception.getMessage());
        return badRequest(CommonErrorCode.PARAM_INVALID.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return build(ErrorType.METHOD_NOT_ALLOWED, CommonErrorCode.PARAM_INVALID.getCode(), "请求方法不支持");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnknownException(Exception exception) {
        log.error("未捕获异常", exception);
        return build(ErrorType.SERVER_ERROR,
                CommonErrorCode.SYSTEM_ERROR.getCode(),
                CommonErrorCode.SYSTEM_ERROR.getMessage());
    }

    private ResponseEntity<R<Void>> badRequest(String message) {
        return build(ErrorType.CLIENT_ERROR, CommonErrorCode.PARAM_INVALID.getCode(), message);
    }

    private ResponseEntity<R<Void>> build(ErrorType type, int code, String message) {
        return ResponseEntity.status(type.getHttpStatus()).body(R.fail(code, message));
    }
}
