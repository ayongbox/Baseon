package io.github.ayongbox.baseon.starter.mybatisplus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import io.github.ayongbox.baseon.starter.mybatisplus.interceptor.SlowSqlInterceptor;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.function.BiConsumer;

/**
 * MyBatis-Plus 增强能力自动装配。
 *
 * <p>MP 本体装配由 {@code mybatis-plus-spring-boot3-starter} 完成（依赖引入即生效），
 * 这里只装配脚手架增强能力：内置插件顺序固化、公共字段填充、慢 SQL 拦截。
 * 逻辑删除等全局默认值见 {@link GlobalConfigEnvironmentPostProcessor}。
 */
@AutoConfiguration
@ConditionalOnClass(SqlSessionFactoryBean.class)
@EnableConfigurationProperties(BaseonMybatisPlusProperties.class)
public class BaseonMybatisPlusAutoConfiguration {

    /**
     * MP 内置插件总入口，InnerInterceptor 顺序固化：租户 → 分页。
     *
     * <p>租户必须排在分页之前：分页插件会先执行 count 查询，若分页在前，
     * count SQL 尚未经过租户插件改写，统计结果会缺租户条件导致数据泄露。
     *
     * <p>租户插件仅当容器中存在 {@link TenantLineHandler}（由 starter-tenant 提供）
     * 时才加入，单库无租户场景不引入租户改写开销。
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<TenantLineHandler> tenantLineHandlerProvider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        TenantLineHandler tenantLineHandler = tenantLineHandlerProvider.getIfUnique();
        if (tenantLineHandler != null) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        }
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 慢 SQL 拦截器经 {@link ConfigurationCustomizer} 挂到 MyBatis 插件链，
     * 由 MP 自动装配创建 SqlSessionFactory 时应用，保证在最终 Configuration 上生效。
     *
     * <p>开关关闭时本 Bean 不装配，拦截器根本不进入插件链（零开销）；
     * 数据库侧已有完善慢日志时可按需关闭应用侧输出。
     *
     * <p>监听器以 JDK 类型 {@link BiConsumer} 注入，本装配类不引用 Micrometer；
     * 指标接线在下方被 {@code @ConditionalOnClass} 守卫的嵌套配置中完成。
     */
    @Bean
    @ConditionalOnProperty(prefix = "baseon.mybatis-plus", name = "slow-sql-enabled",
            havingValue = "true", matchIfMissing = true)
    public ConfigurationCustomizer slowSqlInterceptorConfigurationCustomizer(
            BaseonMybatisPlusProperties properties,
            ObjectProvider<BiConsumer<String, Long>> slowSqlListenerProvider) {
        SlowSqlInterceptor slowSqlInterceptor = new SlowSqlInterceptor(
                properties.getSlowSqlThresholdMs(),
                properties.getSlowSqlWarmupExecutions(),
                properties.getSlowSqlLogIntervalMs(),
                slowSqlListenerProvider.getIfUnique());
        return configuration -> configuration.addInterceptor(slowSqlInterceptor);
    }

    /**
     * Micrometer 存在时才装配慢 SQL 指标回调，消费方未引入 micrometer 不影响主链路。
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class SlowSqlMetricsConfiguration {

        /** 慢 SQL 计数指标名，tag {@code statement} 为 MappedStatement id */
        static final String SLOW_SQL_METRIC_NAME = "baseon.mybatis.slowsql";

        @Bean
        BiConsumer<String, Long> slowSqlMetricsListener(
                ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistryProvider) {
            io.micrometer.core.instrument.MeterRegistry registry = meterRegistryProvider.getIfAvailable();
            if (registry == null) {
                // 有 micrometer 类但无 Registry（未引入 actuator）：回调退化为 no-op
                return (statementId, elapsedMs) -> {
                };
            }
            return (statementId, elapsedMs) ->
                    registry.counter(SLOW_SQL_METRIC_NAME, "statement", statementId).increment();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public MyMetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }
}
