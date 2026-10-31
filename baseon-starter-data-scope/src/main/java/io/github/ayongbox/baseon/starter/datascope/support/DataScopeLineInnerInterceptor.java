package io.github.ayongbox.baseon.starter.datascope.support;

import com.baomidou.mybatisplus.extension.plugins.inner.BaseMultiTableInnerInterceptor;
import io.github.ayongbox.baseon.core.context.DataScopeIgnoreContext;
import io.github.ayongbox.baseon.starter.datascope.config.BaseonDataScopeProperties;
import io.github.ayongbox.baseon.starter.mybatisplus.interceptor.DataScopeInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.List;
import java.util.Set;

/**
 * 行级数据权限拦截器：复用 MP 多表 AST 改写链路（JOIN/子查询/UNION 自动遍历），
 * 把 {@code dept_id IN (...)} 条件拼进每张相关表的 WHERE/ON 子句。
 *
 * <p>装配在 starter-mybatis-plus 的 {@code MybatisPlusInterceptor} 中，位置固定
 * 在租户插件之后、分页插件之前：保证 count SQL 也带上数据范围条件。
 *
 * <p>跳过条件（任一满足即不改写）：
 * <ul>
 *   <li>{@code DataScopeIgnoreContext.isIgnored()}（@IgnoreDataScope 生效）</li>
 *   <li>{@code DataScopeContext.get() == null}（无 @DataScope 注解，未激活）</li>
 *   <li>当前表在 {@code baseon.data-scope.ignore-tables} 白名单中</li>
 * </ul>
 *
 * <p>visibleIds 语义：null=不限制（不改写）；空集=拼入不可能命中的 -1，查询结果为空。
 *
 * <p>仅处理 SELECT 的 WHERE 注入；INSERT/UPDATE/DELETE 不做改写（与字段级权限边界一致：
 * 行级数据权限主要保护「读」；「写」操作的业务主键本就要求业务侧显式校验）。
 */
public class DataScopeLineInnerInterceptor extends BaseMultiTableInnerInterceptor
        implements DataScopeInnerInterceptor {

    /** 空可见集时的兜底值：约定不存在的 ID，保证查询结果为空 */
    private static final long EMPTY_SCOPE_FALLBACK = -1L;

    private final BaseonDataScopeProperties properties;

    public DataScopeLineInnerInterceptor(BaseonDataScopeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        if (DataScopeIgnoreContext.isIgnored()) {
            return;
        }
        if (DataScopeContext.get() == null) {
            return;
        }
        com.baomidou.mybatisplus.core.toolkit.PluginUtils.MPBoundSql mpBs =
                com.baomidou.mybatisplus.core.toolkit.PluginUtils.mpBoundSql(boundSql);
        mpBs.sql(parserSingle(mpBs.sql(), null));
    }

    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        String whereSegment = (String) obj;
        processSelectBody(select, whereSegment);
        List<WithItem> withItems = select.getWithItemsList();
        if (withItems != null && !withItems.isEmpty()) {
            withItems.forEach(withItem -> processSelectBody(withItem.getSelect(), whereSegment));
        }
    }

    @Override
    protected void processInsert(Insert insert, int index, String sql, Object obj) {
        // 不改写 INSERT：行级权限只保护读
    }

    @Override
    protected void processUpdate(Update update, int index, String sql, Object obj) {
        // 不改写 UPDATE
    }

    @Override
    protected void processDelete(Delete delete, int index, String sql, Object obj) {
        // 不改写 DELETE
    }

    @Override
    public Expression buildTableExpression(Table table, Expression where, String whereSegment) {
        if (DataScopeIgnoreContext.isIgnored()) {
            return null;
        }
        DataScopeContext context = DataScopeContext.get();
        if (context == null) {
            return null;
        }
        if (properties.getIgnoreTables().contains(table.getName())) {
            return null;
        }
        // 注解声明了 tableAlias 时，仅对别名匹配的表注入；未声明则对所有非白名单表注入
        if (context.tableAlias() != null
                && (table.getAlias() == null
                || !context.tableAlias().equals(table.getAlias().getName()))) {
            return null;
        }

        Set<Long> visibleIds = context.visibleIds();
        if (visibleIds == null) {
            return null;
        }

        Column column = buildAliasColumn(table, context.column());
        if (visibleIds.isEmpty()) {
            // 无可见数据：拼入不可能命中的等值条件，结果为空
            return new EqualsTo(column, new LongValue(EMPTY_SCOPE_FALLBACK));
        }
        ParenthesedExpressionList<Expression> idList = new ParenthesedExpressionList<>(
                visibleIds.stream().map(LongValue::new).toArray(Expression[]::new));
        return new InExpression(column, idList);
    }

    /**
     * 带表别名前缀的列引用：有别名时 {@code alias.column}，无别名时裸列名。
     * 与 MP 官方 TenantLineInnerInterceptor#getAliasColumn 行为对齐。
     */
    private Column buildAliasColumn(Table table, String columnName) {
        if (table.getAlias() != null) {
            return new Column(table.getAlias().getName() + "." + columnName);
        }
        return new Column(columnName);
    }
}
