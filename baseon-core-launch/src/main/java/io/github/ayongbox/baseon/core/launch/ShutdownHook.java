package io.github.ayongbox.baseon.core.launch;

/**
 * 优雅停机钩子 SPI：业务通过注册 Bean 提供停机清理逻辑，框架负责编排与异常隔离。
 *
 * <p>典型用途：下线前摘除本地任务、flush 缓冲数据、关闭自定义连接池。
 * Web 层的长请求等待已由 {@code server.shutdown=graceful} 承载，本钩子只做业务清理，
 * 不要在这里 sleep 占位——拖延停机时间会放大 K8s 摘流窗口。
 */
@FunctionalInterface
public interface ShutdownHook {

    /**
     * 执行停机清理。抛出的异常不会中断其他钩子，框架记录后继续。
     */
    void onShutdown();
}
