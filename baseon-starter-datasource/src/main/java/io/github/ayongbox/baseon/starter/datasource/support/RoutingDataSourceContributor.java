package io.github.ayongbox.baseon.starter.datasource.support;

import java.util.Map;

/**
 * 动态路由数据源的扩展点：在主库/从库装配完成后，向路由表追加额外的数据源。
 *
 * <p>典型场景：starter-sharding 把 ShardingSphere 聚合数据源注册进路由表，
 * 业务通过 {@code @DS("sharding")} 命中分片数据源。实现类注册为 Spring Bean 即可生效，
 * 多个实现按 Bean 声明顺序依次执行。
 *
 * <p>注意：本接口只负责往 {@code targets} 放数据源，连接池的生命周期由实现方自行管理。
 */
@FunctionalInterface
public interface RoutingDataSourceContributor {

    /**
     * 向路由表追加数据源。
     *
     * @param targets 动态路由数据源的目标表，已含 master/slaveN，key 不允许覆盖已有项
     */
    void contribute(Map<Object, Object> targets);
}
