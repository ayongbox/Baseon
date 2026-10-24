package io.github.ayongbox.baseon.starter.datasource.aspect;

import io.github.ayongbox.baseon.starter.datasource.annotation.DS;
import io.github.ayongbox.baseon.starter.datasource.support.DataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * {@link DS} 注解切面：进入方法前把注解值写入 {@link DataSourceContextHolder}，
 * 方法返回或抛异常后在 finally 中清理，防止线程池复用串号。
 *
 * <p>切点用全限定类名匹配方法与类两个层级，注解解析时方法级优先于类级。
 */
@Aspect
public class DataSourceSwitchAspect {

    @Around("@annotation(io.github.ayongbox.baseon.starter.datasource.annotation.DS)"
            + " || @within(io.github.ayongbox.baseon.starter.datasource.annotation.DS)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        DS ds = resolveAnnotation(pjp);
        if (ds == null) {
            return pjp.proceed();
        }
        DataSourceContextHolder.set(ds.value());
        try {
            return pjp.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    /**
     * 方法级注解优先；方法上没有时回退到声明类的类级注解。
     */
    private DS resolveAnnotation(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        DS ds = AnnotationUtils.findAnnotation(signature.getMethod(), DS.class);
        if (ds == null) {
            ds = AnnotationUtils.findAnnotation(signature.getDeclaringType(), DS.class);
        }
        return ds;
    }
}
