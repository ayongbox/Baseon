package io.github.ayongbox.baseon.core.spi;

import java.util.Set;

/**
 * 数据范围提供者 SPI：starter-data-scope 在执行 SQL 改写前调用，
 * 返回当前登录用户可见的数据 ID 集合，用于拼进行级过滤条件。
 *
 * <p>框架只定义契约，具体规则（谁能看哪些部门/项目/客户）由业务侧实现，
 * 通常走权限中心或组织中心查询。未提供实现时行级过滤不生效（放行），
 * 由业务自行保证安全；若需强制拦截，配置 {@code baseon.data-scope.strict=true}。
 *
 * <p>实现类应保证返回集合不可变；调用方不得修改返回值。
 */
public interface DataScopeProvider {

    /**
     * 当前用户可见的数据 ID 集合（如部门 ID、项目 ID、客户 ID 列表）。
     *
     * @return 可见 ID 集合，不可变；
     *         空集 = 无任何可见数据，查询结果应为空；
     *         {@code null} = 不限制（全量可见，放行）
     */
    Set<Long> visibleIds();

    /**
     * 过滤字段名：行级条件拼到该列上，默认与租户列分离（如 dept_id / project_id）。
     *
     * @return 列名，不得为空
     */
    default String column() {
        return "dept_id";
    }

    /**
     * 当前用户可见的字段集合（字段级过滤用）：返回 null 表示不做字段级过滤，
     * 返回具体集合时序列化阶段仅保留集合内字段（其余置 null）。
     *
     * @return 可见字段名集合；null=不过滤
     */
    default Set<String> visibleFields() {
        return null;
    }

    /** 空实现：不限制数据范围（默认放行），strict 模式下由 starter-data-scope 另行处理 */
    static DataScopeProvider noOp() {
        return new DataScopeProvider() {
            @Override
            public Set<Long> visibleIds() {
                return null;
            }
        };
    }
}
