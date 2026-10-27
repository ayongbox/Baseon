package io.github.ayongbox.baseon.generator.core;

import java.util.List;
import java.util.Set;

/**
 * 表元数据：一张表对应一个实体，渲染 9 个 CRUD 骨架文件的完整输入。
 */
public class TableMeta {

    /**
     * 由实体父类（BaseEntity / TenantEntity）承载的审计列，生成实体时一律跳过，
     * 不允许在业务实体中重复声明（与 baseon-core 的约定保持一致）。
     */
    private static final Set<String> AUDIT_COLUMNS =
            Set.of("id", "create_time", "update_time", "create_by", "update_by", "deleted", "tenant_id");

    private final String tableName;
    private final String entityName;
    private final String tableComment;
    private final List<ColumnMeta> columns;
    private final boolean tenantTable;

    public TableMeta(String tableName, String entityName, String tableComment,
                     List<ColumnMeta> columns, boolean tenantTable) {
        this.tableName = tableName;
        this.entityName = entityName;
        this.tableComment = tableComment;
        this.columns = List.copyOf(columns);
        this.tenantTable = tenantTable;
    }

    public String getTableName() {
        return tableName;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public List<ColumnMeta> getColumns() {
        return columns;
    }

    /**
     * 是否多租户表（含 tenant_id 列）：决定实体继承 TenantEntity 还是 BaseEntity。
     */
    public boolean isTenantTable() {
        return tenantTable;
    }

    /**
     * 实体需要显式声明的业务字段：剔除父类已承载的审计列。
     */
    public List<ColumnMeta> getEntityColumns() {
        return columns.stream()
                .filter(column -> !AUDIT_COLUMNS.contains(column.getColumnName()))
                .toList();
    }
}
