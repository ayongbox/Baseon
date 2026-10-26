package io.github.ayongbox.baseon.starter.resilience.alarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AlarmHook} 默认实现：非稳态转换打日志——打开 ERROR、半开/恢复 WARN。
 *
 * <p>日志是保底通道；生产接入告警渠道请提供自定义 {@link AlarmHook} Bean 覆盖本实现。
 */
public class LoggingAlarmHook implements AlarmHook {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlarmHook.class);

    @Override
    public void onStateChange(String circuitBreakerName, String fromState, String toState) {
        switch (toState) {
            case "OPEN" -> log.error("熔断器打开：name={} {} -> {}", circuitBreakerName, fromState, toState);
            case "HALF_OPEN" -> log.warn("熔断器半开（试探恢复）：name={} {} -> {}", circuitBreakerName, fromState, toState);
            case "CLOSED" -> log.warn("熔断器恢复关闭：name={} {} -> {}", circuitBreakerName, fromState, toState);
            default -> log.debug("熔断器状态转换：name={} {} -> {}", circuitBreakerName, fromState, toState);
        }
    }
}
