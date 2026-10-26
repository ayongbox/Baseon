package io.github.ayongbox.baseon.core.model;

/**
 * 业务异常：代表「预期内的业务失败」，不是系统故障。
 *
 * <p>刻意关闭堆栈采集：业务异常在生产环境按 WARN 输出，只需要错误码与消息，
 * 采集堆栈既浪费 CPU 也会污染日志。需要排障的异常一律属于系统异常，不归本类管。
 *
 * <p>异常自带的 {@link ErrorType} 供 Web 层映射 HTTP 状态码；直接用 code + message
 * 构造时按系统错误处理，业务代码应优先使用 {@link ErrorCode} 构造。
 */
public class BizException extends RuntimeException {

    private final int code;
    private final ErrorType type;

    public BizException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), errorCode.getType());
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message, errorCode.getType());
    }

    /**
     * 携带原始异常：供包装下游故障的场景使用（如降级包装远程调用异常），
     * 仍关闭自身堆栈采集，排障信息由 cause 承担。
     */
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause, false, false);
        this.code = errorCode.getCode();
        this.type = errorCode.getType();
    }

    public BizException(int code, String message) {
        this(code, message, ErrorType.SERVER_ERROR);
    }

    public BizException(int code, String message, ErrorType type) {
        super(message, null, false, false);
        this.code = code;
        this.type = type == null ? ErrorType.SERVER_ERROR : type;
    }

    public int getCode() {
        return code;
    }

    public ErrorType getType() {
        return type;
    }
}
