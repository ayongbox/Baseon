# baseon-starter-tenant · 多租户 SQL 过滤 + 缓存租户前缀

## 定位

多租户能力底座。基于 starter-mybatis-plus 的插件链自动挂载租户 SQL 过滤，同时为缓存 key 提供租户隔离前缀；`@IgnoreTenant` / `@IgnoreTenantCache` 两个注解覆盖跨租户例外场景。

引入方式（版本由 BOM 管控，依赖 baseon-starter-mybatis-plus）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-tenant</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `BaseonTenantLineHandler` | MP `TenantLineHandler` 实现：租户值读 `SecurityUtil.getTenantId()`，列名与白名单走配置 | 默认装配；注册后 starter-mybatis-plus 的 `MybatisPlusInterceptor` 自动挂载租户插件（位置固定在分页插件之前） |
| `TenantCacheKeyPrefixProvider` | `CacheKeyPrefixProvider` SPI 实现：缓存 key 前缀 `t<tenantId>:` | 默认装配，无租户上下文或 `@IgnoreTenantCache` 时返回空前缀 |
| `TenantSqlIgnoreAspect` | `@IgnoreTenant` 切面：方法执行期间关闭 MP 租户插件 | 默认装配 |
| `TenantCacheIgnoreAspect` | `@IgnoreTenantCache` 切面：方法执行期间缓存 key 不加租户前缀 | 默认装配 |

注册方式：`AutoConfiguration.imports`，`baseon.tenant.enabled=false` 时整体不装配。租户 SQL 过滤与缓存前缀两级能力解耦：未引入本 starter 时租户插件不装配、缓存无前缀。

## 配置项

```yaml
baseon:
  tenant:
    enabled: true                # 总开关：false 时租户 SQL 过滤与缓存前缀均不装配
    column: tenant_id            # 租户列名，全库表统一
    ignore-tables:               # 不参与租户过滤的表（如字典表、租户表自身）
      - sys_dict
      - sys_tenant
    strict: false                # 严格模式：true 时缺少租户上下文直接抛异常
```

## 使用场景

### 场景 1：默认租户过滤

租户值由 starter-web 的拦截器从请求头 `X-Tenant-Id` 写入 `SecurityUtil`，`BaseonTenantLineHandler` 在 SQL 改写时读取。业务代码零侵入：

```java
// 当前请求租户为 1001 时，实际执行：SELECT ... FROM order WHERE tenant_id = 1001
orderMapper.selectList(null);
```

无租户上下文（登录前接口、系统任务等）时的行为由 `strict` 决定：

- `false`（默认）：拼入不可能命中的租户值 `-1`，查不到数据但接口不报错——宁可查不到也不跨租户泄露的安全兜底；
- `true`：直接抛 `IllegalStateException`，提示传入租户标识或加 `@IgnoreTenant`。

### 场景 2：写入侧租户自动填充

实体继承 `TenantEntity`（或声明 `tenantId` 属性）即可。starter-mybatis-plus 的 `MyMetaObjectHandler` 按「字段存在且当前值为 null 才填充」策略写入当前租户值，**不依赖 `@TableField(fill=...)` 注解**（内核实体刻意不携带 MP 注解），业务手工赋值不被覆盖。

### 场景 3：@IgnoreTenant 跨租户查询

```java
@IgnoreTenant
public List<OrderStat> statAllTenant() {
    return orderMapper.statAll();   // 不拼接 tenant_id 条件，跨租户统计
}
```

方法/类级均可。双保险：MP 官方 `InterceptorIgnoreHelper` 让 TenantLineInnerInterceptor 跳过条件拼接；`TenantIgnoreContext` 标记供填充器识别，避免 @IgnoreTenant 的写入操作仍被自动填充 tenantId。

### 场景 4：@IgnoreTenantCache 全租户共享缓存

```java
@IgnoreTenantCache
public DictDTO getDict(String code) {
    return cacheHelper.getOrLoad("dict:" + code, DictDTO.class, 3600,
            () -> dictMapper.selectByCode(code));   // key 不加 t<tenantId>: 前缀，全租户共享
}
```

