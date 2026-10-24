# baseon-starter-mybatis-plus · MP 插件链 + 审计填充 + 慢 SQL 告警

## 定位

持久层增强底座。基于 MyBatis-Plus 3.5.9（`mybatis-plus-spring-boot3-starter` + `mybatis-plus-jsqlparser`），固化内置插件顺序、提供逻辑删除全局默认值、公共字段自动填充与慢 SQL 告警。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-mybatis-plus</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `MybatisPlusInterceptor` | MP 内置插件总入口，InnerInterceptor 顺序固化：租户 → 分页 | 默认装配，租户插件条件化（存在 `TenantLineHandler` Bean 才挂载，由 starter-tenant 提供） |
| `SlowSqlInterceptor` | MyBatis 原生插件拦截 Executor 的 update/query，慢 SQL WARN + 指标 | 默认装配（`slow-sql-enabled` 可关），阈值 1000ms，前 3 次预热不判定 |
| `MyMetaObjectHandler` | 公共字段自动填充（审计时间、租户、逻辑删除标记） | 默认装配 |
| `GlobalConfigEnvironmentPostProcessor` | 注入逻辑删除等 MP 全局默认值 | 默认生效，addLast 最低优先级 |

注册方式：`AutoConfiguration.imports`（自动装配）+ `META-INF/spring.factories`（EnvironmentPostProcessor）。MP 本体装配由 `mybatis-plus-spring-boot3-starter` 完成，本 starter 只装配增强能力。

## 配置项

```yaml
baseon:
  mybatis-plus:
    slow-sql-enabled: true          # 慢 SQL 告警总开关；数据库侧慢日志完善时可关，拦截器不进入插件链（零开销）
    slow-sql-threshold-ms: 1000     # 阈值（毫秒），计时口径为端到端 Executor 调用耗时
    slow-sql-warmup-executions: 3   # 每个 statementId 前 N 次执行不判定，跳过建连/类加载/JIT 冷启动成本
    slow-sql-log-interval-ms: 60000 # 同一 statementId 的 WARN 最小间隔；0 表示不限流
```

MP 原生配置（mapperLocations、类型别名等）仍走 `mybatis-plus.*`，本 starter 不重复承载。以下全局默认值由 `GlobalConfigEnvironmentPostProcessor` 以 **addLast 最低优先级**注入，用户显式配置始终优先：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted        # 逻辑删除字段，与 baseon-core 的 BaseEntity 约定对应
      logic-delete-value: 1              # 已删除
      logic-not-delete-value: 0          # 未删除
  configuration:
    map-underscore-to-camel-case: true   # 下划线转驼峰
```

## 使用场景

### 场景 1：分页查询（插件链顺序固化）

```java
Page<Order> page = orderMapper.selectPage(new Page<>(1, 20), null);
```

InnerInterceptor 顺序固化为 **租户 → 分页（`PaginationInnerInterceptor`，DbType.MYSQL）**。租户必须排在分页之前：分页插件会先执行 count 查询，若分页在前，count SQL 尚未经过租户插件改写，统计结果会缺租户条件导致数据泄露。未引入 starter-tenant 时租户插件不装配，单库无租户场景零改写开销。

### 场景 2：慢 SQL 告警

无需编码。单条 SQL 耗时达到 `slow-sql-threshold-ms`（默认 1000ms）即输出 WARN 日志，含 statement id、耗时、阈值、`tenantId`、`traceId` 与参数摘要（实体按字段值渲染，如 `TenantUser{id=1, name=zhangsan}`），总长度截断 200 字符防爆量；Micrometer 可用时逐次上报计数指标 `baseon.mybatis.slowsql`（tag `statement` 为 MappedStatement id）。Micrometer 为可选依赖：未引入时仅打日志，不影响主链路。

**计时口径要先说清楚**：拦截器包住整个 `Executor` 调用，测得的是**端到端 JDBC 耗时**——含连接获取、参数映射、结果集处理与冷启动开销，不等于数据库服务端的 SQL 执行时间。因此：

- **权威判定看数据库侧**：MySQL 慢日志（`long_query_time`）、`performance_schema.events_statements_summary_by_digest`，或云厂商 DAS / DBbrain 的 digest 聚合与索引建议。这些只统计服务端执行时间，零应用开销。
- **应用侧日志的价值是归因**：服务端监控拿不到"哪个 statementId、哪个 traceId、哪个租户、带什么参数"，应用侧日志补的正是这块。多租户场景尤其明显——慢日志看不出 `tenant_id=1001` 与 `tenant_id=3003` 的差异。

两套都开时，应用侧日志容易变成噪音，因此内置两项降噪：

| 机制 | 说明 |
|---|---|
| 预热跳过 | 每个 statementId 的前 `slow-sql-warmup-executions` 次（默认 3）不判定，消除首次建池、驱动/反射类加载、JIT 造成的误报 |
| 日志限流 | 同一 statementId 每 `slow-sql-log-interval-ms`（默认 60s）最多一条 WARN；Micrometer 指标不受限，仍逐次计数 |

数据库侧监控已经完善、不需要应用侧输出时，直接关掉：

```yaml
baseon:
  mybatis-plus:
    slow-sql-enabled: false
```

关闭后 `ConfigurationCustomizer` 不装配，拦截器不进入 MyBatis 插件链，除启动无任何运行开销。

慢 SQL 用 MyBatis 原生 `Interceptor` 而不是 MP 的 `InnerInterceptor`：InnerInterceptor 只有 SQL 解析前后的切面时机，拿不到完整执行耗时；原生插件包住整个 Executor 调用，天然带计时区间。

### 场景 3：公共字段自动填充

实体继承 baseon-core 的 `BaseEntity` / `TenantEntity` 后无需手工赋值：

- `insertFill`：`createTime` / `updateTime` / `deleted=0`，租户上下文存在时填 `tenantId`（读 `SecurityUtil.getTenantId()`，被 `@IgnoreTenant` 标记时不填，避免污染跨租户数据）；
- `updateFill`：`updateTime`。

填充策略为「仅 null 填充」：字段存在且当前值为 null 才填充，业务手工赋值永不覆盖；无对应字段的实体（如不继承 TenantEntity 的表无 tenantId）直接跳过。

## 注意事项

1. **MP 3.5.9 起拦截器拆分到独立构件**：分页/租户等依赖 jsqlparser 的 InnerInterceptor 在 `mybatis-plus-jsqlparser` 中，缺失该依赖时分页静默失效（详见 pitfalls P-010），本 starter 已在 pom 中显式引入。
2. **租户插件是条件化的**：只有容器中存在唯一的 `TenantLineHandler` Bean（由 starter-tenant 提供）时才挂载，不要在本模块手工注册。
3. **默认值是兜底不是覆盖**：application.yml / Nacos 中显式声明的 `mybatis-plus.*` 永远赢过 EnvironmentPostProcessor 注入的默认值。
