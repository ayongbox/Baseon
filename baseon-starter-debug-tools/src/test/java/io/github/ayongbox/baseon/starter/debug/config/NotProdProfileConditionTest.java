package io.github.ayongbox.baseon.starter.debug.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NotProdProfileCondition 的装配期拦截：prod 下不匹配（端点不注册），dev 下匹配。
 */
class NotProdProfileConditionTest {

    private final NotProdProfileCondition condition = new NotProdProfileCondition();

    @Test
    void prodProfileDoesNotMatch() {
        // Given：激活 prod profile 的环境
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);

        // When：执行条件判定
        boolean matched = condition.getMatchOutcome(context, mock(AnnotatedTypeMetadata.class)).isMatch();

        // Then：不匹配，端点 Bean 不注册
        assertFalse(matched);
    }

    @Test
    void devProfileMatches() {
        // Given：仅激活 dev profile 的环境
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);

        // When：执行条件判定
        boolean matched = condition.getMatchOutcome(context, mock(AnnotatedTypeMetadata.class)).isMatch();

        // Then：匹配，端点 Bean 注册
        assertTrue(matched);
    }

    @Test
    void customProdProfilesFromEnvironmentDoNotMatch() {
        // Given：配置自定义生产名单 baseon.debug.prod-profiles=production，并激活 production
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.debug.prod-profiles", "production");
        environment.setActiveProfiles("production");
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);

        // When：执行条件判定
        boolean matched = condition.getMatchOutcome(context, mock(AnnotatedTypeMetadata.class)).isMatch();

        // Then：Condition 与运行期使用同一份名单，同样拦截
        assertFalse(matched);
    }
}
