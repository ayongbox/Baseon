package io.github.ayongbox.baseon.starter.sharding.config;

import io.github.ayongbox.baseon.starter.datasource.config.BaseonDatasourceProperties.DataSourceConfig;
import io.github.ayongbox.baseon.starter.datasource.config.BaseonDatasourceProperties.HikariConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分库分表配置，前缀 {@code baseon.sharding}。
 *
 * <p>本 starter 只负责把 ShardingSphere-JDBC 装配成动态路由数据源的一个路由目标：
 * 物理分片库由 {@link #datasources} 声明（复用 starter-datasource 的连接/池模型），
 * 分片规则（表/库策略、广播表、绑定表）由 {@link #rulesLocation} 指向的
 * ShardingSphere YAML 提供，业务方完全自定。
 */
@ConfigurationProperties(prefix = "baseon.sharding")
public class BaseonShardingProperties {

    /**
     * 分片总开关。关闭后本 starter 不装配任何 Bean，路由表中不存在分片数据源。
     */
    private boolean enabled = true;

    /**
     * ShardingSphere 规则文件位置（唯一必填），支持 classpath:/file: 前缀。
     * 文件内容只含 rules 与 props（物理数据源由本配置声明，不写在 YAML 里）。
     * 为空则不装配。
     */
    private String rulesLocation;

    /**
     * 分片聚合数据源在动态路由表中的 key，业务以 {@code @DS("sharding")} 命中。
     * 不允许与 master/slaveN 重名。
     */
    private String routeKey = "sharding";

    /**
     * 物理分片库声明，key 即 ShardingSphere YAML 中 actualDataNodes 引用的数据源名
     * （如 ds0/ds1）。至少配置一个，且必须与 YAML 中的引用完全对齐。
     */
    private Map<String, DataSourceConfig> datasources = new LinkedHashMap<>();

    /**
     * 全部分片库共用的 Hikari 连接池参数，与 starter-datasource 同模型同默认值。
     */
    private HikariConfig hikari = new HikariConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRulesLocation() {
        return rulesLocation;
    }

    public void setRulesLocation(String rulesLocation) {
        this.rulesLocation = rulesLocation;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public void setRouteKey(String routeKey) {
        this.routeKey = routeKey;
    }

    public Map<String, DataSourceConfig> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, DataSourceConfig> datasources) {
        this.datasources = datasources;
    }

    public HikariConfig getHikari() {
        return hikari;
    }

    public void setHikari(HikariConfig hikari) {
        this.hikari = hikari;
    }
}
