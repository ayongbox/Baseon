package io.github.ayongbox.baseon.starter.redis.config;

import io.github.ayongbox.baseon.starter.redis.support.RedisLock;
import io.github.ayongbox.baseon.starter.redis.support.RedisUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 增强能力自动装配。
 *
 * <p>连接工厂与 {@link StringRedisTemplate} 本体由 Boot 的 RedisAutoConfiguration 装配，
 * 这里只装配脚手架增强能力：统一序列化的 {@code RedisTemplate<String, Object>}、
 * {@link RedisUtil} 与 {@link RedisLock}。
 *
 * <p>自定义 redisTemplate：key/hashKey 用 String 序列化（可读、可命令行排查），
 * value/hashValue 用 JSON 序列化（跨语言、不依赖 JDK 序列化白名单）。
 * 与 Boot 默认的 {@code RedisTemplate<Object, Object>} 通过
 * {@code @ConditionalOnMissingBean(name = "redisTemplate")} 互斥——双方条件相同，
 * 本自动配置在 imports 中先注册即先生效，Boot 随后退避。
 */
@AutoConfiguration
@ConditionalOnClass(RedisOperations.class)
@EnableConfigurationProperties(BaseonRedisProperties.class)
public class BaseonRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisUtil redisUtil(StringRedisTemplate stringRedisTemplate, BaseonRedisProperties properties) {
        return new RedisUtil(stringRedisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisLock redisLock(StringRedisTemplate stringRedisTemplate, BaseonRedisProperties properties) {
        return new RedisLock(stringRedisTemplate, properties);
    }
}
