package io.github.ayongbox.baseon.starter.resilience.config;

import io.github.ayongbox.baseon.starter.resilience.alarm.AlarmHook;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CircuitBreakerAlarmRegistrarTest {

    /** 注册后熔断器发生状态转换，AlarmHook 被回调 */
    @Test
    void alarmHookInvokedOnStateTransition() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        AlarmHook alarmHook = mock(AlarmHook.class);
        ObjectProvider<CircuitBreakerRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);

        CircuitBreakerAlarmRegistrar registrar = new CircuitBreakerAlarmRegistrar(provider, alarmHook);
        registrar.afterSingletonsInstantiated();

        // 熔断器在注册后动态创建，也应被挂接上监听
        registry.circuitBreaker("t").transitionToOpenState();

        verify(alarmHook).onStateChange("t", "CLOSED", "OPEN");
    }

    /** 注册表不存在时静默跳过，不抛异常 */
    @Test
    void skipsSilentlyWhenRegistryAbsent() {
        AlarmHook alarmHook = mock(AlarmHook.class);
        ObjectProvider<CircuitBreakerRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        CircuitBreakerAlarmRegistrar registrar = new CircuitBreakerAlarmRegistrar(provider, alarmHook);
        registrar.afterSingletonsInstantiated();
    }
}
