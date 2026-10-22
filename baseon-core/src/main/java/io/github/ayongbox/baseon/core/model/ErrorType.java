package io.github.ayongbox.baseon.core.model;

/**
 * 错误语义分类：内核只描述「错误属于哪一类」，HTTP 状态码映射由 Starter 层完成。
 *
 * <p>之所以让内核携带 httpStatus，是为了避免各 Starter 重复维护区间判断；
 * 但内核不引入 Spring 的 HttpStatus，只保留协议无关的数字。非 HTTP 场景（MQ、Job）可直接忽略。
 */
public enum ErrorType {

    /** 成功 */
    SUCCESS(200),

    /** 参数、格式、业务规则校验失败 */
    CLIENT_ERROR(400),

    /** 未认证 / 凭证失效 */
    UNAUTHORIZED(401),

    /** 已认证但无权限 */
    FORBIDDEN(403),

    /** 资源不存在 */
    NOT_FOUND(404),

    /** 重复请求 / 状态冲突 */
    DUPLICATE(409),

    /** 限流 */
    TOO_MANY_REQUESTS(429),

    /** 依赖服务异常、超时、熔断 */
    REMOTE_ERROR(502),

    /** 服务不可用（降级） */
    SERVICE_UNAVAILABLE(503),

    /** 未分类的系统内部错误 */
    SERVER_ERROR(500);

    private final int httpStatus;

    ErrorType(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
