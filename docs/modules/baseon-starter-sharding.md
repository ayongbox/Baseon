# baseon-starter-sharding · 分库分表（ShardingSphere-JDBC 基础装配）

## 定位

把 ShardingSphere-JDBC 聚合数据源挂进 starter-datasource 的动态路由表，作为与 master/slaveN 平级的一个路由目标。本 starter **只装配引擎、不提供任何默认分片规则**——表/库策略、广播表、绑定表、分布式主键全部由业务在规则 YAML 中自定。

设计要点：

1. **复用动态路由而非另起数据源**：分片库与主从库共用同一条 `@DS` 路由决策链，业务切换零新 API；
2. **物理数据源声明与规则分离**：连接信息走 `baseon.sharding.datasources`（可加密、可 Nacos），规则 YAML 只含 rules+props；
3. **与 MyBatis-Plus 天然兼容**：MP 插件链（租户→数据权限→分页）在 MyBatis 层先于 ShardingSphere（JDBC 层）执行，租户列可直接作分片键。

引入方式（版本由 BOM 管控，ShardingSphere 版本由父 POM 锁定）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-sharding</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `ShardingRoutingDataSourceContributor` | 构建物理库 Hikari 池 + 读取规则 YAML 聚合 ShardingSphere 数据源，挂入动态路由表 | 配置了 `rules-location` 才装配；实现 `DisposableBean` 负责关闭顺序 |

注册方式：`AutoConfiguration.imports`。`baseon.sharding.enabled=false` 或未配置 `rules-location` 时整体不装配（路由表中不存在分片数据源）。

装配前提：starter-datasource 已生效（`baseon.datasource.master.url` 已配置）——分片数据源是路由表的一个目标，没有路由表就没有挂载点。

## 配置项

```yaml
baseon:
  sharding:
    enabled: true                          # 总开关
    rules-location: classpath:sharding-rules.yaml  # 唯一必填，支持 classpath:/file:；为空则不装配
    route-key: sharding                    # 路由表 key，@DS("sharding") 命中；不得与 master/slaveN 重名
    datasources:                           # 物理分片库，key 即规则 YAML 中 actualDataNodes 引用的 ds 名
      ds0:
        url: jdbc:mysql://192.168.1.10:3306/order_0
        username: order
        password: ${ORDER_DS0_PWD}
        driver-class-name: com.mysql.cj.jdbc.Driver   # 默认值，可省
      ds1:
        url: jdbc:mysql://192.168.1.11:3306/order_1
        username: order
        password: ${ORDER_DS1_PWD}
    hikari:                                # 全部分片库共用池参数，与 starter-datasource 同模型同默认值
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000
      max-lifetime: 1800000
      idle-timeout: 600000
      connection-test-query: SELECT 1
```

## 规则文件（业务自定）

`rules-location` 指向 ShardingSphere 原生 YAML，只写 `rules` 与 `props`（物理数据源已由上面配置声明，**不要**写 `dataSources` 段）。示例——t_order 按 user_id 取模分库：

```yaml
databaseName: sharding_logic

rules:
  - !SHARDING
    tables:
      t_order:
        actualDataNodes: ds$->{0..1}.t_order
        databaseStrategy:
          standard:
            shardingColumn: user_id
            shardingAlgorithmName: db_inline
        keyGenerateStrategy:
          column: id
          keyGeneratorName: snowflake
    shardingAlgorithms:
      db_inline:
        type: INLINE
        props:
          algorithm-expression: ds$->{user_id % 2}
    keyGenerators:
      snowflake:
        type: SNOWFLAKE

props:
  sql-show: false
```

更多语法（分表、范围分片、广播表、绑定表、读写分离叠加）见 ShardingSphere 5.5.x 官方文档。

## 业务用法

分片表的 Mapper / Service 标注 `@DS("sharding")`（与 route-key 一致），路由决策与读写分离同链：

```java
@Mapper
@DS("sharding")
public interface OrderMapper extends BaseMapper<Order> {
    // 全部 SQL 经 ShardingSphere 路由；携带分片键走单库，否则全库广播归并
}
```

未标注的表仍走 master/读写分离，互不干扰。事务语义不变（`@Transactional` 内路由 key 固定，分片内单库操作走本地事务）。

## 注意事项

1. **分片表必须标 `@DS("sharding")`**：未标注的分片表 SQL 会落到 master 报「表不存在」；反之非分片表标了 `@DS("sharding")` 也会因规则引擎无元数据而失败。
2. **物理数据源名必须与 YAML 对齐**：`datasources` 的 key 与 `actualDataNodes` 的 ds 名一一对应，错配启动失败。
3. **route-key 冲突快速失败**：不允许覆盖 master/slaveN，启动即报。
4. **分布式主键二选一**：规则 YAML 的 `keyGenerateStrategy`（SNOWFLAKE）与 MP 的 `@TableId(type = ASSIGN_ID)` 都能生成，取其一，不要叠加。
5. **跨分片事务不在本 starter 范围**：跨库写需业务保证最终一致性；XA/Seata 集成按需后续迭代（S-23）。
6. **规则热更不内置**：YAML 改动需重启生效；动态治理（DistSQL/注册中心模式）不在本 starter 的 JDBC 单机形态内。
