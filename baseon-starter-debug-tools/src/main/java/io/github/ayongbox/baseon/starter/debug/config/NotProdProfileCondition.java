package io.github.ayongbox.baseon.starter.debug.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;

/**
 * 生产硬失效第一层：装配期判定。命中生产 profile 时端点 Bean 不注册，
 * 并打 WARN 日志留痕（第三层），避免生产排障时「端点 404 却查不到原因」。
 *
 * <p>Condition 执行时 {@link BaseonDebugProperties} 尚未绑定，这里用 {@link Binder}
 * 单独绑定 {@code baseon.debug.prod-profiles}，保证 Condition 与运行期判定使用同一份名单。
 */
public class NotProdProfileCondition extends SpringBootCondition {

    private static final Logger log = LoggerFactory.getLogger(NotProdProfileCondition.class);

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        List<String> prodProfiles = Binder.get(context.getEnvironment())
                .bind("baseon.debug.prod-profiles", Bindable.listOf(String.class))
                .orElseGet(() -> List.of("prod"));
        if (DebugProdProfileGuard.isProdActive(context.getEnvironment(), prodProfiles)) {
            log.warn("检测到生产 profile，debug 端点已硬失效");
            return ConditionOutcome.noMatch("生产 profile 激活，debug 端点硬失效");
        }
        return ConditionOutcome.match("未检测到生产 profile");
    }
}
