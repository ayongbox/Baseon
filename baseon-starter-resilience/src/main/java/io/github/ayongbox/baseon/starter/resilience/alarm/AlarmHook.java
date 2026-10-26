package io.github.ayongbox.baseon.starter.resilience.alarm;

/**
 * 熔断状态变更告警钩子：熔断器发生非稳态转换时回调。
 *
 * <p>默认实现 {@link LoggingAlarmHook} 仅打日志；业务方可提供同类型 Bean 覆盖，
 * 接入短信/邮件/IM 等告警渠道（{@code @ConditionalOnMissingBean} 退避）。
 */
public interface AlarmHook {

    /**
     * @param circuitBreakerName 熔断器名称
     * @param fromState          转换前状态（CLOSED / OPEN / HALF_OPEN 等）
     * @param toState            转换后状态
     */
    void onStateChange(String circuitBreakerName, String fromState, String toState);
}
