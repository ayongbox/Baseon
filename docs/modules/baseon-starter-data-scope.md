# baseon-starter-data-scope · 行级/字段级数据权限

## 定位

ToB、多租户、需要行级数据隔离的业务必选。框架只提供**机制**（注解 + SQL 拦截器 + 序列化过滤），
**策略**（谁能看哪些部门/项目/客户）由业务侧实现 `DataScopeProvider` 注入。

## 能力清单

| 能力 | 触发方式 | 实现层 |
|---|---|---|
| 行级过滤 | `@DataScope` 打在 Service/Mapper 方法或类上 | JSqlParser AST 改写 SQL，自动拼 `dept_id IN (...)` |
| 字段级过滤 | `baseon.data-scope.field-filter-enabled=true` + `DataScopeProvider#visibleFields()` | Jackson 序列化阶段动态裁字段（不改 SQL） |
| 豁免 | `@IgnoreDataScope` | 切面标记 + 拦截器读取跳过 |
| 白名单 | `baseon.data-scope.ignore-tables` | 配置类，拦截器按表名跳过 |
| 全局开关 | `baseon.data-scope.enabled=false` | 整个 starter 不装配 |

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-data-scope</artifactId>
</dependency>
```

### 2. 业务侧实现 DataScopeProvider

```java
@Component
public class MyDataScopeProvider implements DataScopeProvider {
    @Override
    public Set<Long> visibleIds() {
        // 从权限中心/组织中心查询当前用户可见部门
        LoginUser user = SecurityUtil.getLoginUser();
        return permissionClient.getVisibleDeptIds(user.getUserId());
    }

    @Override
    public String column() {
        return "dept_id"; // 与表结构对应
    }

    @Override
    public Set<String> visibleFields() {
        // 字段级过滤：当前用户可见的字段名集合；返回 null 表示不过滤
        return Set.of("id", "name", "createTime");
    }
}
```

### 3. 在需要过滤的方法上打注解

```java
@Service
public class UserService {

    @DataScope  // 自动注入 dept_id IN (visibleIds)
    public List<User> listScoped() {
        return mapper.selectList(null);
    }

    @IgnoreDataScope  // 跳过行级过滤（如管理员统计）
    public long countAll() {
        return mapper.selectCount(null);
    }
}
```

## 配置项

```yaml
baseon:
  data-scope:
    enabled: true                # 总开关，默认 true
    column: dept_id              # 默认过滤列名，@DataScope#column 可覆盖
    ignore-tables:               # 白名单表，不参与过滤
      - sys_dict
      - sys_config
    strict: false                # true 时未提供 DataScopeProvider 实现则启动失败
    field-filter-enabled: false  # 字段级过滤开关，默认关闭
```

## 行为语义

| 场景 | 行为 |
|---|---|
| `@DataScope` + visibleIds = [10, 20] | SQL 追加 `dept_id IN (10, 20)` |
| `@DataScope` + visibleIds = [] | SQL 追加 `dept_id = -1`（不可能命中，查不到数据） |
| `@DataScope` + visibleIds = null | 不注入条件（放行，全量可见） |
| 无 `@DataScope` | 拦截器不生效，零开销 |
| `@IgnoreDataScope` | 跳过注入，全量可见 |
| 表在 `ignore-tables` | 跳过该表的条件注入 |

## 与租户插件的协同

拦截器顺序固定：**租户 → 数据权限 → 分页**。

- 租户条件先注入（`tenant_id = ?`），数据权限后注入（`dept_id IN (...)`）
- 租户列通常是联合索引前导列，先命中租户索引再过滤数据范围，避免执行计划退化
- 同一 AST 链路，一次解析同时处理两类条件，不重复解析 SQL

## 字段级过滤实现细节

- 不改 SQL，不影响索引和 ORM 映射
- 通过 `BeanSerializerModifier` 包装默认 `BeanSerializer`，每次序列化时按当前 `DataScopeContext` 动态求值
- Jackson 会缓存序列化器，但裁剪逻辑在每次 `serialize()` 调用时执行，不缓存字段集合
- 业务侧无需在 POJO 上标注 `@JsonFilter`，透明生效

## 注意事项

1. **DataScopeProvider 必须业务侧实现**：框架不内置 RBAC/部门树，规则永远留给业务
2. **strict 模式建议生产开启**：`baseon.data-scope.strict=true` 时未提供实现则启动失败，避免静默放行
3. **空可见集 ≠ 放行**：`visibleIds` 返回空集时拼入 `-1`，查询结果为空；返回 `null` 才是不限制
4. **嵌套 @DataScope**：内层覆盖外层，退出时恢复外层现场（切面用线程栈管理）
5. **字段级过滤仅对 JSON 序列化生效**：不影响 SQL 查询、不影响 Excel 导出、不影响日志输出
6. **缓存穿透**：行级过滤后的结果如果走缓存，缓存 key 需包含 scope 摘要（待 starter-cache 联动，当前版本未实现）
