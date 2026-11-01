package io.github.ayongbox.baseon.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MQ 消费监听注解：标注在 {@code MqConsumerListener} 实现类上，声明订阅关系。
 *
 * <p>注册逻辑由 starter-mq-* 提供；未引入任何 starter-mq-* 时本注解不生效。
 * 实现类缺少本注解会导致启动失败（配置错误应尽早暴露，不做静默跳过）。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqListener {

    /** 订阅主题 */
    String topic();

    /** 消费组：同组内负载均摊，不同组各自全量接收 */
    String consumerGroup();

    /** 过滤标签表达式，默认订阅全部；RocketMQ 支持 {@code "a || b"} 写法 */
    String tags() default "*";
}
