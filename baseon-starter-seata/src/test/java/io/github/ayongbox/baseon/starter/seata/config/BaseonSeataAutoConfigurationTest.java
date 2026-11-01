package io.github.ayongbox.baseon.starter.seata.config;

import io.github.ayongbox.baseon.starter.seata.support.SeataGlobalTransactionExecutor;
import org.apache.seata.tm.api.TransactionalTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 装配开关：baseon.seata.enabled 与 seata.enabled 任一为 false 即不装配；
 * 业务自定义 TransactionalTemplate 时执行器复用之。
 */
class BaseonSeataAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseonSeataAutoConfiguration.class));

    @Test
    void registeredByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SeataGlobalTransactionExecutor.class);
            assertThat(context).hasSingleBean(TransactionalTemplate.class);
        });
    }

    @Test
    void notRegisteredWhenBaseonSwitchOff() {
        contextRunner
                .withPropertyValues("baseon.seata.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SeataGlobalTransactionExecutor.class));
    }

    @Test
    void notRegisteredWhenSeataSwitchOff() {
        contextRunner
                .withPropertyValues("seata.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SeataGlobalTransactionExecutor.class));
    }

    @Test
    void reusesUserDefinedTemplate() {
        contextRunner
                .withUserConfiguration(CustomTemplateConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SeataGlobalTransactionExecutor.class);
                    assertThat(context).hasSingleBean(TransactionalTemplate.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTemplateConfiguration {

        @Bean
        TransactionalTemplate customTransactionalTemplate() {
            return new TransactionalTemplate();
        }
    }
}
