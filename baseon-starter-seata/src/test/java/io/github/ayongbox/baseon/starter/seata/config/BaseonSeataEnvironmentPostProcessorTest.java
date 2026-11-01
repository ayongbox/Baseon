package io.github.ayongbox.baseon.starter.seata.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * baseon.seata → seata.* 的属性映射语义：只映射显式配置项，且以 baseon 为最高优先级。
 */
class BaseonSeataEnvironmentPostProcessorTest {

    private final BaseonSeataEnvironmentPostProcessor processor = new BaseonSeataEnvironmentPostProcessor();

    @Test
    void disabledMapsToSeataEnabledFalse() {
        MockEnvironment environment = new MockEnvironment().withProperty("baseon.seata.enabled", "false");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("seata.enabled")).isEqualTo("false");
    }

    @Test
    void atModeEnablesDataSourceProxy() {
        MockEnvironment environment = new MockEnvironment().withProperty("baseon.seata.mode", "AT");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("seata.enable-auto-data-source-proxy")).isEqualTo("true");
        assertThat(environment.getProperty("seata.data-source-proxy-mode")).isEqualTo("AT");
    }

    @Test
    void tccModeDisablesDataSourceProxy() {
        MockEnvironment environment = new MockEnvironment().withProperty("baseon.seata.mode", "tcc");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("seata.enable-auto-data-source-proxy")).isEqualTo("false");
    }

    @Test
    void mapsServiceGroupAndApplicationId() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("baseon.seata.tx-service-group", "order-tx-group")
                .withProperty("baseon.seata.application-id", "order-service");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("seata.tx-service-group")).isEqualTo("order-tx-group");
        assertThat(environment.getProperty("seata.application-id")).isEqualTo("order-service");
    }

    @Test
    void leavesSeataPropertiesUntouchedWhenNoExplicitBaseonKey() {
        MockEnvironment environment = new MockEnvironment().withProperty("seata.tx-service-group", "user-group");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getPropertySources()
                .contains(BaseonSeataEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isFalse();
        assertThat(environment.getProperty("seata.tx-service-group")).isEqualTo("user-group");
    }

    @Test
    void mappingOverridesSameNameSeataProperty() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("seata.enabled", "true")
                .withProperty("baseon.seata.enabled", "false");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("seata.enabled")).isEqualTo("false");
    }

    @Test
    void isIdempotent() {
        MockEnvironment environment = new MockEnvironment().withProperty("baseon.seata.enabled", "false");

        processor.postProcessEnvironment(environment, null);
        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getPropertySources()
                .contains(BaseonSeataEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isTrue();
        assertThat(environment.getProperty("seata.enabled")).isEqualTo("false");
    }

    @Test
    void registeredAsSpringEnvironmentPostProcessor() {
        // 只取工厂名不实例化：Boot 自带的部分 EnvironmentPostProcessor 需要 DeferredLogFactory 才能构造
        List<String> factories = SpringFactoriesLoader.loadFactoryNames(
                EnvironmentPostProcessor.class, getClass().getClassLoader());

        assertThat(factories).contains(BaseonSeataEnvironmentPostProcessor.class.getName());
    }

    @Test
    void rejectsInvalidSwitchAndMode() {
        assertThatThrownBy(() -> processor.postProcessEnvironment(
                new MockEnvironment().withProperty("baseon.seata.enabled", "yes"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("baseon.seata.enabled");

        assertThatThrownBy(() -> processor.postProcessEnvironment(
                new MockEnvironment().withProperty("baseon.seata.mode", "xa"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("baseon.seata.mode");
    }
}
