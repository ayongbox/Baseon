package io.github.ayongbox.baseon.core.model;

/**
 * 错误码统一契约。
 *
 * <p>错误码分段规约（5 位数字）：
 * <pre>
 * 0        成功
 * 01xxx    系统 / 框架级错误
 * 09xxx    远程调用 / 外部依赖错误
 * 1xxxx+   业务错误：模块码 2 位 + 业务码 3 位（如 20101）
 * </pre>
 *
 * <p>业务模块码由架构组统一分配，禁止各业务自行占用 01 / 09 段。
 */
public interface ErrorCode {

    /**
     * 错误码，成功固定为 0。
     */
    int getCode();

    /**
     * 默认错误消息，最终输出时会按 locale 做国际化替换。
     */
    String getMessage();

    /**
     * 错误语义分类，供 Starter 映射 HTTP 状态码、决定日志级别与是否告警。
     *
     * <p>默认按系统错误处理；业务错误码应显式覆盖，否则会被当作 500 并触发系统告警。
     */
    default ErrorType getType() {
        return ErrorType.SERVER_ERROR;
    }
}
