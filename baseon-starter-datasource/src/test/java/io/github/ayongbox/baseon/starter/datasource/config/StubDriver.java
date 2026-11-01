package io.github.ayongbox.baseon.starter.datasource.config;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 测试用空驱动：Hikari {@code setDriverClassName} 要求类可加载且可无参实例化，
 * 单测只验证装配逻辑、不建立真实连接，因此全部方法返回兜底值。
 */
public class StubDriver implements Driver {

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        throw new SQLException("stub driver does not connect");
    }

    @Override
    public boolean acceptsURL(String url) {
        return false;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }
}
