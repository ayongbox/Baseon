package io.github.ayongbox.baseon.starter.datascope.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * starter-data-scope 集成测试：H2 + MyBatis-Plus 全链路验证。
 *
 * <p>关键验证点：
 * <ul>
 *   <li>@DataScope 触发 SQL 注入，仅返回可见部门数据</li>
 *   <li>@IgnoreDataScope 跳过注入，返回全表</li>
 *   <li>无注解时上下文为空，拦截器放行</li>
 *   <li>空可见集（Set.of()）时返回空列表，不抛错</li>
 * </ul>
 */
@SpringBootTest(classes = DataScopeItConfig.class, properties = {
    "spring.datasource.url=jdbc:h2:mem:datascope_it;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:schema.sql",
    "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"
})
class DataScopeIntegrationTest {

    @Autowired
    private DataScopeDemoService service;

    @BeforeEach
    void resetVisibleIds() {
        DataScopeItConfig.VISIBLE_IDS.set(Set.of(10L));
    }

    @Test
    void shouldFilterRowsByDeptIdWhenDataScopePresent() {
        List<DataScopeDemoEntity> result = service.listScoped();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e -> e.getDeptId().equals(10L));
    }

    @Test
    void shouldReturnAllRowsWhenIgnoreDataScopePresent() {
        List<DataScopeDemoEntity> result = service.listIgnoringScope();
        assertThat(result).hasSize(6);
    }

    @Test
    void shouldReturnAllRowsWhenNoAnnotation() {
        List<DataScopeDemoEntity> result = service.listWithoutAnnotation();
        assertThat(result).hasSize(6);
    }

    @Test
    void shouldReturnEmptyWhenVisibleIdsEmpty() {
        DataScopeItConfig.VISIBLE_IDS.set(Set.of());
        List<DataScopeDemoEntity> result = service.listScoped();
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnMultiDeptRowsWhenVisibleIdsHasMultiple() {
        DataScopeItConfig.VISIBLE_IDS.set(Set.of(10L, 20L));
        List<DataScopeDemoEntity> result = service.listScoped();
        assertThat(result).hasSize(4);
        assertThat(result).allMatch(e -> e.getDeptId() == 10L || e.getDeptId() == 20L);
    }

    @Test
    void shouldReturnAllRowsWhenVisibleIdsNullMeansUnlimited() {
        DataScopeItConfig.VISIBLE_IDS.set(null);
        List<DataScopeDemoEntity> result = service.listScoped();
        assertThat(result).hasSize(6);
    }
}
