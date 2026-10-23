package io.github.ayongbox.baseon.starter.loadbalance.config;

import io.github.ayongbox.baseon.starter.loadbalance.filter.GrayContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;

/**
 * 负载均衡治理自动装配：灰度路由对所有通过 LoadBalancer 的调用生效
 * （Feign、RestTemplate、WebClient），灰度标签入口过滤器只在 Servlet 环境装配。
 */
@AutoConfiguration
@ConditionalOnClass(LoadBalancerClientFactory.class)
@EnableConfigurationProperties(BaseonLoadBalanceProperties.class)
@LoadBalancerClients(defaultConfiguration = BaseonLoadBalancerConfiguration.class)
public class BaseonLoadBalanceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public GrayContextFilter grayContextFilter() {
        return new GrayContextFilter();
    }
}
