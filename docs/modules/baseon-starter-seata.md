# baseon-starter-seata · 分布式事务（可选，AT/TCC）

## 定位

Apache Seata 的可选封装，**默认不引入**。跨库强一致事务成为必需时再加依赖，避免全局数据源代理与 TC 通信成本。本 starter 不重复实现 Seata 能力，只做三件事：

1. **统一配置入口**：`baseon.seata.*` 映射为 Seata 原生 `seata.*`，业务不用记两套前缀；
2. **模式推导**：AT 自动开数据源代理、TCC 自动关，避免共用一套代理开关导致隐性错配；
3. **编程式入口**：`SeataGlobalTransactionExecutor` 覆盖注解代理不生效的场景（跨线程、非 Spring Bean）。

注解式 `@GlobalTransactional` 由 Seata 官方 `GlobalTransactionScanner` 提供，开箱可用。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-seata</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `BaseonSeataEnvironmentPostProcessor` | `baseon.seata.*` → `seata.*` 映射；关闭时同步禁 Seata | `spring.factories` 注册，配置加载后执行 |
| `TransactionalTemplate` | Seata 全局事务模板 | `@ConditionalOnMissingBean`，业务可替换 |
| `SeataGlobalTransactionExecutor` | 编程式全局事务执行器 | 同上，`@ConditionalOnMissingBean` |
| Seata 官方 `GlobalTransactionScanner` / 数据源代理 | `@GlobalTransactional`、RM 注册 | 由 `seata-spring-boot-starter` 自带 |

装配条件：`baseon.seata.enabled`（默认 true）与 `seata.enabled` 均为 true，且 classpath 存在 `TransactionalTemplate`。

## 配置项

```yaml
baseon:
  seata:
    enabled: true                 # 总开关；false 会同步把 seata.enabled 置 false
    mode: at                      # at：自动代理数据源；tcc：不代理，业务实现两阶段接口
    tx-service-group: baseon-tx-group  # 映射 seata.tx-service-group，须与 TC 集群一致
    application-id: order-service      # 映射 seata.application-id，为空沿用 Seata 默认
```

映射规则：

- 只映射**显式配置**的 baseon 键，未配置的 `seata.*` 保持业务自身取值；
- 映射结果注入最高优先级属性源，同名 `seata.*` 会被 baseon 键覆盖；
- `mode=at` → `seata.enable-auto-data-source-proxy=true` + `seata.data-source-proxy-mode=AT`；
- `mode=tcc` → `seata.enable-auto-data-source-proxy=false`；
- 非法开关/模式值（如 `enabled: yes`、`mode: xa`）启动即报。

TC 侧连接、注册中心、分组映射等仍使用 Seata 原生配置（`seata.registry.*`、`seata.service.vgroup-mapping.*`），本 starter 不改写。

## 用法

### 注解式（首选）

```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public void createOrder(OrderDTO dto) {
    orderMapper.insert(order);
    stockFeign.deduct(dto.getSkuId(), dto.getCount());
}
```

约束：必须是 Spring Bean 的**外部调用**入口；同类内部调用、`private` 方法、跨线程调用均不生效。

### 编程式（注解不生效的场景）

```java
@RequiredArgsConstructor
public class OrderService {

    private final SeataGlobalTransactionExecutor txExecutor;

    public String submit(OrderDTO dto) {
        // 事务失败自动回滚；BizException 原样抛出，其余异常包装为 SYSTEM_ERROR
        return txExecutor.execute("submit-order", () -> {
            orderMapper.insert(order);
            return order.getId();
        });
    }
}
```

跨线程示例：线程池任务内无法继承 `@GlobalTransactional` 的代理上下文，用执行器显式开启全局事务（注意线程池需配合 `ContextAwareExecutor` 透传业务上下文，XID 由 Seata 自身管理）。

## 注意事项

1. **默认不引入**：把依赖加进不需要分布式事务的服务，会带来数据源代理与 TC 心跳成本；`baseon.seata.enabled=false` 可整体关闭。
2. **AT 自动代理 + 多数据源是陷阱**：starter-datasource 的动态路由数据源会被整体代理为一个分支资源，多物理库分支共享同一 resourceId，回滚可能落到错误的库；需按物理库注册 `DataSourceProxy`，或用 `seata.excludes-for-auto-proxying` 排除后手工代理。与 starter-sharding 叠加时同理（跨分片事务属 XA/Seata 专项能力，不在 sharding starter 范围）。
3. **TCC 的幂等/悬挂/空回滚由业务兜底**：本 starter 只负责开关与装配，不提供事务状态表。
4. **TC 必须自行部署**：seata-server、存储模式（file/db/redis）与注册中心不在脚手架范围；`tx-service-group` 与 TC 集群映射错配会导致启动期连不上 TC。
5. **`DataSourceProxy` 与连接池顺序**：AT 只代理 `DataSource` Bean，业务不要自行 new 连接池绕过 Spring 容器，否则该连接池不被纳入分支事务。
6. **Seata 日志与本地 logback 同栈**：两者都用 logback，多实例部署时注意 TC 客户端日志量（`seata.log.*` 可调级别）。
