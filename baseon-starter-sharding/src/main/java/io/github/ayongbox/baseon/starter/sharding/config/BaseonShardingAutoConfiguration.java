package io.github.ayongbox.baseon.starter.sharding.config;

import com.zaxxer.hikari.HikariDataSource;
import io.github.ayongbox.baseon.starter.datasource.config.BaseonDatasourceAutoConfiguration;
import io.github.ayongbox.baseon.starter.datasource.config.BaseonDatasourceProperties.DataSourceConfig;
import io.github.ayongbox.baseon.starter.datasource.config.BaseonDatasourceProperties.HikariConfig;
import io.github.ayongbox.baseon.starter.sharding.support.ShardingRoutingDataSourceContributor;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分库分表自动装配：把 ShardingSphere-JDBC 聚合成一个逻辑数据源，
 * 经 {@link io.github.ayongbox.baseon.starter.datasource.support.RoutingDataSourceContributor}
 * 挂进 starter-datasource 的动态路由表，业务以 {@code @DS("sharding")} 命中。
 *
 * <p>装配条件：baseon.sharding.enabled 未显式关闭，且配置了 rules-location。
 * 物理分片库在 baseon.sharding.datasources 声明，分片规则由业务在 YAML 中自定；
 * MyBatis-Plus 插件链（租户/数据权限/分页）在 MyBatis 层先于 ShardingSphere（JDBC 层）执行，天然兼容。
 */
@AutoConfiguration(after = BaseonDatasourceAutoConfiguration.class)
@ConditionalOnClass(YamlShardingSphereDataSourceFactory.class)
@EnableConfigurationProperties(BaseonShardingProperties.class)
@ConditionalOnProperty(prefix = "baseon.sharding", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BaseonShardingAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "baseon.sharding.rules-location")
    public ShardingRoutingDataSourceContributor shardingDataSourceContributor(
            BaseonShardingProperties properties, ResourceLoader resourceLoader) throws IOException, SQLException {
        if (properties.getDatasources().isEmpty()) {
            throw new IllegalStateException(
                    "baseon.sharding.datasources 至少声明一个物理分片库，且名称需与规则 YAML 的 actualDataNodes 对齐");
        }

        Map<String, DataSource> physicalDataSources = new LinkedHashMap<>();
        List<HikariDataSource> pools = new ArrayList<>(properties.getDatasources().size());
        properties.getDatasources().forEach((name, config) -> {
            HikariDataSource pool = buildDataSource(config, properties.getHikari());
            pools.add(pool);
            physicalDataSources.put(name, pool);
        });

        Resource rules = resourceLoader.getResource(properties.getRulesLocation());
        if (!rules.exists()) {
            throw new IllegalStateException("分片规则文件不存在: " + properties.getRulesLocation());
        }
        byte[] yaml = rules.getInputStream().readAllBytes();

        DataSource shardingDataSource =
                YamlShardingSphereDataSourceFactory.createDataSource(physicalDataSources, yaml);
        return new ShardingRoutingDataSourceContributor(properties.getRouteKey(), shardingDataSource, pools);
    }

    /**
     * 与 starter-datasource 同款的 Hikari 构建：连接配置 + 共用池参数。
     */
    private HikariDataSource buildDataSource(DataSourceConfig config, HikariConfig hikari) {
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
