package io.github.ayongbox.baseon.starter.datasource.support;

import io.github.ayongbox.baseon.starter.datasource.config.BaseonDatasourceProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicRoutingDataSourceTest {

    private final BaseonDatasourceProperties properties = new BaseonDatasourceProperties();
    private final DynamicRoutingDataSource dataSource =
            new DynamicRoutingDataSource(properties, List.of("slave1", "slave2"));

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clear();
    }

    @Test
    void explicitDsAnnotationWinsOverReadOnlyTx() {
        assertThat(dataSource.resolveKey("slave1", true)).isEqualTo("slave1");
        assertThat(dataSource.resolveKey("slave2", false)).isEqualTo("slave2");
    }

    @Test
    void readOnlyTxRoundRobinsSlaves() {
        String first = dataSource.resolveKey(null, true);
        String second = dataSource.resolveKey(null, true);
        String third = dataSource.resolveKey(null, true);

        assertThat(first).isNotEqualTo(second);
        assertThat(first).isEqualTo("slave1");
        assertThat(second).isEqualTo("slave2");
        assertThat(third).isEqualTo("slave1");
    }

    @Test
    void writeTxFallsBackToMaster() {
        assertThat(dataSource.resolveKey(null, false)).isEqualTo("master");
        assertThat(dataSource.resolveKey(" ", false)).isEqualTo("master");
    }

    @Test
    void readOnlyTxWithoutSlavesFallsBackToMaster() {
        DynamicRoutingDataSource noSlave = new DynamicRoutingDataSource(properties, List.of());
        assertThat(noSlave.resolveKey(null, true)).isEqualTo("master");
    }

    @Test
    void readOnlyTxWithSplitDisabledFallsBackToMaster() {
        properties.setReadWriteSplitEnabled(false);
        assertThat(dataSource.resolveKey(null, true)).isEqualTo("master");
    }

    @Test
    void determineCurrentLookupKeyReadsContextHolder() {
        DataSourceContextHolder.set("slave1");
        assertThat(dataSource.determineCurrentLookupKey()).isEqualTo("slave1");
    }
}
