package io.github.ayongbox.baseon.starter.sharding.support;

import com.zaxxer.hikari.HikariDataSource;
import io.github.ayongbox.baseon.starter.datasource.support.RoutingDataSourceContributor;
import org.springframework.beans.factory.DisposableBean;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 把 ShardingSphere 聚合数据源挂进动态路由表的 contributor。
 *
 * <p>路由表中的数据源不是容器 Bean，Spring 不会代为销毁，因此本类承担生命周期：
 * 容器关闭时先关 ShardingSphere 聚合数据源（释放规则引擎与元数据上下文），
 * 再逐个关物理库 Hikari 池。
 */
public class ShardingRoutingDataSourceContributor implements RoutingDataSourceContributor, DisposableBean {

    private final String routeKey;
    private final DataSource shardingDataSource;
    private final List<HikariDataSource> physicalPools;

    public ShardingRoutingDataSourceContributor(String routeKey,
                                                DataSource shardingDataSource,
                                                List<HikariDataSource> physicalPools) {
        this.routeKey = routeKey;
        this.shardingDataSource = shardingDataSource;
        this.physicalPools = List.copyOf(physicalPools);
    }

    @Override
    public void contribute(Map<Object, Object> targets) {
        if (targets.containsKey(routeKey)) {
            throw new IllegalStateException(
                    "分片数据源 routeKey [" + routeKey + "] 与动态路由表已有 key 冲突，请调整 baseon.sharding.route-key");
        }
        targets.put(routeKey, shardingDataSource);
    }

    @Override
    public void destroy() throws Exception {
        if (shardingDataSource instanceof AutoCloseable closeable) {
            closeable.close();
        }
        physicalPools.forEach(HikariDataSource::close);
    }
}
