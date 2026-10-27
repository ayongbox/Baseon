package io.github.ayongbox.baseon.generator.core;

/**
 * 列元数据：渲染所需的列信息已全部就绪，模板不再做任何命名与类型推断。
 */
public class ColumnMeta {

    private final String columnName;
    private final String propertyName;
    private final String javaType;
    private final String comment;
    private final boolean primaryKey;

    public ColumnMeta(String columnName, String propertyName, String javaType, String comment, boolean primaryKey) {
        this.columnName = columnName;
        this.propertyName = propertyName;
        this.javaType = javaType;
        this.comment = comment;
        this.primaryKey = primaryKey;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getJavaType() {
        return javaType;
    }

    public String getComment() {
        return comment;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }
}
