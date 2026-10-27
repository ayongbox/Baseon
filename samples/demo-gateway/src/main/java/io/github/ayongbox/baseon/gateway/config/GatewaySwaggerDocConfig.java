package io.github.ayongbox.baseon.gateway.config;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 网关 Swagger 聚合配置。
 *
 * <p>从注册中心（DiscoveryClient）拉取在线服务列表，把每个服务的
 * {@code /{serviceId}/v3/api-docs} 动态写入 springdoc 的 Swagger UI 下拉列表，
 * UI 右上角即可切换各服务文档。文档地址经由网关 discovery locator 路由
 * （{@code /{serviceId}/**}）转发到目标服务，因此业务服务只需保留
 * /v3/api-docs 端点，UI 统一收敛到本网关。
 *
 * <p>刷新时机：启动时初始化一次（ApplicationRunner）；
 * 之后服务上下线触发 Nacos 心跳，容器发布 {@link HeartbeatEvent} 时重建列表。
 *
 * <p>实现要点（springdoc 2.5.0）：swagger-config 端点每次请求都会从
 * {@link SwaggerUiConfigProperties#getUrls()} 增量同步到实际出参的
 * {@link SwaggerUiConfigParameters}（按 name 去重覆盖，只增不删），
 * 因此两个 Bean 必须同时 setUrls——前者保证 URL 不被改写、后者保证下线服务立即从下拉消失。
 */
@Component
public class GatewaySwaggerDocConfig implements ApplicationRunner {

    /**
     * springdoc 的 api-docs 默认路径，与业务服务侧保持一致
     */
    private static final String API_DOCS_PATH = "/v3/api-docs";

    private final DiscoveryClient discoveryClient;

    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    private final SwaggerUiConfigParameters swaggerUiConfigParameters;

    /**
     * 网关自身应用名，用于从聚合列表中排除（网关无业务接口文档）
     */
    private final String gatewayServiceId;

    public GatewaySwaggerDocConfig(DiscoveryClient discoveryClient,
                                   SwaggerUiConfigProperties swaggerUiConfigProperties,
                                   SwaggerUiConfigParameters swaggerUiConfigParameters,
                                   @Value("${spring.application.name}") String gatewayServiceId) {
        this.discoveryClient = discoveryClient;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        this.swaggerUiConfigParameters = swaggerUiConfigParameters;
        this.gatewayServiceId = gatewayServiceId;
    }

    @Override
    public void run(ApplicationArguments args) {
        refreshSwaggerUrls();
    }

    /**
     * Nacos 心跳/服务实例上下线时容器会发布 HeartbeatEvent，据此重建聚合列表
     */
    @EventListener(HeartbeatEvent.class)
    public void onHeartbeat(HeartbeatEvent event) {
        refreshSwaggerUrls();
    }

    private void refreshSwaggerUrls() {
        Set<SwaggerUrl> urls = discoveryClient.getServices().stream()
                // 网关自身没有业务接口文档，排除
                .filter(serviceId -> !serviceId.equalsIgnoreCase(gatewayServiceId))
                // locator 开启了 lower-case-service-id，路由路径一律小写
                .map(serviceId -> new SwaggerUrl(serviceId,
                        "/" + serviceId.toLowerCase(Locale.ROOT) + API_DOCS_PATH, null))
                .collect(Collectors.toSet());
        // SwaggerUrl 的 equals/hashCode 基于 name，整体替换即可覆盖增删改三种场景
        swaggerUiConfigProperties.setUrls(urls);
        swaggerUiConfigParameters.setUrls(urls);
    }
}
