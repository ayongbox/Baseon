-- 本地演示：demo-service 用同实例两个 schema 模拟主从（baseon_master 已由 MYSQL_DATABASE 创建）
CREATE DATABASE IF NOT EXISTS `baseon_slave`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
