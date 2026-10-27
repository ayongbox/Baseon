package io.github.ayongbox.baseon.starter.web.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.github.ayongbox.baseon.starter.web.exception.GlobalExceptionHandler;
import io.github.ayongbox.baseon.starter.web.interceptor.UserContextInterceptor;
import io.github.ayongbox.baseon.starter.web.log.RequestLogFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;

/**
 * Web 能力自动装配入口。
 *
 * <p>所有 Bean 都带 {@link ConditionalOnMissingBean}，业务声明同类型 Bean 即覆盖默认实现。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(BaseonWebProperties.class)
public class BaseonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public UserContextInterceptor userContextInterceptor(BaseonWebProperties properties) {
        return new UserContextInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "baseonWebMvcConfigurer")
    public WebMvcConfigurer baseonWebMvcConfigurer(BaseonWebProperties properties,
                                                   UserContextInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                if (properties.getUserContext().isEnabled()) {
                    registry.addInterceptor(interceptor).addPathPatterns("/**");
                }
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                BaseonWebProperties.Cors cors = properties.getCors();
                if (cors.getAllowedOrigins().isEmpty()) {
                    return;
                }
                registry.addMapping("/**")
                        .allowedOrigins(cors.getAllowedOrigins().toArray(new String[0]))
                        .allowedMethods(cors.getAllowedMethods().toArray(new String[0]))
                        .allowedHeaders(cors.getAllowedHeaders().toArray(new String[0]))
                        .exposedHeaders(cors.getExposedHeaders().toArray(new String[0]))
                        .allowCredentials(cors.isAllowCredentials())
                        .maxAge(cors.getMaxAge());
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.web.request-log", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RequestLogFilter requestLogFilter(BaseonWebProperties properties) {
        return new RequestLogFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer baseonJacksonCustomizer(BaseonWebProperties properties) {
        return builder -> {
            BaseonWebProperties.Jackson config = properties.getJackson();
            builder.simpleDateFormat(config.getDateTimeFormat());

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(config.getDateTimeFormat());
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(config.getDateFormat());
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(config.getTimeFormat());

            builder.serializers(new LocalDateTimeSerializer(dateTimeFormatter));
            builder.deserializers(new LocalDateTimeDeserializer(dateTimeFormatter));
            builder.serializers(new LocalDateSerializer(dateFormatter));
            builder.deserializers(new LocalDateDeserializer(dateFormatter));
            builder.serializers(new LocalTimeSerializer(timeFormatter));
            builder.deserializers(new LocalTimeDeserializer(timeFormatter));

            if (config.isLongToString()) {
                // 前端 JS 的 Number 精度只有 53 位，雪花 ID 必须按字符串输出
                builder.serializerByType(Long.class, ToStringSerializer.instance);
            }
        };
    }
}
