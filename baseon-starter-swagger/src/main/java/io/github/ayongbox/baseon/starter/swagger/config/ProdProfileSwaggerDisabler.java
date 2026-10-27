package io.github.ayongbox.baseon.starter.swagger.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 生产防护第二层（端点层）：springdoc 端点的硬关闭开关。
 *
 * <p>检测到生产 profile（激活 profile 与 {@code baseon.swagger.prod-profiles}
 * 名单有交集）时，把 {@code springdoc.api-docs.enabled=false} 与
 * {@code springdoc.swagger-ui.enabled=false} 以 {@link MapPropertySource}
 * 塞进 Environment 并放最前（addFirst，最高优先级），强制覆盖业务在
 * Nacos/yml 中对 springdoc 的任何误配置，使 /v3/api-docs 与 /swagger-ui 端点彻底失效。
 *
 * <p>实现 {@link Ordered} 返回最低优先级，保证本处理器尽量晚执行——
 * 其他 EnvironmentPostProcessor 即使也往最前塞 springdoc 配置，也会被本处理器覆盖。
 *
 * <p>注册方式：{@code META-INF/spring.factories}
 * （Boot 3 的 EnvironmentPostProcessor 仍走 spring.factories）。
 */
public class ProdProfileSwaggerDisabler implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ProdProfileSwaggerDisabler.class);

    static final String PROPERTY_SOURCE_NAME = "baseonSwaggerProdDisabler";

    static final String API_DOCS_ENABLED_KEY = "springdoc.api-docs.enabled";

    static final String SWAGGER_UI_ENABLED_KEY = "springdoc.swagger-ui.enabled";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        BaseonSwaggerProperties properties = Binder.get(environment)
                .bind("baseon.swagger", BaseonSwaggerProperties.class)
                .orElse(new BaseonSwaggerProperties());
        if (!BaseonSwaggerAutoConfiguration.isProdActive(environment, properties.getProdProfiles())) {
            return;
        }

        Map<String, Object> overrides = new HashMap<>();
        overrides.put(API_DOCS_ENABLED_KEY, "false");
        overrides.put(SWAGGER_UI_ENABLED_KEY, "false");
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
        log.warn("[baseon] 检测到生产 profile，已强制关闭 springdoc api-docs 与 swagger-ui 端点");
    }

    /**
     * 靠后执行：保证本处理器注入的关闭属性最后落位，覆盖一切先行的开启配置。
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
