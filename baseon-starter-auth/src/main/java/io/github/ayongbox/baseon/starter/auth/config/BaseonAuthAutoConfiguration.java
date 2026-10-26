package io.github.ayongbox.baseon.starter.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ayongbox.baseon.core.secure.AuthProvider;
import io.github.ayongbox.baseon.core.secure.PermissionChecker;
import io.github.ayongbox.baseon.starter.auth.aspect.RequiresPermissionAspect;
import io.github.ayongbox.baseon.starter.auth.filter.AuthTokenFilter;
import io.github.ayongbox.baseon.starter.auth.support.DefaultPermissionChecker;
import io.github.ayongbox.baseon.starter.auth.support.JwtTokenProvider;
import io.github.ayongbox.baseon.starter.auth.support.TokenService;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * JWT 鉴权自动装配。
 *
 * <p>{@link TokenService} 以 {@link AuthProvider} 注册：业务接入企业统一认证
 * （OAuth2 / CAS / SSO）时声明自己的 {@link AuthProvider} Bean，本默认实现整体退避。
 */
@AutoConfiguration
@EnableConfigurationProperties(BaseonAuthProperties.class)
public class BaseonAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(BaseonAuthProperties properties) {
        return new JwtTokenProvider(properties);
    }

    /**
     * 条件按 {@link AuthProvider} 判断：业务自定义 AuthProvider 时默认 TokenService
     * 一并退避，避免注入点出现两个 AuthProvider 候选。
     */
    @Bean
    @ConditionalOnMissingBean({AuthProvider.class, TokenService.class})
    public TokenService tokenService(JwtTokenProvider tokenProvider, RedisUtil redisUtil,
                                     BaseonAuthProperties properties) {
        return new TokenService(tokenProvider, redisUtil, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionChecker permissionChecker() {
        return new DefaultPermissionChecker();
    }

    @Bean
    public RequiresPermissionAspect requiresPermissionAspect(PermissionChecker permissionChecker) {
        return new RequiresPermissionAspect(permissionChecker);
    }

    /**
     * 用 FilterRegistrationBean 注册而不把 AuthTokenFilter 直接暴露为 Bean，
     * 避免 Boot 对 Filter 类型 Bean 的自动注册造成重复挂载。
     */
    @Bean
    @ConditionalOnProperty(prefix = "baseon.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<AuthTokenFilter> authTokenFilterRegistration(
            TokenService tokenService, BaseonAuthProperties properties, ObjectMapper objectMapper) {
        FilterRegistrationBean<AuthTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthTokenFilter(tokenService, properties, objectMapper));
        registration.addUrlPatterns("/*");
        // 先于 starter-web 的用户上下文拦截器完成认证，同时避开 tracing 等更前置的过滤器
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
