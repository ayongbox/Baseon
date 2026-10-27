package io.github.ayongbox.baseon.generator.core;

import java.util.Collection;
import java.util.List;

/**
 * 表元数据读取入口：core 只定义契约，具体实现（MyBatis-Plus / JDBC）放在 meta 适配层。
 */
public interface TableMetaReader {

    /**
     * 按表名读取元数据。
     *
     * @param tableNames 表名集合，原样传入数据库（不做命名转换）
     * @return 与入参一一对应的表元数据；表不存在时实现方应抛出异常而非静默跳过
     */
    List<TableMeta> readTables(Collection<String> tableNames);
}
