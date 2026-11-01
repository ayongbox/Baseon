package io.github.ayongbox.baseon.core.spi;

/**
 * MQ 消费者监听器 SPI：业务实现本接口并标注 {@code @MqListener} 即完成消费接入，
 * 实现 Bean 由 starter-mq-* 在启动期扫描并注册到 MQ 客户端。
 *
 * <p>消费约定：
 * <ol>
 *   <li>方法执行前框架已从消息头恢复登录上下文（{@code SecurityUtil} 可读），
 *       返回后自动清理；</li>
 *   <li>抛异常等价于返回 {@link ConsumeResult#RETRY}，消息将被重投；</li>
 *   <li>幂等由框架兜底：消息 {@code key}（为空时退化用 msgId）在窗口期内重复到达时
 *       直接跳过，不进入本方法；窗口由实现侧配置。</li>
 * </ol>
 */
public interface MqConsumerListener {

    /**
     * 处理单条消息。
     *
     * @param message 消费端重建的消息（含 msgId、透传头）
     * @return 消费结果；{@code null} 按 {@link ConsumeResult#SUCCESS} 处理
     */
    ConsumeResult onMessage(MqMessage message);
}
