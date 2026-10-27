package io.github.ayongbox.baseon.generator.meta;

import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.TemplateConfig;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import io.github.ayongbox.baseon.generator.core.ColumnMeta;
import io.github.ayongbox.baseon.generator.core.JdbcTypeMapper;
import io.github.ayongbox.baseon.generator.core.NamingConverter;
import io.github.ayongbox.baseon.generator.core.TableMeta;
import io.github.ayongbox.baseon.generator.core.TableMetaReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于 MyBatis-Plus 生成器元数据体系的表元数据读取实现。
 *
 * <p>只借用 MP 的 JDBC 元数据装配能力（ConfigBuilder / TableInfo / TableField），
 * 命名与类型映射全部回到 core 完成，保证口径唯一、可单测。
 */
public class MybatisPlusTableMetaReader implements TableMetaReader {

    private final DataSourceConfig dataSourceConfig;

    public MybatisPlusTableMetaReader(String dbUrl, String dbUser, String dbPassword) {
        this.dataSourceConfig = new DataSourceConfig.Builder(dbUrl, dbUser, dbPassword).build();
    }

    @Override
    public List<TableMeta> readTables(Collection<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            throw new IllegalArgumentException("表名集合不能为空");
        }
        StrategyConfig strategyConfig = new StrategyConfig.Builder()
                .addInclude(List.copyOf(tableNames))
                .build();
        ConfigBuilder configBuilder = new ConfigBuilder(
                new PackageConfig.Builder().build(),
                dataSourceConfig,
                strategyConfig,
                new TemplateConfig.Builder().build(),
                new GlobalConfig.Builder().build(),
                new InjectionConfig.Builder().build());

        List<TableMeta> tables = new ArrayList<>();
        Set<String> found = new HashSet<>();
        for (TableInfo tableInfo : configBuilder.getTableInfoList()) {
            tables.add(toTableMeta(tableInfo));
            found.add(tableInfo.getName());
        }
        for (String tableName : tableNames) {
            if (!found.contains(tableName)) {
                throw new IllegalStateException("表不存在或无读取权限: " + tableName);
            }
        }
        return tables;
    }

    /**
     * MP 表信息转 core 模型：原始表名/列名交给 core 做命名与类型映射。
     */
    private TableMeta toTableMeta(TableInfo tableInfo) {
        List<ColumnMeta> columns = new ArrayList<>();
        boolean tenantTable = false;
        for (TableField field : tableInfo.getFields()) {
            String columnName = field.getColumnName();
            if ("tenant_id".equals(columnName)) {
                tenantTable = true;
            }
            columns.add(new ColumnMeta(
                    columnName,
                    NamingConverter.toPropertyName(columnName),
                    resolveJavaType(field),
                    field.getComment(),
                    field.isKeyFlag()));
        }
        return new TableMeta(
                tableInfo.getName(),
                NamingConverter.toEntityName(tableInfo.getName()),
                tableInfo.getComment(),
                columns,
                tenantTable);
    }

    /**
     * 列 Java 类型：优先按 JDBC 原始类型名 + 长度走 core 映射（tinyint(1) → Boolean 依赖长度）；
     * 驱动不回填 MetaInfo 时兜底使用 MP 已推断的 Java 类型名。
     */
    private String resolveJavaType(TableField field) {
        TableField.MetaInfo metaInfo = field.getMetaInfo();
        if (metaInfo != null && metaInfo.getTypeName() != null && !metaInfo.getTypeName().isBlank()) {
            return JdbcTypeMapper.toJavaType(metaInfo.getTypeName(), metaInfo.getLength());
        }
        return field.getPropertyType();
    }
}
