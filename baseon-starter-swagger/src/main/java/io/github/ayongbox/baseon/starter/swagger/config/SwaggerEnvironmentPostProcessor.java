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
 * Swagger/Knife4j 端点启停的统一环境后处理器。
 *
 * <p>三层职责：
 * <ol>
 *     <li><b>显式关闭</b>：{@code baseon.swagger.enabled=false} 时，把 {@code knife4j.enable}
 *         置 false（Knife4j 4.x 的 {@code enable} 默认 false，显式关闭语义更明确）。</li>
 *     <li><b>生产硬关闭</b>：激活 profile 命中 {@code baseon.swagger.prod-profiles} 时，
 *         强制关闭 springdoc 的 api-docs / swagger-ui 端点，并把 {@code knife4j.enable=false}、
 *         {@code knife4j.production=true}，确保 {@code /doc.html} 与规范端点全部失效。</li>
 *     <li><b>非生产自动开启</b>：非生产且未显式关闭时，把 {@code knife4j.enable=true}——
 *         Knife4j 4.x 的 {@code enable} 默认为 false，不显式开启则 {@code /doc.html}
 *         不会渲染，与「starter 引入即用」的约定冲突，由本处理器兜底。</li>
 * </ol>
 *
 * <p>所有覆盖均以 {@link MapPropertySource} 注入并 {@code addFirst}（最高优先级），
 * 覆盖业务在 Nacos/yml 中对同名键的任何误配置。
 *
 * <p>实现 {@link Ordered} 返回最低优先级，保证本处理器尽量晚执行，覆盖其他先行处理器的注入。
 *
 * <p>注册方式：{@code META-INF/spring.factories}
 * （Boot 3 的 EnvironmentPostProcessor 仍走 spring.factories）。
 */
public class SwaggerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SwaggerEnvironmentPostProcessor.class);

    static final String PROPERTY_SOURCE_NAME = "baseonSwaggerEndpointOverrides";

    static final String API_DOCS_ENABLED_KEY = "springdoc.api-docs.enabled";

    static final String SWAGGER_UI_ENABLED_KEY = "springdoc.swagger-ui.enabled";

    static final String KNIFE4J_ENABLED_KEY = "knife4j.enable";

    static final String KNIFE4J_PRODUCTION_KEY = "knife4j.production";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        BaseonSwaggerProperties properties = Binder.get(environment)
                .bind("baseon.swagger", BaseonSwaggerProperties.class)
                .orElse(new BaseonSwaggerProperties());

        Map<String, Object> overrides = new HashMap<>();

        if (!properties.isEnabled()) {
            // 显式关闭：只关 Knife4j，springdoc 端点保持业务自身配置（避免越俎代庖）
            overrides.put(KNIFE4J_ENABLED_KEY, "false");
            log.info("[baseon] baseon.swagger.enabled=false，已关闭 Knife4j");
        } else if (BaseonSwaggerAutoConfiguration.isProdActive(environment, properties.getProdProfiles())) {
            // 生产硬关闭：api-docs + swagger-ui + Knife4j 全部失效
            overrides.put(API_DOCS_ENABLED_KEY, "false");
            overrides.put(SWAGGER_UI_ENABLED_KEY, "false");
            overrides.put(KNIFE4J_ENABLED_KEY, "false");
            overrides.put(KNIFE4J_PRODUCTION_KEY, "true");
            log.warn("[baseon] 检测到生产 profile，已强制关闭 springdoc api-docs / swagger-ui 与 Knife4j");
        } else {
            // 非生产：Knife4j 4.x 的 enable 默认 false，必须显式打开才能渲染 /doc.html
            overrides.put(KNIFE4J_ENABLED_KEY, "true");
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
        }
    }

    /**
     * 靠后执行：保证本处理器注入的覆盖属性最后落位，覆盖一切先行的开启配置。
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
