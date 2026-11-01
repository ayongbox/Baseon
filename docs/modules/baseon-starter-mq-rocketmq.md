# baseon-starter-mq-rocketmq · MQ 抽象（core.spi）的 RocketMQ 实现

## 定位

统一消息能力底座。core.spi 定义与中间件无关的契约（`MqProducer` / `MqConsumerListener` / `MqMessage`），本模块提供 RocketMQ 4.9.7 经典 remoting 客户端实现（兼容 4.9 / 5.x 服务端）。业务只面向 core 契约编程，替换 MQ 实现时业务代码零改动。

内置三项横切能力，业务监听器零感知：

1. **消费幂等**：引入 starter-redis 时自动生效，同一业务 key 在窗口期内重复到达直接跳过；
2. **登录上下文透传**：发送时自动注入 userId/tenantId/username，消费前恢复到 `SecurityUtil`；
3. **失败重投**：监听器返回 `RETRY` 或抛异常 → `RECONSUME_LATER`，由 RocketMQ 按重投策略投递。

引入方式（版本由 BOM 管控，消费幂等可选依赖 starter-redis）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-mq-rocketmq</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `DefaultMQProducer` | RocketMQ 原生生产者（destroyMethod=shutdown） | 配置了 `name-server` 才装配 |
| `RocketMqProducerAdapter` | `MqProducer` 契约实现 | 同上，`@ConditionalOnMissingBean` |
| `MqConsumerRegistrar` | 扫描 `MqConsumerListener` Bean，按 `@MqListener` 注解注册 PushConsumer | 同上 |

注册方式：`AutoConfiguration.imports`。`baseon.mq.rocketmq.enabled=false` 或未配置 `name-server` 时整体不装配（视为未启用 MQ）。

## 配置项

```yaml
baseon:
  mq:
    rocketmq:
      enabled: true                    # 总开关
      name-server: 127.0.0.1:9876      # 唯一必填，多个用分号分隔；为空则不装配
      access-key:                      # ACL，与 secret-key 成对；集群未开 ACL 留空
      secret-key:
      producer-group: baseon-producer  # 生产者组名
      send-timeout-millis: 3000        # 同步发送超时
      idempotent-consume: true         # 消费幂等开关（需容器中有 RedisUtil）
      idempotent-ttl-seconds: 86400    # 幂等窗口，默认 24h，覆盖 16 次重投全周期
      context-propagate: true          # 登录上下文透传开关
      consume-timeout-minutes: 15      # 单条消费超时，超时按失败重投
```

## 发送消息

注入 core 契约 `MqProducer`（不要直接注入 RocketMQ 原生类型）：

```java
// 最简：topic + body
mqProducer.send("order-topic", "{\"orderId\":1}");

// 完整：tag/key/body，key 用于幂等与排查
MqMessage msg = MqMessage.of("order-topic", "paid", "order-1001", body);
mqProducer.send(msg);                          // 同步，失败抛 BizException(REMOTE_CALL_FAILED)
mqProducer.sendAsync(msg, callback);           // 异步，callback 收 error
mqProducer.sendOneway(msg);                    // 单向，不关心结果
mqProducer.sendDelay(msg, 30_000);             // 延迟 30 秒（就近向上映射到固定级别）
```

## 消费消息

实现 `MqConsumerListener` 并标注 `@MqListener`（二者缺一不可，缺注解启动失败）：

```java
@Component
@MqListener(topic = "order-topic", consumerGroup = "order-service", tags = "paid")
public class OrderPaidListener implements MqConsumerListener {

    @Override
    public ConsumeResult onMessage(MqMessage message) {
        // message.getBody() 业务体；SecurityUtil.get() 已恢复发送方登录上下文
        // 返回 RETRY 或抛异常 → RECONSUME_LATER 重投
        return ConsumeResult.SUCCESS;
    }
}
```

- `tags` 默认 `"*"` 订阅全部，支持 `"a||b"` 多标签；
- 单条消费（batchMaxSize=1），一条一结果；
- 同一 `consumerGroup` 多实例自动负载（集群消费模式）。

## 消费幂等与上下文透传

幂等 key 形如 `mq:idem:[租户前缀]业务key`：业务 key 取 `MqMessage.getKey()`，为空时用 msgId 兜底。引入 starter-tenant 后自动带租户前缀，跨租户互不拦截；未引入 starter-redis 时幂等自动跳过（降级为至少一次语义）。

上下文透传：发送方调 `send*` 时若 `SecurityUtil.isLogin()`，自动把 userId/tenantId/username 写入消息头（常量收口在 `MqMessage.HEADER_*`）；消费方执行监听器前恢复，finally 清理。定时任务等非登录场景发送的消息，消费侧上下文为 null，属正常语义。

## 注意事项

1. **延迟消息是固定级别映射**：RocketMQ 4.9.x 只有 18 个固定延迟级别（1s~2h），`sendDelay` 按就近向上映射，不是任意毫秒精度；需要精确定时的业务走调度任务。
2. **消费幂等依赖共享 Redis**：多实例必须连同一 Redis，否则重投打到不同实例会绕过幂等（与 starter-idempotent 同语义）。
3. **业务 key 是幂等的稳定标识**：msgId 兜底只覆盖 Broker 重投；生产端重试（发送超时重发）会产生新 msgId，有天然业务键的消息务必设置 key。
4. **同步发送失败即抛异常，不重试**：需要重试语义走 `sendAsync` 回调自行补偿，或让上游（如本地消息表）兜底。
5. **监听器必须幂等友好**：即使开了消费幂等，窗口期外或 Redis 抖动时仍可能重复投递，业务侧写操作应天然幂等（唯一键/状态机）。
6. **`rocketmq-acl` 需显式依赖**：ACL 类不在 rocketmq-client 传递闭包内（P-034）；本 starter 已声明，业务工程无需再引。
7. **消费者组名全局唯一**：`consumerGroup` 冲突会导致消息被其他应用的同组实例抢走，按「应用名-用途」命名。
