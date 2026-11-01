package io.github.ayongbox.baseon.core.launch;

/**
 * 启动前置校验失败：fail-fast 模式下抛出，终止应用启动。
 *
 * <p>独立于 {@code BizException}：启动阶段的失败不是业务错误，
 * 不需要错误码与 HTTP 语义，直接用运行时异常让 Spring 以失败状态退出。
 */
public class LaunchCheckException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LaunchCheckException(String message) {
        super(message);
    }
}
