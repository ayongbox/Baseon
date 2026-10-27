package io.github.ayongbox.baseon.generator.core;

/**
 * 命名转换：数据库蛇形命名与 Java 驼峰命名之间的唯一换算入口，
 * 模板与元数据适配层都必须复用本类，禁止各自实现。
 */
public final class NamingConverter {

    /** 表名业务前缀，生成实体名时剔除 */
    private static final String[] TABLE_PREFIXES = {"t_", "tb_"};

    private NamingConverter() {
    }

    /**
     * 表名转实体名：去掉 t_/tb_ 前缀后转大驼峰，如 t_tenant_user → TenantUser。
     */
    public static String toEntityName(String tableName) {
        String name = tableName;
        for (String prefix : TABLE_PREFIXES) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
                break;
            }
        }
        return toUpperCamel(name);
    }

    /**
     * 列名转属性名：蛇形转小驼峰，如 create_time → createTime。
     */
    public static String toPropertyName(String columnName) {
        return toLowerCamel(columnName);
    }

    /**
     * 实体名转变量名：首字母小写，如 TenantUser → tenantUser。
     */
    public static String toVariableName(String entityName) {
        if (entityName == null || entityName.isEmpty()) {
            throw new IllegalArgumentException("实体名不能为空");
        }
        return Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
    }

    /**
     * 实体名转 REST 路径段：大驼峰转连字符小写并附加复数后缀，如 TenantUser → tenant-users。
     */
    public static String toRequestPath(String entityName) {
        StringBuilder path = new StringBuilder();
        for (char ch : entityName.toCharArray()) {
            if (Character.isUpperCase(ch) && path.length() > 0) {
                path.append('-');
            }
            path.append(Character.toLowerCase(ch));
        }
        return path.append('s').toString();
    }

    private static String toUpperCamel(String snake) {
        String lowerCamel = toLowerCamel(snake);
        return Character.toUpperCase(lowerCamel.charAt(0)) + lowerCamel.substring(1);
    }

    private static String toLowerCamel(String snake) {
        if (snake == null || snake.isBlank()) {
            throw new IllegalArgumentException("蛇形命名不能为空");
        }
        StringBuilder camel = new StringBuilder();
        boolean upperNext = false;
        for (char ch : snake.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
            } else if (upperNext) {
                camel.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                camel.append(Character.toLowerCase(ch));
            }
        }
        return camel.toString();
    }
}
