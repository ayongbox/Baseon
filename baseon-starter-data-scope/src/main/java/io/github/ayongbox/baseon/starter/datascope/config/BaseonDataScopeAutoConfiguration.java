package io.github.ayongbox.baseon.starter.datascope.config;

import io.github.ayongbox.baseon.core.spi.DataScopeProvider;
import io.github.ayongbox.baseon.starter.datascope.aspect.DataScopeAspect;
import io.github.ayongbox.baseon.starter.datascope.aspect.DataScopeIgnoreAspect;
import io.github.ayongbox.baseon.starter.datascope.support.DataScopeLineInnerInterceptor;
import io.github.ayongbox.baseon.starter.mybatisplus.interceptor.DataScopeInnerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 数据权限装配。
 *
 * <p>行级过滤：{@link DataScopeLineInnerInterceptor} 以标记接口 {@link DataScopeInnerInterceptor}
 * 暴露，starter-mybatis-plus 的 {@code MybatisPlusInterceptor} 自动发现并挂载到
 * 「租户之后、分页之前」的固定位置；未引入本 starter 时该插件不装配。
 *
 * <p>策略注入：业务侧必须提供 {@link DataScopeProvider} 实现 Bean 来告知当前用户可见的
 * 数据 ID 集合；缺省时默认放行（等同未引入），strict 模式下启动失败提示接入。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonDataScopeProperties.class)
public class BaseonDataScopeAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BaseonDataScopeAutoConfiguration.class);

    /**
     * 缺省实现：放行。strict=true 时改为启动失败，避免线上静默失防。
     */
    @Bean
    @ConditionalOnMissingBean
    public DataScopeProvider dataScopeProvider(BaseonDataScopeProperties properties) {
        if (properties.isStrict()) {
            throw new IllegalStateException(
                    "baseon.data-scope.strict=true，但未发现 DataScopeProvider 实现 Bean："
                            + "请实现 io.github.ayongbox.baseon.core.spi.DataScopeProvider 并注册为 Spring Bean");
        }
        log.warn("[baseon-data-scope] 未发现 DataScopeProvider 实现，行级数据权限放行；"
                + "若需强制拦截请设置 baseon.data-scope.strict=true");
        return DataScopeProvider.noOp();
    }

    @Bean
    public DataScopeInnerInterceptor dataScopeLineInnerInterceptor(BaseonDataScopeProperties properties) {
        return new DataScopeLineInnerInterceptor(properties);
    }

    @Bean
    public DataScopeAspect dataScopeAspect(DataScopeProvider dataScopeProvider,
                                           BaseonDataScopeProperties properties) {
        return new DataScopeAspect(dataScopeProvider, properties);
    }

    @Bean
    public DataScopeIgnoreAspect dataScopeIgnoreAspect() {
        return new DataScopeIgnoreAspect();
    }

    /**
     * 字段级过滤：仅当 classpath 上存在 Jackson（业务侧引入 web starter）且
     * {@code baseon.data-scope.field-filter-enabled=true} 时装配。
     * 通过注册 Jackson Module 把 BeanSerializerModifier 织入 ObjectMapper，
     * 每次序列化时按当前 DataScopeContext 动态裁字段。
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
            name = "com.fasterxml.jackson.databind.ObjectMapper")
    @ConditionalOnProperty(prefix = "baseon.data-scope", name = "field-filter-enabled",
            havingValue = "true")
    static class DataScopeFieldFilterConfiguration {

        @Bean
        public com.fasterxml.jackson.databind.Module dataScopeFieldFilterModule(
                DataScopeProvider dataScopeProvider) {
            return new com.fasterxml.jackson.databind.module.SimpleModule("baseon-data-scope")
                    .setSerializerModifier(
                            new io.github.ayongbox.baseon.starter.datascope.support
                                    .DataScopeFieldSerializerModifier(dataScopeProvider));
        }
    }
}
