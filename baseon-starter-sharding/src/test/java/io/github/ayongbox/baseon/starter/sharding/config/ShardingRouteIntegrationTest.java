package io.github.ayongbox.baseon.starter.sharding.config;

import io.github.ayongbox.baseon.starter.sharding.support.ShardingRoutingDataSourceContributor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分库分表端到端验证：双 H2 内存库（MySQL 兼容模式），
 * t_order 按 user_id 取模路由到 ds0/ds1，聚合查询跨库归并。
 */
class ShardingRouteIntegrationTest {

    private static final String DS0_URL = "jdbc:h2:mem:it_ds0;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String DS1_URL = "jdbc:h2:mem:it_ds1;MODE=MySQL;DB_CLOSE_DELAY=-1";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseonShardingAutoConfiguration.class))
            .withPropertyValues(
                    "baseon.sharding.rules-location=classpath:sharding-rules-it.yaml",
                    "baseon.sharding.datasources.ds0.url=" + DS0_URL,
                    "baseon.sharding.datasources.ds0.driver-class-name=org.h2.Driver",
                    "baseon.sharding.datasources.ds1.url=" + DS1_URL,
                    "baseon.sharding.datasources.ds1.driver-class-name=org.h2.Driver");

    @Test
    void routesRowsToDifferentShardsByShardingColumn() {
        contextRunner.run(context -> {
            ShardingRoutingDataSourceContributor contributor =
                    context.getBean(ShardingRoutingDataSourceContributor.class);
            Map<Object, Object> targets = new HashMap<>();
            contributor.contribute(targets);
            DataSource sharding = (DataSource) targets.get("sharding");

            try (Connection conn = sharding.getConnection(); Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE t_order (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, name VARCHAR(32))");
                st.execute("INSERT INTO t_order (id, user_id, name) VALUES (1, 101, 'a')");
                st.execute("INSERT INTO t_order (id, user_id, name) VALUES (2, 102, 'b')");
                // 无分片键查询：全库路由 + 归并
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM t_order")) {
                    rs.next();
                    assertThat(rs.getLong(1)).isEqualTo(2);
                }
                // 带分片键查询：单库路由
                try (ResultSet rs = st.executeQuery("SELECT name FROM t_order WHERE user_id = 101")) {
                    rs.next();
                    assertThat(rs.getString(1)).isEqualTo("a");
                }
            }

            // 取模路由断言：102 % 2 = 0 → ds0，101 % 2 = 1 → ds1
            assertThat(countByUserId(DS0_URL, 102)).isEqualTo(1);
            assertThat(countByUserId(DS0_URL, 101)).isZero();
            assertThat(countByUserId(DS1_URL, 101)).isEqualTo(1);
            assertThat(countByUserId(DS1_URL, 102)).isZero();
        });
    }

    /**
     * 绕过 ShardingSphere 直连物理库，验证行真实落位。
     */
    private long countByUserId(String url, long userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM t_order WHERE user_id = " + userId)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
