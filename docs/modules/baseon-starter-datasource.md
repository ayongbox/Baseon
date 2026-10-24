# baseon-starter-datasource · 动态数据源 + 读写分离

## 定位

数据访问层底座。一个依赖获得「主库 + 多从库」的动态路由能力：每库一个 Hikari 池（统一池参数），`@DS` 注解显式切换，`@Transactional(readOnly = true)` 只读事务自动轮询路由到从库。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-datasource</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `BaseonDatasourceAutoConfiguration` | 构建主库 + 各从库 Hikari 池，装配 `@Primary` 的 `DynamicRoutingDataSource` | 配置了 `baseon.datasource.master.url` 才生效，先于 Boot 默认 DataSource 装配 |
| `DataSourceSwitchAspect` | `@DS` 注解切面，进入方法前写入数据源上下文，finally 清理 | 默认装配 |
| `ReadOnlyRouteAspect` | 只读事务前置探测切面，`@Order(HIGHEST_PRECEDENCE)` 先于事务拦截器执行 | 默认装配 |
| `DynamicRoutingDataSource` | 路由决策：@DS 显式值 > 只读轮询从库 > 主库兜底 | 随数据源装配 |

注册方式：`AutoConfiguration.imports`。装配条件：`baseon.datasource.enabled` 未显式关闭（默认 true）且配置了 `baseon.datasource.master.url`；关闭后数据源回退给 Spring Boot 默认装配。

## 配置项

```yaml
baseon:
  datasource:
    enabled: true                    # 总开关，false 时本 starter 不装配任何 Bean
    master:
      url: jdbc:mysql://localhost:3306/baseon?useSSL=false&serverTimezone=Asia/Shanghai
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver   # 默认值，可省略
    slaves:                          # 从库列表，按声明顺序命名 slave1、slave2……
      - url: jdbc:mysql://localhost:3307/baseon?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: root
    read-write-split-enabled: true   # 只读事务自动路由从库开关
    hikari:                          # 全部数据源共用的连接池参数，不支持单池覆盖
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000       # 毫秒，快速失败避免拖垮业务线程
      max-lifetime: 1800000          # 毫秒，须小于数据库 wait_timeout
      idle-timeout: 600000           # 毫秒
      connection-test-query: SELECT 1
```

## 使用场景

### 场景 1：@DS 显式切换数据源

```java
@Service
public class ReportService {

    @DS("slave1")
    public List<Order> queryReport() {
        return orderMapper.selectList(null);
    }
}
```

标注在方法上优先于标注在类上。注意 **必须在事务边界外使用**：事务开启时连接已绑定到当前线程，事务内切换数据源不会重新获取连接，注解将静默失效。正确用法是标注在非事务的入口方法（或外层 Facade）上，再调用内部的 `@Transactional` 方法。

### 场景 2：只读事务自动走从库

```java
@Transactional(readOnly = true)
public List<Order> listOrders() {
    return orderMapper.selectList(null);   // 自动轮询路由到 slave1/slave2……
}
```

`ReadOnlyRouteAspect` 在事务开启**之前**识别 `@Transactional(readOnly = true)` 并写入只读标记，路由数据源据此轮询选从库。之所以不能靠 `AbstractRoutingDataSource` 自己读事务标记：`DataSourceTransactionManager` 在 `doBegin()` 获取连接时 readOnly 尚未注册到 `TransactionSynchronizationManager`（详见 pitfalls P-011）。

### 场景 3：关闭读写分离

```yaml
baseon:
  datasource:
    read-write-split-enabled: false
```

关闭后只读事务也回主库，`@DS` 显式切换不受影响。

## 注意事项

1. **嵌套事务不切换数据源**：`ReadOnlyRouteAspect` 检测到当前线程已有事务同步（外层事务已绑定连接）时直接放弃路由，切换无效且危险。
2. **@DS 优先于读写分离**：上下文中已有 @DS 显式值时，只读探测主动让位。
3. **无事务的普通查询走 master**：路由决策只认 @DS 显式值与只读标记，两者都没有时兜底主库。
4. **没有配置从库时仍回主库**：读写分离开启但 slaves 为空，路由退化为全部走 master。
5. **池参数全库共用**：`baseon.datasource.hikari.*` 一套参数应用到主库与所有从库，不支持单池覆盖。
