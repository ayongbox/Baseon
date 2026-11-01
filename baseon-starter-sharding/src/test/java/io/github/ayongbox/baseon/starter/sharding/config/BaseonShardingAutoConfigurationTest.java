package io.github.ayongbox.baseon.starter.sharding.config;

import io.github.ayongbox.baseon.starter.sharding.support.ShardingRoutingDataSourceContributor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分片装配的开关语义：rules-location 是唯一触发点，缺失/关总开关时整体不装配。
 */
class BaseonShardingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseonShardingAutoConfiguration.class));

    @Test
    void notRegisteredWithoutRulesLocation() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(ShardingRoutingDataSourceContributor.class));
    }

    @Test
    void notRegisteredWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "baseon.sharding.enabled=false",
                        "baseon.sharding.rules-location=classpath:sharding-rules-it.yaml")
                .run(context ->
                        assertThat(context).doesNotHaveBean(ShardingRoutingDataSourceContributor.class));
    }

    @Test
    void failsFastWhenPhysicalDataSourcesMissing() {
        contextRunner
                .withPropertyValues("baseon.sharding.rules-location=classpath:sharding-rules-it.yaml")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
                });
    }

    @Test
    void failsFastWhenRulesFileNotFound() {
        contextRunner
                .withPropertyValues(
                        "baseon.sharding.rules-location=classpath:not-exists.yaml",
                        "baseon.sharding.datasources.ds0.url=jdbc:h2:mem:missing_rules;MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "baseon.sharding.datasources.ds0.driver-class-name=org.h2.Driver")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
                });
    }
}
