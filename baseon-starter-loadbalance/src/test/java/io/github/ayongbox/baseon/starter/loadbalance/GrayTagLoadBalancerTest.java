package io.github.ayongbox.baseon.starter.loadbalance;

import io.github.ayongbox.baseon.core.cloud.CloudHeaders;
import io.github.ayongbox.baseon.core.context.GrayContext;
import io.github.ayongbox.baseon.starter.loadbalance.config.BaseonLoadBalanceProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GrayTagLoadBalancerTest {

    private final BaseonLoadBalanceProperties properties = new BaseonLoadBalanceProperties();
    private final GrayTagLoadBalancer balancer = new GrayTagLoadBalancer(null, "demo-service-b", properties);

    private final ServiceInstance normal = instance("n1", Map.of());
    private final ServiceInstance gray = instance("g1", Map.of(CloudHeaders.GRAY_TAG_METADATA, "gray"));
    private final ServiceInstance canary = instance("c1", Map.of(CloudHeaders.GRAY_TAG_METADATA, "canary"));
    private final ServiceInstance local = instance("l1", Map.of(CloudHeaders.LOCAL_METADATA, "true"));

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Test
    void noTagExcludesGrayAndLocalInstances() {
        List<ServiceInstance> result = balancer.filter(List.of(normal, gray, canary, local));
        assertThat(result).containsExactly(normal);
    }

    @Test
    void withTagSelectsOnlyMatchingInstances() {
        GrayContext.set("gray");
        List<ServiceInstance> result = balancer.filter(List.of(normal, gray, canary));
        assertThat(result).containsExactly(gray);
    }

    @Test
    void withUnknownTagMatchesNothing() {
        GrayContext.set("not-exist");
        List<ServiceInstance> result = balancer.filter(List.of(normal, gray));
        assertThat(result).isEmpty();
    }

    @Test
    void allowLocalKeepsLocalInstances() {
        properties.setAllowLocal(true);
        List<ServiceInstance> result = balancer.filter(List.of(normal, gray, local));
        assertThat(result).containsExactlyInAnyOrder(normal, local);
    }

    @Test
    void disabledSkipsFiltering() {
        properties.setEnabled(false);
        GrayContext.set("gray");
        List<ServiceInstance> all = List.of(normal, gray, canary, local);
        assertThat(balancer.filter(all)).isEqualTo(all);
    }

    @Test
    void selectFallsBackToAllWhenNoMatch() {
        GrayContext.set("not-exist");
        assertThat(balancer.select(List.of(normal)).getServer()).isEqualTo(normal);
    }

    @Test
    void selectRoundRobinsCandidates() {
        ServiceInstance n2 = instance("n2", Map.of());
        assertThat(balancer.select(List.of(normal, n2)).getServer()).isIn(normal, n2);
    }

    private static DefaultServiceInstance instance(String id, Map<String, String> metadata) {
        return new DefaultServiceInstance(id, "demo-service-b", "127.0.0.1", 8081, false, metadata);
    }
}
