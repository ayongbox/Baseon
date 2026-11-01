# baseon-core-boot · 内核启动扩展

## 定位

承载 `core.boot` 中**必须依赖 Spring** 的能力：容器持有者、编程式事务模板、线程池工厂。

`baseon-core` 由 ArchUnit 强制零 Spring 依赖，因此该包物理上独立成 `baseon-core-boot` 构件，
逻辑包名仍为 `io.github.ayongbox.baseon.core.boot`。只允许依赖 `spring-context` / `spring-tx` /
`spring-boot-autoconfigure`，禁止依赖 `spring-cloud-*` 与任何 Starter。

## 自动装配清单

| 类 | 作用 |
|---|---|
| `BaseonCoreBootAutoConfiguration` | 注册 `SpringContextHolder`，容器启动时回填静态引用 |

登记文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

## 配置项

| 配置 | 默认 | 说明 |
|---|---|---|
| `baseon.boot.enabled` | `true` | 关闭后不注册 `SpringContextHolder`，`TransactionUtil` 等依赖容器的工具不可用 |

## 使用场景

### 场景 1：静态上下文取 Bean

无法构造器注入的场景（第三方回调、静态工具、`main` 方法）才使用：

```java
OrderService orderService = SpringContextHolder.getBean(OrderService.class);
String region = SpringContextHolder.getProperty("baseon.region", "cn");
```

可选依赖使用 `getBeanOrNull`，容器未就绪或 Bean 不存在都返回 `null`：

```java
SmsSender sender = SpringContextHolder.getBeanOrNull(SmsSender.class);
if (sender == null) {
    log.warn("未接入短信通道，跳过通知");
}
```

事件发布：

```java
SpringContextHolder.publishEvent(new OrderPaidEvent(orderId));
```

### 场景 2：静态方法 / Lambda 中开启事务

`@Transactional` 只能作用于 Spring 代理的 Bean 方法，静态工具与回调里用编程式事务：

```java
TransactionUtil.executeWithoutResult(status -> {
    orderMapper.insert(order);
    stockMapper.decrease(order.getSkuId(), order.getQuantity());
});
```

需要独立提交（外层事务回滚不影响内层）：

```java
TransactionUtil.executeInNewTransaction(status -> {
    auditMapper.insert(record);   // 审计日志独立落库
    return null;
});
```

提交后回调（例如事务成功后再发 MQ）：

```java
TransactionUtil.executeWithoutResult(status -> {
    orderMapper.insert(order);
    TransactionUtil.afterCommit(() -> mqSender.send(order));
});
```

### 场景 3：统一线程池

```java
ThreadPoolExecutor executor = ThreadPoolFactory.build(
        "order-async", 4, 8, 256, 60, ThreadPoolFactory.RejectionPolicy.CALLER_RUNS);

executor.submit(() -> {
    // 自动带上提交线程的 LoginUser / tenantId
    reportService.generate(SecurityUtil.getTenantId());
});
```

周期任务：

```java
ScheduledThreadPoolExecutor scheduler = ThreadPoolFactory.buildScheduled("order-retry", 1);
scheduler.scheduleAtFixedRate(() -> retryService.retryFailed(), 0, 30, TimeUnit.SECONDS);
```

运行快照接入日志/监控端点：

```java
ThreadPoolStats stats = ThreadPoolFactory.snapshot(executor);
log.info("thread-pool active={}, queue={}/{}", stats.activeCount(), stats.queueSize(), stats.queueCapacity());
```

## 注意事项

1. **优先构造器注入**：`SpringContextHolder` 只用于无法注入的场景；业务代码直接依赖它会让依赖关系不可见。
2. **事务管理器必须唯一**：`TransactionUtil` 按类型从容器取 `PlatformTransactionManager`；多数据源场景必须用 `@Primary` 指定默认事务管理器，否则启动期/首次调用即报 `NoUniqueBeanDefinitionException`。
3. **受检异常默认不回滚**：与 `@Transactional` 默认规则一致。需要回滚时在回调里调用 `status.setRollbackOnly()`，不要指望抛受检异常。
4. **NESTED 需要显式开启**：`DataSourceTransactionManager` 默认 `nestedTransactionAllowed=false`，`executeInNestedTransaction` 会抛 `NestedTransactionNotSupportedException`（P-029）。
5. **线程池归调用方所有**：工厂只创建不托管生命周期；非守护线程会阻止 JVM 退出，停机钩子里必须 `shutdown()`。
6. **上下文在提交时捕获**：`ThreadPoolFactory` 在提交线程捕获 `LoginUser`；周期任务在 `schedule` 时捕获一次，之后固定使用该份上下文。
7. **Spring 依赖为 optional**：本构件不强制传递 spring-context / spring-tx；纯 Spring 工程直接使用 `TransactionUtil` 时需自行确保 `spring-tx` 在类路径。
