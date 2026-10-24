package io.github.ayongbox.baseon.starter.mybatisplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis-Plus 增强能力配置，前缀 {@code baseon.mybatis-plus}。
 *
 * <p>注意：mapperLocations、类型别名等 MP 原生配置仍走 {@code mybatis-plus.*}，
 * 本类只承载脚手架增强能力，避免与 MP 原生配置重复。
 */
@ConfigurationProperties(prefix = "baseon.mybatis-plus")
public class BaseonMybatisPlusProperties {

    /**
     * 慢 SQL 告警总开关。数据库侧已有完善慢日志（MySQL slow log / 云厂商 DAS 等）时，
     * 可关闭应用侧输出以消除日志噪音。
     */
    private boolean slowSqlEnabled = true;

    /**
     * 慢 SQL 阈值（毫秒）。注意计时口径是<b>端到端 Executor 调用耗时</b>：
     * 含连接获取、参数映射与结果集处理，不等于数据库服务端执行时间，
     * 因此默认值比 MySQL 慢日志的 1s 略宽。
     */
    private long slowSqlThresholdMs = 1000;

    /**
     * 每个 statementId 的预热执行次数：前 N 次不参与慢 SQL 判定。
     * 用于跳过首次建连、驱动/反射类加载、JIT 等一次性成本造成的误报。
     */
    private int slowSqlWarmupExecutions = 3;

    /**
     * 同一 statementId 的 WARN 日志最小间隔（毫秒），0 表示不限流。
     * 仅限制日志条数，Micrometer 指标仍逐次计数。
     */
    private long slowSqlLogIntervalMs = 60_000;

    public boolean isSlowSqlEnabled() {
        return slowSqlEnabled;
    }

    public void setSlowSqlEnabled(boolean slowSqlEnabled) {
        this.slowSqlEnabled = slowSqlEnabled;
    }

    public long getSlowSqlThresholdMs() {
        return slowSqlThresholdMs;
    }

    public void setSlowSqlThresholdMs(long slowSqlThresholdMs) {
        this.slowSqlThresholdMs = slowSqlThresholdMs;
    }

    public int getSlowSqlWarmupExecutions() {
        return slowSqlWarmupExecutions;
    }

    public void setSlowSqlWarmupExecutions(int slowSqlWarmupExecutions) {
        this.slowSqlWarmupExecutions = slowSqlWarmupExecutions;
    }

    public long getSlowSqlLogIntervalMs() {
        return slowSqlLogIntervalMs;
    }

    public void setSlowSqlLogIntervalMs(long slowSqlLogIntervalMs) {
        this.slowSqlLogIntervalMs = slowSqlLogIntervalMs;
    }
}
