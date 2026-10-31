package io.github.ayongbox.baseon.starter.datascope.aspect;

import io.github.ayongbox.baseon.core.annotation.DataScope;
import io.github.ayongbox.baseon.core.spi.DataScopeProvider;
import io.github.ayongbox.baseon.starter.datascope.config.BaseonDataScopeProperties;
import io.github.ayongbox.baseon.starter.datascope.support.DataScopeContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * {@code @DataScope} 切面：方法执行期间，从 {@link DataScopeProvider} 拉取当前用户
 * 可见的数据 ID 集合，连同注解上的 column/tableAlias 一并写入 {@link DataScopeContext}，
 * 供行级 SQL 拦截器读取；方法退出（含异常）时清理，避免线程复用串号。
 *
 * <p>嵌套语义：内层 @DataScope 覆盖外层（用线程栈保存外层现场，退出时恢复），
 * 避免嵌套调用时内层条件被外层意外清空。
 */
@Aspect
public class DataScopeAspect {

    private final DataScopeProvider dataScopeProvider;
    private final BaseonDataScopeProperties properties;

    public DataScopeAspect(DataScopeProvider dataScopeProvider, BaseonDataScopeProperties properties) {
        this.dataScopeProvider = dataScopeProvider;
        this.properties = properties;
    }

    @Around("@annotation(io.github.ayongbox.baseon.core.annotation.DataScope)"
            + " || @within(io.github.ayongbox.baseon.core.annotation.DataScope)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        DataScope dataScope = resolveAnnotation(pjp);
        DataScopeContext previous = DataScopeContext.get();

        String column = StringUtils.hasText(dataScope.column())
                ? dataScope.column()
                : defaultColumn();
        String tableAlias = StringUtils.hasText(dataScope.tableAlias())
                ? dataScope.tableAlias()
                : null;
        Set<Long> visibleIds = dataScopeProvider.visibleIds();

        DataScopeContext.set(DataScopeContext.of(column, tableAlias, visibleIds));
        try {
            return pjp.proceed();
        } finally {
            if (previous != null) {
                DataScopeContext.set(previous);
            } else {
                DataScopeContext.clear();
            }
        }
    }

    /**
     * 同时支持方法级 {@code @annotation} 与类级 {@code @within}：
     * 优先取方法上的注解，未标注时回退到声明类上的注解。
     *
     * <p>{@link MethodSignature#getDeclaringType()} 返回原生 {@code Class}，
     * 因此 {@code getAnnotation} 返回原始 {@link java.lang.annotation.Annotation}，
     * 需要显式强转为 {@link DataScope}（编译期可控，注解类型必然匹配）。
     */
    private DataScope resolveAnnotation(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        DataScope annotation = signature.getMethod().getAnnotation(DataScope.class);
        if (annotation != null) {
            return annotation;
        }
        return (DataScope) signature.getDeclaringType().getAnnotation(DataScope.class);
    }

    private String defaultColumn() {
        String providerColumn = dataScopeProvider.column();
        return StringUtils.hasText(providerColumn) ? providerColumn : properties.getColumn();
    }
}
