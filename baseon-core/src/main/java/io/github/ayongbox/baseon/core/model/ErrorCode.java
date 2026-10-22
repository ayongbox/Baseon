package io.github.ayongbox.baseon.core.model;

/**
 * 错误码统一契约。
 *
 * <p>错误码分段规约（5 位数字）：
 * <pre>
 * 0        成功
 * 01xxx    系统 / 框架级错误
 * 09xxx    远程调用 / 外部依赖错误
 * 1xxxx+   业务错误：模块码 2 位 + 业务码 3 位（如 10001）
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
}
