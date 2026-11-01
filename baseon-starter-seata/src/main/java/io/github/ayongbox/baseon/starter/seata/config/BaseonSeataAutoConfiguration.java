package io.github.ayongbox.baseon.starter.seata.config;

import io.github.ayongbox.baseon.starter.seata.support.SeataGlobalTransactionExecutor;
import org.apache.seata.tm.api.TransactionalTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 分布式事务自动装配（可选）：Seata 自身的 GlobalTransactionScanner、数据源自动代理
 * 由官方 starter 负责，本类只补 baseon 侧的编程式入口。
 *
 * <p>装配条件：baseon.seata.enabled（默认 true）与 seata.enabled 均为 true。
 * 关闭 baseon 开关时 {@link BaseonSeataEnvironmentPostProcessor} 会同步把
 * {@code seata.enabled} 置 false，Seata 原生装配随之失效。
 */
@AutoConfiguration
@ConditionalOnClass(TransactionalTemplate.class)
@EnableConfigurationProperties(BaseonSeataProperties.class)
@ConditionalOnExpression("${baseon.seata.enabled:true} and ${seata.enabled:true}")
public class BaseonSeataAutoConfiguration {

    /**
     * 可替换：业务可自定义 {@link TransactionalTemplate}（如重写失败处理），执行器自动复用。
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionalTemplate seataTransactionalTemplate() {
        return new TransactionalTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public SeataGlobalTransactionExecutor seataGlobalTransactionExecutor(
            TransactionalTemplate seataTransactionalTemplate) {
        return new SeataGlobalTransactionExecutor(seataTransactionalTemplate);
    }
}
