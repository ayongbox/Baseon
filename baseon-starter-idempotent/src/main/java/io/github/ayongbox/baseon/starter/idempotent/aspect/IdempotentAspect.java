package io.github.ayongbox.baseon.starter.idempotent.aspect;

import io.github.ayongbox.baseon.core.annotation.Idempotent;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.spi.CacheKeyPrefixProvider;
import io.github.ayongbox.baseon.core.tool.StrUtil;
import io.github.ayongbox.baseon.starter.idempotent.config.BaseonIdempotentProperties;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * {@code @Idempotent} 切面：Redis 原子占位（SET NX EX）实现幂等控制。
 *
 * <p>幂等 key 组装：{@code idem:} + [租户前缀（引入 starter-tenant 时）] + 类简单名.方法名
 * + {@code :} + 求值结果。求值结果按三级来源取：
 * <ol>
 *   <li>注解 {@code key()} 非空：SpEL 对方法参数求值（如 {@code "#name"}）；</li>
 *   <li>否则读当前请求的幂等令牌头（{@code baseon.idempotent.token-header}）；</li>
 *   <li>两者都无：退化为方法级幂等——同一方法的重复调用整体互斥，不再追加后缀。</li>
 * </ol>
 *
 * <p>占位成功即放行，<b>不主动删 key</b>：TTL 自然过期即清理，符合防重复提交语义
 * （执行失败也占用窗口，避免失败重试与正常重试难以区分导致重复写入）。
 */
@Aspect
public class IdempotentAspect {

    /** 幂等 key 的命名空间前缀 */
    private static final String KEY_NAMESPACE = "idem:";

    /** 占位值无业务含义，固定 "1" */
    private static final String PLACEHOLDER_VALUE = "1";

    private final RedisUtil redisUtil;
    private final BaseonIdempotentProperties properties;
    private final ObjectProvider<CacheKeyPrefixProvider> prefixProvider;
    private final ExpressionParser spelParser = new SpelExpressionParser();

    public IdempotentAspect(RedisUtil redisUtil, BaseonIdempotentProperties properties,
                            ObjectProvider<CacheKeyPrefixProvider> prefixProvider) {
        this.redisUtil = redisUtil;
        this.properties = properties;
        this.prefixProvider = prefixProvider;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String key = buildKey(signature, pjp.getArgs(), idempotent);
        long ttlSeconds = idempotent.ttlSeconds() > 0
                ? idempotent.ttlSeconds()
                : properties.getDefaultTtlSeconds();

        Boolean acquired = redisUtil.setIfAbsent(key, PLACEHOLDER_VALUE, ttlSeconds);
        if (Boolean.FALSE.equals(acquired)) {
            if (StrUtil.isNotBlank(idempotent.message())) {
                throw new BizException(CommonErrorCode.DUPLICATE_REQUEST, idempotent.message());
            }
            throw new BizException(CommonErrorCode.DUPLICATE_REQUEST);
        }
        return pjp.proceed();
    }

    /**
     * 组装幂等 key：{@code idem:} + [租户前缀] + 类简单名.方法名 + [:求值结果]。
     * 全局前缀与 key 规范校验由 {@link RedisUtil} 内部统一处理，这里不重复拼。
     */
    private String buildKey(MethodSignature signature, Object[] args, Idempotent idempotent) {
        StringBuilder key = new StringBuilder(KEY_NAMESPACE);
        CacheKeyPrefixProvider provider = prefixProvider.getIfAvailable();
        if (provider != null) {
            key.append(provider.prefix());
        }
        Method method = signature.getMethod();
        key.append(method.getDeclaringClass().getSimpleName()).append('.').append(method.getName());
        String evaluated = evaluateKey(signature, args, idempotent);
        if (StrUtil.isNotBlank(evaluated)) {
            key.append(':').append(evaluated);
        }
        return key.toString();
    }

    /**
     * 按三级来源求幂等后缀：注解 SpEL → 请求头令牌 → 退化为 null（方法级幂等）。
     */
    private String evaluateKey(MethodSignature signature, Object[] args, Idempotent idempotent) {
        if (StrUtil.isNotBlank(idempotent.key())) {
            EvaluationContext context = new StandardEvaluationContext();
            String[] names = signature.getParameterNames();
            for (int i = 0; i < names.length; i++) {
                context.setVariable(names[i], args[i]);
            }
            Object value = spelParser.parseExpression(idempotent.key()).getValue(context);
            return value == null ? null : String.valueOf(value);
        }
        return currentRequestToken();
    }

    /** 从当前请求读幂等令牌头；非 Web 场景（定时任务、消息消费）无请求上下文，返回 null */
    private String currentRequestToken() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest().getHeader(properties.getTokenHeader());
        }
        return null;
    }
}
