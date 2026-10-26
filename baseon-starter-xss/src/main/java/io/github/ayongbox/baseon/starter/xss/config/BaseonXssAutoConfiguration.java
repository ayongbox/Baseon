package io.github.ayongbox.baseon.starter.xss.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.ayongbox.baseon.starter.xss.filter.XssFilter;
import io.github.ayongbox.baseon.starter.xss.jackson.XssStringDeserializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * XSS 防护自动装配入口。
 *
 * <p>两条防线同时装配：
 * <ul>
 *   <li>{@link XssFilter} 以 {@code Ordered.HIGHEST_PRECEDENCE + 30} 注册，
 *       晚于 trace/日志类过滤器，早于业务过滤器，覆盖 query/form 参数与请求头；</li>
 *   <li>{@link XssStringDeserializer} 注册为 Jackson {@link Module} Bean，
 *       由 Boot 自动纳入全局 ObjectMapper，覆盖 JSON 请求体字符串字段。</li>
 * </ul>
 * 所有 Bean 都带 {@link ConditionalOnMissingBean}，业务可整体替换默认实现。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(BaseonXssProperties.class)
public class BaseonXssAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XssFilter xssFilter(BaseonXssProperties properties) {
        return new XssFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "xssFilterRegistration")
    public FilterRegistrationBean<XssFilter> xssFilterRegistration(XssFilter xssFilter) {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(xssFilter);
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 30);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(name = "xssJacksonModule")
    public Module xssJacksonModule() {
        SimpleModule module = new SimpleModule("baseon-xss");
        module.addDeserializer(String.class, new XssStringDeserializer());
        return module;
    }
}
