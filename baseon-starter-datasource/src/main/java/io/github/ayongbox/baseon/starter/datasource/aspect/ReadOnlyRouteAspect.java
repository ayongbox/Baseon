package io.github.ayongbox.baseon.starter.datasource.aspect;

import io.github.ayongbox.baseon.starter.datasource.support.DataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

/**
 * 读写分离前置探测：在事务开启<b>之前</b>识别 {@code @Transactional(readOnly = true)}，
 * 把只读标记写入 {@link DataSourceContextHolder}，让路由数据源切换到从库。
 *
 * <p>为什么必须存在：{@code DataSourceTransactionManager} 在 {@code doBegin()} 获取连接时，
 * readOnly 标记尚未注册到 {@link TransactionSynchronizationManager}（prepareSynchronization
 * 在 doBegin 之后才执行），路由数据源在事务内永远读不到 readOnly=true。
 * 因此只能在更高优先级的 AOP 里读注解元数据完成路由决策。
 *
 * <p>两种主动放弃路由的情形：
 * <ol>
 *   <li>已有显式数据源上下文（@DS 优先）；</li>
 *   <li>已存在事务同步（嵌套调用：连接由外层事务绑定，切换无效且危险）。</li>
 * </ol>
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReadOnlyRouteAspect {

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)"
            + " || @within(org.springframework.transaction.annotation.Transactional)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Transactional transactional = resolveTransactional(pjp);
        if (transactional == null || !transactional.readOnly()) {
            return pjp.proceed();
        }
        if (DataSourceContextHolder.get() != null
                || TransactionSynchronizationManager.isSynchronizationActive()) {
            return pjp.proceed();
        }
        DataSourceContextHolder.set(DataSourceContextHolder.READ_ONLY);
        try {
            return pjp.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    /**
     * 方法级注解优先，类级兜底；代理调用时从目标类解析真实方法上的注解。
     */
    private Transactional resolveTransactional(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
        if (transactional == null) {
            transactional = AnnotatedElementUtils.findMergedAnnotation(
                    pjp.getTarget().getClass(), Transactional.class);
        }
        return transactional;
    }
}
