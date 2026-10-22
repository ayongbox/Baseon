package io.github.ayongbox.baseon.core.model;

/**
 * 框架内置通用错误码，业务自定义错误码请实现 {@link ErrorCode}，不要扩展本枚举。
 */
public enum CommonErrorCode implements ErrorCode {

    /** 成功 */
    SUCCESS(0, "操作成功", ErrorType.SUCCESS),

    /** 参数校验失败 */
    PARAM_INVALID(1001, "参数校验失败", ErrorType.CLIENT_ERROR),

    /** 未认证 / Token 无效 */
    UNAUTHORIZED(1002, "未认证或登录已失效", ErrorType.UNAUTHORIZED),

    /** 无访问权限 */
    FORBIDDEN(1003, "无访问权限", ErrorType.FORBIDDEN),

    /** 资源不存在 */
    NOT_FOUND(1004, "资源不存在", ErrorType.NOT_FOUND),

    /** 请求过于频繁 */
    TOO_MANY_REQUESTS(1005, "请求过于频繁，请稍后重试", ErrorType.TOO_MANY_REQUESTS),

    /** 幂等重复请求 */
    DUPLICATE_REQUEST(1006, "重复请求，请勿重复提交", ErrorType.DUPLICATE),

    /** 系统内部错误 */
    SYSTEM_ERROR(1999, "系统繁忙，请稍后重试", ErrorType.SERVER_ERROR),

    /** 远程调用失败 */
    REMOTE_CALL_FAILED(9001, "远程调用失败", ErrorType.REMOTE_ERROR),

    /** 远程调用超时 */
    REMOTE_CALL_TIMEOUT(9002, "远程调用超时", ErrorType.REMOTE_ERROR),

    /** 服务已降级 */
    SERVICE_DEGRADED(9003, "服务已降级，请稍后重试", ErrorType.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final ErrorType type;

    CommonErrorCode(int code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public ErrorType getType() {
        return type;
    }
}
