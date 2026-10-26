package io.github.ayongbox.baseon.starter.resilience.aspect;

import io.github.ayongbox.baseon.core.annotation.Degrade;
import io.github.ayongbox.baseon.core.annotation.DegradeStrategy;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.starter.resilience.support.DegradeResultCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Arrays;

/**
 * {@code @Degrade} 切面：方法抛异常时按策略兜底取值。
 *
 * <p>与 resilience4j 的 {@code @CircuitBreaker} 组合使用：熔断打开后调用快速失败，
 * 本切面负责失败后的取值兜底。策略语义见 {@link DegradeStrategy}。
 *
 * <p><b>切面顺序是本组件正确性的关键</b>：本切面必须在 CircuitBreakerAspect
 * <b>外层</b>执行（Spring AOP 中 order 值小者为外层），使熔断器更贴近目标方法——
 * 方法抛出的真实异常先经熔断器计数、再被本切面兜底。若顺序反转（本切面在内），
 * 兜底返回值会被熔断器误判为成功，熔断器永远不打开。
 *
 * <p>注意：resilience4j-spring6 2.2.x 的 CircuitBreakerAspect 默认 order 为
 * {@code LOWEST_PRECEDENCE - 4}（可通过 circuitBreakerAspectOrder 配置修改），
 * 本切面取 {@code LOWEST_PRECEDENCE - 5} 确保稳处其外层；若业务自定义了
 * circuitBreakerAspectOrder，需同步审视本值。
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 5)
public class DegradeAspect {

    private static final Logger log = LoggerFactory.getLogger(DegradeAspect.class);

    private final DegradeResultCache resultCache;

    public DegradeAspect(DegradeResultCache resultCache) {
        this.resultCache = resultCache;
    }

    @Around("@annotation(degrade)")
    public Object around(ProceedingJoinPoint pjp, Degrade degrade) throws Throwable {
        String cacheKey = buildCacheKey(pjp);
        try {
            Object result = pjp.proceed();
            // 仅成功时写缓存，保证缓存里永远是「最近一次成功结果」
            if (degrade.strategy() == DegradeStrategy.CACHE) {
                resultCache.put(cacheKey, result);
            }
            return result;
        } catch (Throwable ex) {
            log.warn("降级触发：method={} strategy={} cause={}",
                    cacheKey, degrade.strategy(), ex.getMessage());
            return switch (degrade.strategy()) {
                case CACHE -> {
                    Object cached = resultCache.get(cacheKey);
                    if (cached != null) {
                        yield cached;
                    }
                    throw degraded(ex);
                }
                case DEFAULT -> defaultValue(pjp);
                case FAIL_FAST -> throw degraded(ex);
            };
        }
    }

    /** 缓存 key：类全名.方法名 + 参数摘要，同方法不同参数互不污染 */
    private String buildCacheKey(ProceedingJoinPoint pjp) {
        return pjp.getSignature().getDeclaringTypeName()
                + "." + pjp.getSignature().getName()
                + Arrays.deepToString(pjp.getArgs());
    }

    /** DEFAULT 策略的兜底值：引用类型 null，数值 0，布尔 false */
    private Object defaultValue(ProceedingJoinPoint pjp) {
        Class<?> returnType = ((MethodSignature) pjp.getSignature()).getReturnType();
        if (!returnType.isPrimitive() || returnType == void.class) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        // 基本数值类型需返回对应包装类，避免代理拆箱时类型不匹配
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        return 0;
    }

    /** 包装为「服务已降级」业务异常，保留原异常作为 cause 便于排障 */
    private BizException degraded(Throwable cause) {
        return new BizException(CommonErrorCode.SERVICE_DEGRADED, cause);
    }
}
