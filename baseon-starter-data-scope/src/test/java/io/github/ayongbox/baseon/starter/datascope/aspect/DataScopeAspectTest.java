package io.github.ayongbox.baseon.starter.datascope.aspect;

import io.github.ayongbox.baseon.core.context.DataScopeIgnoreContext;
import io.github.ayongbox.baseon.core.spi.DataScopeProvider;
import io.github.ayongbox.baseon.starter.datascope.config.BaseonDataScopeProperties;
import io.github.ayongbox.baseon.starter.datascope.support.DataScopeContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataScopeAspect} 上下文生命周期验证：进入方法时写入、退出时清理、
 * 嵌套调用外层现场恢复、@IgnoreDataScope 与 @DataScope 互不干扰。
 */
class DataScopeAspectTest {

    private final DataScopeProvider provider = new DataScopeProvider() {
        @Override
        public Set<Long> visibleIds() {
            return Set.of(1L, 2L);
        }
    };
    private final BaseonDataScopeProperties properties = new BaseonDataScopeProperties();
    private final DataScopeAspect aspect = new DataScopeAspect(provider, properties);
    private final DataScopeIgnoreAspect ignoreAspect = new DataScopeIgnoreAspect();

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
        DataScopeIgnoreContext.clearIgnored();
    }

    @Test
    void shouldSetAndClearContextAroundMethod() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("annotatedMethod");
        Mockito.when(pjp.proceed()).thenAnswer(inv -> {
            DataScopeContext ctx = DataScopeContext.get();
            assertThat(ctx).isNotNull();
            assertThat(ctx.column()).isEqualTo("dept_id");
            assertThat(ctx.visibleIds()).containsExactlyInAnyOrder(1L, 2L);
            return "ok";
        });

        Object result = aspect.around(pjp);
        assertThat(result).isEqualTo("ok");
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    void shouldClearContextEvenWhenMethodThrows() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("annotatedMethod");
        Mockito.when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        try {
            aspect.around(pjp);
        } catch (IllegalStateException expected) {
            // fallthrough
        }
        assertThat(DataScopeContext.get()).isNull();
    }

    @Test
    void shouldRestoreOuterContextAfterNestedCall() throws Throwable {
        DataScopeContext outer = DataScopeContext.of("dept_id", null, Set.of(99L));
        DataScopeContext.set(outer);

        ProceedingJoinPoint pjp = mockPjp("customColumnMethod");
        Mockito.when(pjp.proceed()).thenAnswer(inv -> {
            // 内层看到的是内层注解的 column
            assertThat(DataScopeContext.get().column()).isEqualTo("project_id");
            return "ok";
        });

        aspect.around(pjp);

        // 内层退出后恢复外层
        assertThat(DataScopeContext.get()).isSameAs(outer);
    }

    @Test
    void shouldPreferAnnotationColumnOverProviderDefault() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("customColumnMethod");
        Mockito.when(pjp.proceed()).thenAnswer(inv -> {
            assertThat(DataScopeContext.get().column()).isEqualTo("project_id");
            return "ok";
        });
        aspect.around(pjp);
    }

    @Test
    void shouldResolveClassLevelAnnotationWhenMethodNotAnnotated() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("plainMethodInAnnotatedClass");
        Mockito.when(pjp.proceed()).thenAnswer(inv -> {
            assertThat(DataScopeContext.get()).isNotNull();
            assertThat(DataScopeContext.get().column()).isEqualTo("org_id");
            return "ok";
        });
        aspect.around(pjp);
    }

    @Test
    void shouldSetAndClearIgnoreFlagAroundIgnoreAnnotation() throws Throwable {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(pjp.proceed()).thenAnswer(inv -> {
            assertThat(DataScopeIgnoreContext.isIgnored()).isTrue();
            return "ok";
        });
        ignoreAspect.around(pjp);
        assertThat(DataScopeIgnoreContext.isIgnored()).isFalse();
    }

    /**
     * 构造最小可用的 ProceedingJoinPoint：只 mock 出 MethodSignature，
     * 返回 FixtureClass 上指定名字的 Method（注解随之可达）。
     */
    private ProceedingJoinPoint mockPjp(String methodName) throws NoSuchMethodException {
        Method method = FixtureClass.class.getDeclaredMethod(methodName);
        MethodSignature signature = Mockito.mock(MethodSignature.class);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(signature.getDeclaringType()).thenReturn(FixtureClass.class);

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(pjp.getSignature()).thenReturn(signature);
        return pjp;
    }

    /** 测试 fixture：提供方法级/类级 @DataScope 注解样本 */
    @SuppressWarnings("unused")
    @io.github.ayongbox.baseon.core.annotation.DataScope(column = "org_id")
    static class FixtureClass {

        @io.github.ayongbox.baseon.core.annotation.DataScope
        public void annotatedMethod() {
        }

        @io.github.ayongbox.baseon.core.annotation.DataScope(column = "project_id")
        public void customColumnMethod() {
        }

        public void plainMethodInAnnotatedClass() {
        }
    }
}
