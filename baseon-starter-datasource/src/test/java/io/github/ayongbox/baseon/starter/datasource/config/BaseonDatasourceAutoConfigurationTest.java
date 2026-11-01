package io.github.ayongbox.baseon.starter.datasource.config;

import io.github.ayongbox.baseon.starter.datasource.support.RoutingDataSourceContributor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 动态数据源装配的扩展点验证：{@link RoutingDataSourceContributor} 放入的数据源
 * 必须进入最终路由表，且执行时机在主库/从库之后。
 */
class BaseonDatasourceAutoConfigurationTest {

    private final BaseonDatasourceAutoConfiguration configuration = new BaseonDatasourceAutoConfiguration();

    @Test
    void contributorAppendsDataSourceAfterMasterAndSlaves() {
        BaseonDatasourceProperties properties = new BaseonDatasourceProperties();
        properties.getMaster().setUrl("jdbc:stub://master");
        // Hikari setDriverClassName 会立即加载并实例化驱动类，单测用桩驱动避免引入真实 JDBC 驱动
        properties.getMaster().setDriverClassName(StubDriver.class.getName());
        BaseonDatasourceProperties.DataSourceConfig slave = new BaseonDatasourceProperties.DataSourceConfig();
        slave.setUrl("jdbc:stub://slave");
        slave.setDriverClassName(StubDriver.class.getName());
        properties.getSlaves().add(slave);

        AtomicReference<Map<Object, Object>> seenByContributor = new AtomicReference<>();
        DataSource shardingDataSource = mock(DataSource.class);
        RoutingDataSourceContributor contributor = targets -> {
            seenByContributor.set(Map.copyOf(targets));
            targets.put("sharding", shardingDataSource);
        };

        DataSource routing = configuration.dataSource(properties, providerOf(contributor));

        // contributor 执行时主从已装配完成
        assertThat(seenByContributor.get()).containsKeys("master", "slave1");
        // contributor 放入的数据源进入最终路由表
        assertThat(resolvedRoutingTable(routing)).containsEntry("sharding", shardingDataSource);
    }

    @Test
    void noContributorKeepsRoutingTableUnchanged() {
        BaseonDatasourceProperties properties = new BaseonDatasourceProperties();
        properties.getMaster().setUrl("jdbc:stub://master");
        properties.getMaster().setDriverClassName(StubDriver.class.getName());

        DataSource routing = configuration.dataSource(properties, providerOf());

        assertThat(resolvedRoutingTable(routing)).containsOnlyKeys("master");
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> resolvedRoutingTable(DataSource routing) {
        return (Map<Object, Object>) ReflectionTestUtils.getField(routing, "resolvedDataSources");
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<RoutingDataSourceContributor> providerOf(
            RoutingDataSourceContributor... contributors) {
        ObjectProvider<RoutingDataSourceContributor> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.of(contributors));
        return provider;
    }
}
