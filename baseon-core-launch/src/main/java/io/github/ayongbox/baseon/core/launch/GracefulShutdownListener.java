package io.github.ayongbox.baseon.core.launch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;

import java.util.Map;

/**
 * 优雅停机监听器：容器关闭时统一执行所有 {@link ShutdownHook} Bean。
 *
 * <p>单个钩子异常不影响其余钩子（异常隔离 + 错误日志），避免「第一个钩子失败
 * 导致缓冲数据未 flush」的连锁问题。监听器以最高优先级执行，先于普通
 * Lifecycle/DisposableBean 回调，保证业务清理在框架销毁前完成。
 */
public class GracefulShutdownListener implements ApplicationListener<ContextClosedEvent>, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownListener.class);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        Map<String, ShutdownHook> hooks = event.getApplicationContext().getBeansOfType(ShutdownHook.class);
        log.info("[baseon] 优雅停机开始，停机钩子数量：{}", hooks.size());
        hooks.forEach((name, hook) -> {
            try {
                hook.onShutdown();
                log.info("[baseon] 停机钩子完成：{}", name);
            } catch (Exception ex) {
                log.error("[baseon] 停机钩子执行异常：{}", name, ex);
            }
        });
        log.info("[baseon] 优雅停机结束");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
