package io.github.ayongbox.baseon.starter.loadbalance;

import io.github.ayongbox.baseon.core.cloud.CloudHeaders;
import io.github.ayongbox.baseon.core.context.GrayContext;
import io.github.ayongbox.baseon.starter.loadbalance.config.BaseonLoadBalanceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 灰度标签路由负载均衡器，规则：
 *
 * <ol>
 *   <li>请求带 {@code X-Gray-Tag}：只路由到元数据 {@code gray-tag} 相同的实例；
 *       无匹配实例时回退到全部实例并打 WARN（宁可降级可用，不让灰度流量 503）</li>
 *   <li>请求不带标签：排除所有灰度实例（保护灰度环境不被普通流量打进去），
 *       并排除 {@code baseon.local=true} 的本地调试实例（除非显式 allow-local）</li>
 *   <li>候选实例多于一个时轮询</li>
 * </ol>
 */
public class GrayTagLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final Logger log = LoggerFactory.getLogger(GrayTagLoadBalancer.class);

    private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
    private final String serviceId;
    private final BaseonLoadBalanceProperties properties;
    private final AtomicInteger position;

    public GrayTagLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
                               String serviceId, BaseonLoadBalanceProperties properties) {
        this.supplierProvider = supplierProvider;
        this.serviceId = serviceId;
        this.properties = properties;
        this.position = new AtomicInteger(new Random().nextInt(1000));
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = supplierProvider.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }
        return supplier.get(request).next().map(this::select);
    }

    Response<ServiceInstance> select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return new EmptyResponse();
        }
        List<ServiceInstance> candidates = filter(instances);
        if (candidates.isEmpty()) {
            log.warn("[baseon] 服务 {} 无匹配实例（grayTag={}），回退到全部 {} 个实例",
                    serviceId, GrayContext.get(), instances.size());
            candidates = instances;
        }
        int pos = Math.floorMod(position.incrementAndGet(), candidates.size());
        return new DefaultResponse(candidates.get(pos));
    }

    /**
     * 实例过滤主逻辑，包私有便于单测。
     */
    List<ServiceInstance> filter(List<ServiceInstance> instances) {
        if (!properties.isEnabled()) {
            return instances;
        }
        String tag = GrayContext.get();
        if (tag != null && !tag.isBlank()) {
            return instances.stream()
                    .filter(instance -> tag.equals(instance.getMetadata().get(CloudHeaders.GRAY_TAG_METADATA)))
                    .toList();
        }
        return instances.stream()
                .filter(instance -> !instance.getMetadata().containsKey(CloudHeaders.GRAY_TAG_METADATA))
                .filter(instance -> properties.isAllowLocal() || !isLocalInstance(instance))
                .toList();
    }

    private boolean isLocalInstance(ServiceInstance instance) {
        return "true".equalsIgnoreCase(instance.getMetadata().get(CloudHeaders.LOCAL_METADATA));
    }
}
