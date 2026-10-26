package io.github.ayongbox.baseon.starter.idempotent.aspect;

import io.github.ayongbox.baseon.core.annotation.Idempotent;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;
import io.github.ayongbox.baseon.starter.idempotent.config.BaseonIdempotentProperties;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotentAspectTest {

    private final RedisUtil redisUtil = mock(RedisUtil.class);
    private final BaseonIdempotentProperties properties = new BaseonIdempotentProperties();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void spelKeyIsEvaluatedAndAppendedToKey() throws Throwable {
        Method method = DemoService.class.getMethod("create", String.class);
        ProceedingJoinPoint pjp = mockJoinPoint(method, new Object[]{"orderA"});
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);

        newAspect(null).around(pjp, method.getAnnotation(Idempotent.class));

        verify(redisUtil).setIfAbsent("idem:DemoService.create:orderA", "1", 5L);
        verify(pjp).proceed();
    }

    @Test
    void headerTokenIsUsedWhenAnnotationKeyBlank() throws Throwable {
        Method method = DemoService.class.getMethod("submit", String.class);
        ProceedingJoinPoint pjp = mockJoinPoint(method, new Object[]{"anything"});
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Idempotent-Token", "tok123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);

        newAspect(null).around(pjp, method.getAnnotation(Idempotent.class));

        verify(redisUtil).setIfAbsent("idem:DemoService.submit:tok123", "1", 10L);
    }

    @Test
    void fallsBackToMethodLevelWhenNoKeyAndNoHeader() throws Throwable {
        Method method = DemoService.class.getMethod("submit", String.class);
        ProceedingJoinPoint pjp = mockJoinPoint(method, new Object[]{"anything"});
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);

        newAspect(null).around(pjp, method.getAnnotation(Idempotent.class));

        verify(redisUtil).setIfAbsent("idem:DemoService.submit", "1", 10L);
    }

    @Test
    void tenantPrefixIsAppendedWhenProviderPresent() throws Throwable {
        CacheKeyPrefixProvider provider = () -> "t1001:";
        Method method = DemoService.class.getMethod("create", String.class);
        ProceedingJoinPoint pjp = mockJoinPoint(method, new Object[]{"orderA"});
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);

        newAspect(provider).around(pjp, method.getAnnotation(Idempotent.class));

        verify(redisUtil).setIfAbsent("idem:t1001:DemoService.create:orderA", "1", 5L);
    }

    @Test
    void duplicateRequestThrows1006WhenPlaceholderFails() throws Throwable {
        Method method = DemoService.class.getMethod("create", String.class);
        ProceedingJoinPoint pjp = mockJoinPoint(method, new Object[]{"orderA"});
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(false);

        Idempotent annotation = method.getAnnotation(Idempotent.class);
        assertThatThrownBy(() -> newAspect(null).around(pjp, annotation))
                .isInstanceOfSatisfying(BizException.class, ex -> assertThat(ex.getCode()).isEqualTo(1006));
        verify(pjp, never()).proceed();
    }

    @Test
    void customMessageOverridesDefaultErrorText() {
        Method method = findMethod("placeOrder");
        ProceedingJoinPoint pjp = mockJoinPoint(method, new Object[]{"NO-1"});
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(false);

        Idempotent annotation = method.getAnnotation(Idempotent.class);
        assertThatThrownBy(() -> newAspect(null).around(pjp, annotation))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(1006);
                    assertThat(ex.getMessage()).isEqualTo("请勿重复下单");
                });
    }

    @Test
    void proceedsWithBusinessResultWhenPlaceholderAcquired() throws Throwable {
        Method method = DemoService.class.getMethod("create", String.class);
        ProceedingJoinPoint pjp = mockJoinPoint(method, new Object[]{"orderA"});
        when(redisUtil.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);
        when(pjp.proceed()).thenReturn("created:orderA");

        Object result = newAspect(null).around(pjp, method.getAnnotation(Idempotent.class));

        assertThat(result).isEqualTo("created:orderA");
        verify(pjp).proceed();
    }

    /** 构造切面：prefixProvider 为 null 表示容器中无 CacheKeyPrefixProvider 实现 */
    @SuppressWarnings("unchecked")
    private IdempotentAspect newAspect(CacheKeyPrefixProvider prefixProvider) {
        ObjectProvider<CacheKeyPrefixProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(prefixProvider);
        return new IdempotentAspect(redisUtil, properties, objectProvider);
    }

    private ProceedingJoinPoint mockJoinPoint(Method method, Object[] args) {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(
                Arrays.stream(method.getParameters()).map(Parameter::getName).toArray(String[]::new));
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(args);
        return pjp;
    }

    private Method findMethod(String name) {
        return Arrays.stream(DemoService.class.getMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    /** 演示业务类：覆盖三种注解形态（SpEL key + 自定义 ttl / 全默认 / 自定义文案） */
    static class DemoService {

        @Idempotent(key = "#name", ttlSeconds = 5)
        public String create(String name) {
            return "created:" + name;
        }

        @Idempotent
        public String submit(String name) {
            return "submitted";
        }

        @Idempotent(message = "请勿重复下单")
        public String placeOrder(String orderNo) {
            return "ok";
        }
    }
}
