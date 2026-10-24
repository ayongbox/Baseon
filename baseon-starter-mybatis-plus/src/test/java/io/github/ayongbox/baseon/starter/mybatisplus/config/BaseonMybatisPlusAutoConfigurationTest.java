package io.github.ayongbox.baseon.starter.mybatisplus.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import io.github.ayongbox.baseon.starter.mybatisplus.interceptor.SlowSqlInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BaseonMybatisPlusAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseonMybatisPlusAutoConfiguration.class));

    @Test
    void onlyPaginationInnerInterceptorWhenNoTenantHandler() {
        contextRunner.run(context -> {
            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
            List<InnerInterceptor> innerInterceptors = interceptor.getInterceptors();
            assertThat(innerInterceptors).hasSize(1);
            assertThat(innerInterceptors.get(0)).isInstanceOf(PaginationInnerInterceptor.class);
        });
    }

    @Test
    void tenantInnerInterceptorPrecedesPaginationWhenHandlerPresent() {
        contextRunner.withBean(TenantLineHandler.class, () -> mock(TenantLineHandler.class))
                .run(context -> {
                    MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
                    List<InnerInterceptor> innerInterceptors = interceptor.getInterceptors();
                    assertThat(innerInterceptors).hasSize(2);
                    assertThat(innerInterceptors.get(0)).isInstanceOf(TenantLineInnerInterceptor.class);
                    assertThat(innerInterceptors.get(1)).isInstanceOf(PaginationInnerInterceptor.class);
                });
    }

    @Test
    void metaObjectHandlerBeanRegistered() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(MyMetaObjectHandler.class));
    }

    @Test
    void slowSqlInterceptorRegisteredIntoMybatisConfiguration() {
        contextRunner.withPropertyValues("baseon.mybatis-plus.slow-sql-threshold-ms=1234")
                .run(context -> {
                    ConfigurationCustomizer customizer = context.getBean(ConfigurationCustomizer.class);
                    MybatisConfiguration configuration = new MybatisConfiguration();
                    customizer.customize(configuration);
                    assertThat(configuration.getInterceptors())
                            .hasAtLeastOneElementOfType(SlowSqlInterceptor.class);
                    BaseonMybatisPlusProperties properties = context.getBean(BaseonMybatisPlusProperties.class);
                    assertThat(properties.getSlowSqlThresholdMs()).isEqualTo(1234);
                });
    }

    @Test
    void slowSqlInterceptorAbsentWhenSwitchOff() {
        contextRunner.withPropertyValues("baseon.mybatis-plus.slow-sql-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ConfigurationCustomizer.class));
    }
}