适用于字典、配置等全租户共享的缓存场景，避免同一份数据按租户重复缓存。

### 场景 5：编程式忽略（非 Web 场景）

注解只能标在方法上，且依赖 AOP 代理生效。定时任务、消息消费、跨租户批处理等场景里，用 `TenantOps` 的代码块级 API 更顺手：

```java
// 代码块内关闭租户 SQL 过滤与 tenantId 自动填充，等价于 @IgnoreTenant
long total = TenantOps.callWithSqlIgnored(() -> orderMapper.selectCount(null));

// 无返回值形态
TenantOps.runWithSqlIgnored(() -> syncService.syncAllTenants());

// 代码块内缓存 key 不加租户前缀，等价于 @IgnoreTenantCache
DictDTO dict = TenantOps.callWithCacheIgnored(() -> cacheHelper.getOrLoad("dict:" + code, DictDTO.class, 3600, loader));
```

`callWithSqlIgnored` 与注解切面是同一套双保险（`InterceptorIgnoreHelper` + `TenantIgnoreContext` 标记）。**嵌套安全**：外层方法已标 `@IgnoreTenant` 时，内层 `TenantOps` 退出不会误清外层状态。方法名用 `call` / `run` 前缀区分有无返回值——避免无参 lambda 在 `Supplier` / `Runnable` 间的类型推导歧义（见 pitfalls P-005）。

### 场景 6：复杂 SQL 与手动租户条件

租户插件基于 JSqlParser 的 AST 改写，大多数复杂 SQL 能正确处理：

| 场景 | 行为 |
|---|---|
| 多表 JOIN | 每张表注入租户条件；**LEFT JOIN 的表条件追加到 ON 子句而非 WHERE**（避免 LEFT JOIN 退化成 INNER JOIN） |
| 子查询 / UNION | 递归处理每个 SELECT 分支 |
| INSERT ... SELECT | 目标表与源表都处理 |
| 手写 XML SQL | 与注解 SQL 一样被拦截改写 |

需要人工干预的边界：

1. **JOIN 到非租户表**（字典表等）：被误拼 `tenant_id` 会查不到数据，必须把表加进 `ignore-tables`。
2. **JSqlParser 解析失败的方言 SQL**（`FORCE INDEX`、`WITH RECURSIVE`、存储过程等）：解析失败直接抛异常，没有优雅降级。
3. **索引必须配合**：所有租户表的联合索引以 `tenant_id` 前导，否则自动注入后执行计划可能退化。

方言 SQL 或特殊租户逻辑（如 `tenant_id IN (...)`）只能手动写条件，正确姿势三步：

```java
// 1. 关闭自动插件，避免与手动条件重复拼接
@IgnoreTenant
List<Order> selectComplex(@Param("tenantId") Long tenantId);
```

```xml
<!-- 2. XML 里手动写条件 -->
<select id="selectComplex" resultType="...">
    SELECT ... FROM t_order o FORCE INDEX(idx_tenant_status)
    WHERE o.tenant_id = #{tenantId} AND ...
</select>
```

```java
// 3. 值从上下文取，绝不允许调用方传入——前端可控的 tenantId 参数等于越权大门
return mapper.selectComplex(SecurityUtil.getTenantId());
```

手动只能是例外通道：自动注入是**默认安全**（漏配白名单会报错暴露），手动是**默认不安全**（漏写条件即越权）。手动 SQL 必须走 Code Review 逐条盯防。

## 注意事项

1. **租户列全库统一**：`baseon.tenant.column` 一个值管全库，各表租户列名必须一致；不参与过滤的表显式列入 `ignore-tables`。
2. **租户过滤只作用于 MP 插件链**：手写的 JDBC / 其他持久层框架不受控，需自行保证租户条件。
3. **@IgnoreTenant 是双刃剑**：关闭 SQL 过滤的同时也关闭 tenantId 自动填充，跨租户写入的实体需自行设置 tenantId。
4. **strict=true 会暴露缺上下文的调用**：上线前建议先在测试环境开 strict 跑一轮，把漏传租户的入口接口找齐，再决定生产档位。
