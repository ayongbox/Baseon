package io.github.ayongbox.baseon.starter.trace.async;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;

/**
 * MDC 透传装饰器：把当前线程的 MDC 复制到异步任务线程。
 *
 * <p>Spring 的 {@link TaskDecorator} 只支持 {@link Async} 场景；
 * {@link Scheduled} 需要额外切面处理（见 {@link ScheduledTraceAspect}）。
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
