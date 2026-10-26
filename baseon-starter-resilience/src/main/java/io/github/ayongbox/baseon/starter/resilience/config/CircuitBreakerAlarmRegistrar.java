package io.github.ayongbox.baseon.starter.resilience.config;

import io.github.ayongbox.baseon.starter.resilience.alarm.AlarmHook;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 熔断器状态转换事件 → {@link AlarmHook} 的桥接器。
 *
 * <p>{@link CircuitBreakerRegistry} 由 resilience4j-spring-boot3 的自动配置装配；
 * 本类待所有单例就绪后（SmartInitializingSingleton）订阅注册表事件，
 * 对存量与后续动态创建的熔断器统一挂状态转换监听。
 * 注册表不存在（resilience4j 自动配置被排除的场景）时静默跳过。
 */
public class CircuitBreakerAlarmRegistrar implements SmartInitializingSingleton {

    private final ObjectProvider<CircuitBreakerRegistry> registryProvider;
    private final AlarmHook alarmHook;

    public CircuitBreakerAlarmRegistrar(ObjectProvider<CircuitBreakerRegistry> registryProvider, AlarmHook alarmHook) {
        this.registryProvider = registryProvider;
        this.alarmHook = alarmHook;
    }

    @Override
    public void afterSingletonsInstantiated() {
        CircuitBreakerRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        // 覆盖注册前已存在的熔断器，再监听后续动态创建的
        registry.getAllCircuitBreakers().forEach(this::subscribeStateTransition);
        registry.getEventPublisher()
                .onEntryAdded(event -> subscribeStateTransition(event.getAddedEntry()));
    }

    private void subscribeStateTransition(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            CircuitBreaker.StateTransition transition = event.getStateTransition();
            alarmHook.onStateChange(
                    event.getCircuitBreakerName(),
                    transition.getFromState().name(),
                    transition.getToState().name());
        });
    }
}
