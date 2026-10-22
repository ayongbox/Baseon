package io.github.ayongbox.baseon.core.model;

/**
 * 业务异常：代表「预期内的业务失败」，不是系统故障。
 *
 * <p>刻意关闭堆栈采集：业务异常在生产环境按 WARN 输出，只需要错误码与消息，
 * 采集堆栈既浪费 CPU 也会污染日志。需要排障的异常一律属于系统异常，不归本类管。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

    public BizException(int code, String message) {
        super(message, null, false, false);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
