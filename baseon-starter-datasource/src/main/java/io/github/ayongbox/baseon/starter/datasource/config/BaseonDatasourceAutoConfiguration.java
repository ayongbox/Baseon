package io.github.ayongbox.baseon.starter.datasource.config;

import com.zaxxer.hikari.HikariDataSource;
import io.github.ayongbox.baseon.starter.datasource.aspect.DataSourceSwitchAspect;
import io.github.ayongbox.baseon.starter.datasource.aspect.ReadOnlyRouteAspect;
import io.github.ayongbox.baseon.starter.datasource.support.DynamicRoutingDataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态数据源自动装配：必须在 Spring Boot 默认的 {@link DataSourceAutoConfiguration}
 * 之前生效并声明 @Primary，否则默认单数据源会抢先注册。
 *
 * <p>装配条件：baseon.datasource.enabled 未显式关闭，且配置了主库 url。
 * 主库与每个从库各建一个 Hikari 池（共用同一套池参数），统一交给
 * {@link DynamicRoutingDataSource} 按线程上下文路由。
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@ConditionalOnClass(HikariDataSource.class)
@EnableConfigurationProperties(BaseonDatasourceProperties.class)
@ConditionalOnProperty(prefix = "baseon.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BaseonDatasourceAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "baseon.datasource.master.url")
    public DataSource dataSource(BaseonDatasourceProperties properties) {
        Map<Object, Object> targets = new HashMap<>();
        HikariDataSource master = buildDataSource(properties.getMaster(), properties.getHikari());
        targets.put(DynamicRoutingDataSource.MASTER, master);

        List<BaseonDatasourceProperties.DataSourceConfig> slaves = properties.getSlaves();
        List<String> slaveNames = new ArrayList<>(slaves.size());
        for (int i = 0; i < slaves.size(); i++) {
            String name = "slave" + (i + 1);
            targets.put(name, buildDataSource(slaves.get(i), properties.getHikari()));
            slaveNames.add(name);
        }

        DynamicRoutingDataSource routing = new DynamicRoutingDataSource(properties, slaveNames);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(master);
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "baseon.datasource.master.url")
    public DataSourceSwitchAspect dataSourceSwitchAspect() {
        return new DataSourceSwitchAspect();
    }

    /**
     * 只读事务前置路由：必须在事务拦截器之前执行（切面内 @Order(HIGHEST_PRECEDENCE)）。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "baseon.datasource.master.url")
    public ReadOnlyRouteAspect readOnlyRouteAspect() {
        return new ReadOnlyRouteAspect();
    }

    /**
     * 按连接配置 + 共用池参数构建一个 Hikari 数据源。
     */
    private HikariDataSource buildDataSource(BaseonDatasourceProperties.DataSourceConfig config,
                                             BaseonDatasourceProperties.HikariConfig hikari) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(config.getUrl());
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        dataSource.setDriverClassName(config.getDriverClassName());
        dataSource.setMaximumPoolSize(hikari.getMaximumPoolSize());
        dataSource.setMinimumIdle(hikari.getMinimumIdle());
        dataSource.setConnectionTimeout(hikari.getConnectionTimeout());
        dataSource.setMaxLifetime(hikari.getMaxLifetime());
        dataSource.setIdleTimeout(hikari.getIdleTimeout());
        dataSource.setConnectionTestQuery(hikari.getConnectionTestQuery());
        return dataSource;
    }
}
