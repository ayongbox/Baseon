# baseon-core · 基础内核

## 定位

所有 Starter 与业务服务的公共底座。只放**与框架无关**的模型、工具、上下文契约。

**硬约束（ArchUnit 强制）**

| 规则 | 说明 |
|---|---|
| 零 Spring 依赖 | `model` / `tool` / `context` 不得出现 `org.springframework.*` |
| 零反向依赖 | `core` 不得依赖 `baseon.starter.*`，依赖方向单向 |
| model 最底层 | `model` 不得依赖 `tool` / `context` |

编译期与运行期**零第三方依赖**（测试依赖除外），因此不引入 Jackson、Lombok、commons-lang3。

## 包结构

| 包 | 职责 | 当前状态 |
|---|---|---|
| `core.model` | 返回体、分页、错误码与错误语义、异常、实体父类、登录用户 | ✅ |
| `core.tool` | 字符串、金额、Bean 拷贝 | ✅ |
| `core.context` | 登录上下文存取与异步透传 | ✅ |
| `core.boot` | 线程池工厂、事务模板、容器持有者 | ⬜ |
| `core.cloud` | 超时/灰度/租户 Header 常量与元数据抽象 | ⬜ |
| `core.db` | 分页查询、持久化 PO 契约（不含 ORM 依赖） | ⬜ |
| `core.secure` | 认证/鉴权/加解密 SPI | ⬜ |
| `core.launch` | 启动前置校验、优雅停机钩子 | ⬜ |
| `core.spi` | 跨模块扩展点契约 | ⬜ |

## 使用场景

### 场景 1：Controller 统一返回

```java
@GetMapping("/{id}")
public R<UserVO> detail(@PathVariable Long id) {
    return R.ok(userService.detail(id));
}

@GetMapping("/page")
public R<PageResult<UserVO>> page(UserQuery query) {
    // UserQuery extends PageQuery，自带 current / size 与上限保护
    return R.ok(userService.page(query));
}
```

判断成功不要写 `code == 0`：

```java
R<UserVO> result = remoteUserService.detail(id);
if (!result.success()) {
    throw new BizException(CommonErrorCode.REMOTE_CALL_FAILED);
}
```

### 场景 2：抛出业务异常

```java
public void transfer(Long from, Long to, BigDecimal amount) {
    if (!MoneyUtil.isPositive(amount)) {
        throw new BizException(CommonErrorCode.PARAM_INVALID, "转账金额必须大于 0");
    }
    // ...
}
```

业务自定义错误码实现 `ErrorCode` 即可，不要扩展 `CommonErrorCode`：

```java
public enum OrderErrorCode implements ErrorCode {
    STOCK_NOT_ENOUGH(20101, "库存不足", ErrorType.CLIENT_ERROR),
    ORDER_CLOSED(20102, "订单已关闭", ErrorType.DUPLICATE);

    private final int code;
    private final String message;
    private final ErrorType type;

    OrderErrorCode(int code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    // getCode / getMessage / getType 省略
}
```

`ErrorType` 决定 `starter-web` 返回的 HTTP 状态码。**不覆盖 `getType()` 时会默认为 `SERVER_ERROR`（500）并触发系统告警**，所以每个业务错误码都必须显式声明语义。

> 模块码由架构组统一分配，禁止业务占用 `01` / `09` 段。

### 场景 3：异步任务上下文透传

```java
// 提交任务时捕获当前登录上下文
CompletableFuture.runAsync(ContextWrapper.wrap(() -> {
    Long tenantId = SecurityUtil.getTenantId();   // 子线程可正常获取
    reportService.generate(tenantId);
}), executor);
```

`ContextWrapper.wrap()` **必须在提交线程调用**，在子线程内部调用无效。

`CompletableFuture.supplyAsync` 需要 `Supplier`，请使用 `ContextWrapper.wrapSupplier(...)`；Supplier 重载没有命名为 `wrap`，因为与 `wrap(Callable)` 存在 lambda 歧义。

### 场景 4：业务实体定义

```java
public class UserPO extends TenantEntity {
    private String username;
    private String mobile;
    // getter / setter
}
```

不需要租户隔离的表继承 `BaseEntity`，不要为了「统一」全部带 `tenantId`。

`deleted`（逻辑删除标记）已在 `BaseEntity` 中定义，业务实体**禁止重复声明**；删除行为由 `starter-mybatis-plus` 统一拦截。

### 场景 5：金额计算

```java
BigDecimal payable = MoneyUtil.scale(unitPrice.multiply(quantity));
if (MoneyUtil.isZero(payable)) { ... }
```

统一 `scale=2`、`HALF_UP`；禁止 `double` / `float` 参与金额运算；金额比较用 `compareTo`，不要用 `equals`。

## 注意事项

1. **内核 POJO 命名**：非属性的判断方法不能用 `isXxx` / `getXxx`，Jackson 会把它当字段输出，而内核又不能加 `@JsonIgnore`。统一用 `success()` 这类非 getter 命名。
2. **`BizException` 不带堆栈**：预期内业务失败按 WARN 打印即可；需要堆栈排障的场景属于系统异常。
3. **`SecurityUtil` 只读**：写入与清理由 `starter-web` 的拦截器负责。业务自行 `set()` 会导致上下文错乱。
4. **线程池必须包装**：手写线程池、`CompletableFuture` 必须显式 `ContextWrapper.wrap()`；框架托管线程池由 Starter 自动包装（2 期实现）。
5. **`BaseEntity` 无 ORM 注解**：字段映射依赖 `map-underscore-to-camel-case` 约定，MyBatis-Plus 相关能力在 `starter-mybatis-plus`。
6. **实例化即用**：`R` 是可变 POJO（提供 setter 供反序列化），不要把它当不可变值对象长期持有。
7. **错误语义必须显式声明**：`ErrorCode#getType()` 默认 `SERVER_ERROR`，业务错误码不覆盖会被当作 500。
8. **分页参数统一继承**：查询对象继承 `PageQuery`，`size` 超过 500 会被静默截断；大导出走独立接口。
9. **Supplier 用 `wrapSupplier`**：`wrap(Runnable)` / `wrap(Callable)` 之外的返回值包装必须用独立方法名，避免 lambda 重载歧义。

## 错误码规约

| 段 | 范围 | 用途 | 典型 HTTP 状态 |
|---|---|---|---|
| `0` | 固定值 | 成功 | 200 |
| `01xxx` | 1001 ~ 1999 | 系统 / 框架级错误（参数、认证、权限、限流、系统繁忙） | 400 / 401 / 403 / 404 / 429 |
| `09xxx` | 9001 ~ 9999 | 远程调用 / 外部依赖错误（失败、超时、降级） | 502 / 503 |
| 业务段 | 10000 起 | 模块码 2 位 + 业务码 3 位，如 `20101` | 由 `ErrorType` 决定 |

已有错误码见 `CommonErrorCode`，新增系统级错误码需在 `CHANGELOG` 中登记。
