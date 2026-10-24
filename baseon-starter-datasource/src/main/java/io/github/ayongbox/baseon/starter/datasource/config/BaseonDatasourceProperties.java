package io.github.ayongbox.baseon.starter.datasource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态数据源配置，前缀 {@code baseon.datasource}。
 *
 * <p>主库 {@link #master} 必填（至少配置 url），从库 {@link #slaves} 可空；
 * 所有数据源共用一套 {@link #hikari} 连接池参数，不支持单池覆盖。
 */
@ConfigurationProperties(prefix = "baseon.datasource")
public class BaseonDatasourceProperties {

    /**
     * 动态数据源总开关。关闭后本 starter 不装配任何 Bean，
     * 数据源回退给 Spring Boot 默认的 DataSource 自动装配。
     */
    private boolean enabled = true;

    /**
     * 主库配置，承担所有写操作与未命中路由规则的读操作。
     */
    private DataSourceConfig master = new DataSourceConfig();

    /**
     * 从库配置列表，按声明顺序命名 slave1、slave2……，只读事务按轮询分摊。
     */
    private List<DataSourceConfig> slaves = new ArrayList<>();

    /**
     * 读写分离开关。开启后只读事务（@Transactional(readOnly = true)）
     * 自动轮询路由到从库；没有配置从库时仍回主库。
     */
    private boolean readWriteSplitEnabled = true;

    /**
     * 全部数据源共用的 Hikari 连接池参数。
     */
    private HikariConfig hikari = new HikariConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DataSourceConfig getMaster() {
        return master;
    }

    public void setMaster(DataSourceConfig master) {
        this.master = master;
    }

    public List<DataSourceConfig> getSlaves() {
        return slaves;
    }

    public void setSlaves(List<DataSourceConfig> slaves) {
        this.slaves = slaves;
    }

    public boolean isReadWriteSplitEnabled() {
        return readWriteSplitEnabled;
    }

    public void setReadWriteSplitEnabled(boolean readWriteSplitEnabled) {
        this.readWriteSplitEnabled = readWriteSplitEnabled;
    }

    public HikariConfig getHikari() {
        return hikari;
    }

    public void setHikari(HikariConfig hikari) {
        this.hikari = hikari;
    }

    /**
     * 单个数据源的连接配置。
     */
    public static class DataSourceConfig {

        /**
         * JDBC 连接串，主库必填、从库必填。
         */
        private String url;

        /**
         * 数据库用户名。
         */
        private String username;

        /**
         * 数据库密码。
         */
        private String password;

        /**
         * JDBC 驱动类名，默认 MySQL 8 驱动。
         */
        private String driverClassName = "com.mysql.cj.jdbc.Driver";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }

    /**
     * Hikari 生产默认参数，全部数据源共用。
     */
    public static class HikariConfig {

        /**
         * 单池最大连接数。
         */
        private int maximumPoolSize = 20;

        /**
         * 单池最小空闲连接数。
         */
        private int minimumIdle = 5;

        /**
         * 获取连接的最长等待时间（毫秒），快速失败避免拖垮业务线程。
         */
        private long connectionTimeout = 3000;

        /**
         * 连接最长存活时间（毫秒），须小于数据库 wait_timeout。
         */
        private long maxLifetime = 1800000;

        /**
         * 空闲连接最长保留时间（毫秒）。
         */
        private long idleTimeout = 600000;

        /**
         * 连接探活 SQL。
         */
        private String connectionTestQuery = "SELECT 1";

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getMaxLifetime() {
            return maxLifetime;
        }

        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }

        public long getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public String getConnectionTestQuery() {
            return connectionTestQuery;
        }

        public void setConnectionTestQuery(String connectionTestQuery) {
            this.connectionTestQuery = connectionTestQuery;
        }
    }
}
