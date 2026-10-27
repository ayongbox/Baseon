package io.github.ayongbox.baseon.starter.log.support;

/**
 * 操作审计记录处理器 SPI。
 *
 * <p>默认实现 {@link LoggingOperateLogHandler} 仅打日志；业务声明自己的
 * {@code OperateLogHandler} Bean（写库 / 发 MQ）即整体替换默认实现，
 * 切面零改动。
 */
public interface OperateLogHandler {

    /**
     * 消费一条审计记录。实现应轻量、可靠——在业务调用链上同步执行，
     * 重活（落库、外发）建议实现内异步化，避免拖慢或打断业务主流程。
     */
    void handle(OperateLogRecord record);
}
