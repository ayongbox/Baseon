package io.github.ayongbox.baseon.starter.swagger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;
import java.util.Set;

/**
 * Swagger/OpenAPI 自动装配入口。
 *
 * <p>装配一个全局 {@link OpenAPI} 定制 Bean：info 从 {@code baseon.swagger.*} 读取，
 * 并注册名为 {@code bearerAuth} 的 HTTP Bearer（JWT）安全方案、全局生效——
 * 与 baseon-starter-auth 联动，Swagger UI 右上角 Authorize 粘贴 token 即可调试受保护接口。
 *
 * <p>生产防护第一层（定制层）：激活 profile 命中
 * {@code baseon.swagger.prod-profiles} 名单时，整个自动配置类被
 * {@link NonProdProfileCondition} 拦下，不装配任何定制 Bean。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "baseon.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
@Conditional(BaseonSwaggerAutoConfiguration.NonProdProfileCondition.class)
@EnableConfigurationProperties(BaseonSwaggerProperties.class)
public class BaseonSwaggerAutoConfiguration {

    /**
     * 全局 Bearer 安全方案名，Swagger UI Authorize 弹窗中对应项。
     */
    static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean(name = "baseonOpenAPI")
    public OpenAPI baseonOpenAPI(BaseonSwaggerProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getTitle())
                        .description(properties.getDescription())
                        .version(properties.getVersion()))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }

    /**
     * 生产 profile 判定：激活 profile 与 prodProfiles 名单存在交集即视为生产。
     * prod 与 dev 并存时同样命中（宁可错杀）。名单为空/null 时永不命中。
     */
    static boolean isProdActive(Environment env, List<String> prodProfiles) {
        if (prodProfiles == null || prodProfiles.isEmpty()) {
            return false;
        }
        Set<String> activeProfiles = Set.of(env.getActiveProfiles());
        for (String prodProfile : prodProfiles) {
            if (activeProfiles.contains(prodProfile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生产防护第一层：命中生产 profile 时整个自动配置不生效，并打 WARN。
     * prodProfiles 名单在条件求值阶段通过 {@link Binder} 现场绑定，支持自定义。
     */
    static class NonProdProfileCondition implements Condition {

        private static final Logger log = LoggerFactory.getLogger(NonProdProfileCondition.class);

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            BaseonSwaggerProperties properties = Binder.get(environment)
                    .bind("baseon.swagger", BaseonSwaggerProperties.class)
                    .orElse(new BaseonSwaggerProperties());
            boolean prodActive = isProdActive(environment, properties.getProdProfiles());
            if (prodActive) {
                log.warn("[baseon] 检测到生产 profile，Swagger 定制已禁用");
            }
            return !prodActive;
        }
    }
}
