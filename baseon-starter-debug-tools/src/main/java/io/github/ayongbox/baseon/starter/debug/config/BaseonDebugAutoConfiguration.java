package io.github.ayongbox.baseon.starter.debug.config;

import io.github.ayongbox.baseon.starter.debug.endpoint.DebugEndpointController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;

/**
 * 调试端点自动装配入口。
 *
 * <p>设计意图：debug 端点暴露当前登录上下文（userId/tenantId/permissions）、traceId
 * 与运行配置，便于开发联调时自检「拦截器有没有正确解析上下文」。这类信息在生产环境
 * 属于信息泄露面，因此生产失效必须是「硬失效」（profile 命中即不装配，且运行期二次拦截），
 * 而不是一个可以被配置中心误改回去的普通开关。
 *
 * <p>三层硬失效：
 * <ol>
 *   <li>装配期：{@link ConditionalOnProperty} 处理 {@code baseon.debug.enabled}，
 *       {@link NotProdProfileCondition} 处理生产 profile 判定，两者叠加，任一不满足则端点不存在；</li>
 *   <li>运行期：{@link DebugEndpointController} 每个端点入口再执行一次
 *       {@link DebugProdProfileGuard#isProdActive}，防止误判装配成功后的泄露；</li>
 *   <li>留痕：装配被生产 profile 拦截时输出 WARN 日志。</li>
 * </ol>
 *
 * <p>端点类放在独立的 endpoint 包，由本配置以 {@link Bean} 方式注册，不被业务组件扫描命中，
 * 保证「未启用时端点不存在」，而非「存在但返回拒绝」。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(BaseonDebugProperties.class)
public class BaseonDebugAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.debug", name = "enabled", havingValue = "true", matchIfMissing = true)
    @Conditional(NotProdProfileCondition.class)
    public DebugEndpointController debugEndpointController(Environment environment,
                                                           BaseonDebugProperties properties) {
        return new DebugEndpointController(environment, properties);
    }
}
