package io.github.ayongbox.baseon.generator.core;

import java.util.Map;
import java.util.Optional;

/**
 * MySQL 列类型到 Java 类型的映射，输出 Java 简单类名（供模板直接使用）。
 *
 * <p>映射口径与脚手架实体规范一致：tinyint(1) 视为布尔位，其余 tinyint 视为整数。
 */
public final class JdbcTypeMapper {

    /** 需要 import 的 Java 类型：简单类名 → 全限定名；未出现的类型（Long/String 等）无需 import */
    private static final Map<String, String> TYPE_IMPORTS = Map.of(
            "BigDecimal", "java.math.BigDecimal",
            "LocalDate", "java.time.LocalDate",
            "LocalDateTime", "java.time.LocalDateTime");

    private JdbcTypeMapper() {
    }

    /**
     * 映射列类型。
     *
     * @param typeName 数据库原始类型名（大小写与 UNSIGNED 后缀不敏感），如 bigint / varchar
     * @param length   列长度，仅 tinyint(1) 判定布尔位时使用，其余类型忽略
     * @return Java 简单类名
     */
    public static String toJavaType(String typeName, int length) {
        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("列类型不能为空");
        }
        String normalized = typeName.toLowerCase()
                .replace(" unsigned", "")
                .trim();
        return switch (normalized) {
            case "bigint" -> "Long";
            case "int", "integer", "smallint", "mediumint" -> "Integer";
            case "tinyint" -> length == 1 ? "Boolean" : "Integer";
            case "varchar", "char", "text", "tinytext", "mediumtext", "longtext", "json" -> "String";
            case "decimal", "numeric" -> "BigDecimal";
            case "datetime", "timestamp" -> "LocalDateTime";
            case "date" -> "LocalDate";
            default -> throw new IllegalArgumentException("暂不支持的列类型: " + typeName + "，请在 JdbcTypeMapper 中补充映射");
        };
    }

    /**
     * Java 简单类名对应的 import 全限定名；无需 import 的类型返回空。
     */
    public static Optional<String> importOf(String javaType) {
        return Optional.ofNullable(TYPE_IMPORTS.get(javaType));
    }
}
