package io.github.ayongbox.baseon.starter.loadbalance.config;

import io.github.ayongbox.baseon.starter.loadbalance.GrayTagLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 负载均衡器装配：由 {@code @LoadBalancerClients(defaultConfiguration=...)} 引用，
 * 在每个服务的 LoadBalancer 子上下文中生效，<b>不能</b>被组件扫描到，
 * 否则会对所有服务实例重复注册并覆盖业务的自定义配置。
 */
public class BaseonLoadBalancerConfiguration {

    @Bean
    public ReactorServiceInstanceLoadBalancer grayTagLoadBalancer(
            Environment environment, LoadBalancerClientFactory loadBalancerClientFactory,
            BaseonLoadBalanceProperties properties) {
        String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new GrayTagLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class),
                serviceId, properties);
    }
}
