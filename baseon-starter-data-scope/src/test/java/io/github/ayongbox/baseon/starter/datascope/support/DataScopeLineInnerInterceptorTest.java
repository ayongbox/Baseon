package io.github.ayongbox.baseon.starter.datascope.support;

import io.github.ayongbox.baseon.core.context.DataScopeIgnoreContext;
import io.github.ayongbox.baseon.starter.datascope.config.BaseonDataScopeProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataScopeLineInnerInterceptor} SQL 改写验证：通过公共入口
 * {@code parserSingle(sql, null)} 走完整的 JSqlParser 解析 + 改写链路，
 * 覆盖单表 / JOIN / 子查询 / 白名单 / 空可见集 / 自定义列 / 表别名 / 忽略标记。
 */
class DataScopeLineInnerInterceptorTest {

    private final BaseonDataScopeProperties properties = new BaseonDataScopeProperties();
    private final DataScopeLineInnerInterceptor interceptor = new DataScopeLineInnerInterceptor(properties);

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
        DataScopeIgnoreContext.clearIgnored();
        properties.setIgnoreTables(List.of());
    }

    @Test
    void shouldInjectInConditionOnSingleTableSelect() {
        DataScopeContext.set(DataScopeContext.of("dept_id", null, Set.of(10L, 20L)));

        String sql = interceptor.parserSingle("SELECT id, name FROM sys_user WHERE status = 1", null)
                .toUpperCase();

        // Set.of 顺序不稳定，分别断言两个 ID 均在 IN 列表里
        assertThat(sql).contains("DEPT_ID IN");
        assertThat(sql).contains("10");
        assertThat(sql).contains("20");
        assertThat(sql).contains("STATUS = 1");
    }

    @Test
    void shouldInjectOnEachNonIgnoredTableInJoin() {
        DataScopeContext.set(DataScopeContext.of("dept_id", null, Set.of(10L)));

        String sql = interceptor.parserSingle(
                "SELECT u.id, o.id FROM sys_user u LEFT JOIN sys_order o ON o.user_id = u.id", null)
                .toUpperCase();

        // 两张表都要带 dept_id 条件；LEFT JOIN 的条件应追加到 ON 子句
        assertThat(countOccurrences(sql, "DEPT_ID IN (10)")).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldNotInjectWhenNoDataScopeAnnotation() {
        // 未设置 DataScopeContext：模拟无 @DataScope 场景
        String sql = interceptor.parserSingle("SELECT id FROM sys_user", null).toUpperCase();
        assertThat(sql).doesNotContain("DEPT_ID");
    }

    @Test
    void shouldNotInjectWhenIgnoreDataScopeMarked() {
        DataScopeContext.set(DataScopeContext.of("dept_id", null, Set.of(10L)));
        DataScopeIgnoreContext.markIgnored();

        String sql = interceptor.parserSingle("SELECT id FROM sys_user", null).toUpperCase();
        assertThat(sql).doesNotContain("DEPT_ID");
    }

    @Test
    void shouldNotInjectOnWhitelistedTable() {
        properties.setIgnoreTables(List.of("sys_dict"));
        DataScopeContext.set(DataScopeContext.of("dept_id", null, Set.of(10L)));

        String sql = interceptor.parserSingle("SELECT id FROM sys_dict", null).toUpperCase();
        assertThat(sql).doesNotContain("DEPT_ID");
    }

    @Test
    void shouldInjectImpossibleEqualsWhenVisibleIdsEmpty() {
        DataScopeContext.set(DataScopeContext.of("dept_id", null, Collections.emptySet()));

        String sql = interceptor.parserSingle("SELECT id FROM sys_user", null).toUpperCase();
        assertThat(sql).contains("DEPT_ID = -1");
    }

    @Test
    void shouldNotInjectWhenVisibleIdsNullMeansUnlimited() {
        DataScopeContext.set(DataScopeContext.of("dept_id", null, null));

        String sql = interceptor.parserSingle("SELECT id FROM sys_user", null).toUpperCase();
        assertThat(sql).doesNotContain("DEPT_ID");
    }

    @Test
    void shouldUseCustomColumnNameWhenAnnotationSpecified() {
        DataScopeContext.set(DataScopeContext.of("project_id", null, Set.of(7L)));

        String sql = interceptor.parserSingle("SELECT id FROM biz_task", null).toUpperCase();
        assertThat(sql).contains("PROJECT_ID IN (7)");
        assertThat(sql).doesNotContain("DEPT_ID");
    }

    @Test
    void shouldRespectTableAliasFilterWhenDeclared() {
        // 仅别名 u 的表注入条件
        DataScopeContext.set(DataScopeContext.of("dept_id", "u", Set.of(10L)));

        String sql = interceptor.parserSingle(
                "SELECT * FROM sys_user u JOIN sys_order o ON o.user_id = u.id", null)
                .toUpperCase();

        assertThat(sql).contains("U.DEPT_ID IN (10)");
        assertThat(sql).doesNotContain("O.DEPT_ID");
    }

    @Test
    void shouldInjectIntoSubQuery() {
        DataScopeContext.set(DataScopeContext.of("dept_id", null, Set.of(10L)));

        String sql = interceptor.parserSingle(
                "SELECT id FROM sys_user WHERE dept_id IN (SELECT dept_id FROM sys_dept WHERE pid = 1)",
                null).toUpperCase();

        assertThat(countOccurrences(sql, "DEPT_ID IN (10)")).isGreaterThanOrEqualTo(2);
    }

    private int countOccurrences(String sql, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = sql.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
