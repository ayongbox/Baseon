package io.github.ayongbox.baseon.starter.seata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式事务配置，前缀 {@code baseon.seata}。
 *
 * <p>本 starter 是 Seata 的可选封装：统一开关与模式在这里声明，
 * 启动时由 {@link BaseonSeataEnvironmentPostProcessor} 映射为 Seata 原生
 * {@code seata.*} 属性（显式配置的 baseon 键优先），业务无需再记两套前缀。
 */
@ConfigurationProperties(prefix = "baseon.seata")
public class BaseonSeataProperties {

    /**
     * 分布式事务总开关（默认开启，引入即生效）。
     * 关闭时同时把 {@code seata.enabled} 置为 false，Seata 自身装配整体不生效。
     */
    private boolean enabled = true;

    /**
     * 事务模式：AT 依赖数据源自动代理；TCC 不代理数据源，由业务实现两阶段接口。
     */
    private Mode mode = Mode.AT;

    /**
     * 事务分组名，映射 {@code seata.tx-service-group}，须与 TC 侧集群配置一致。
     */
    private String txServiceGroup = "baseon-tx-group";

    /**
     * 应用标识，映射 {@code seata.application-id}；为空时沿用 Seata 的默认取值。
     */
    private String applicationId;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * 事务模式。
     */
    public enum Mode {

        /**
         * 自动事务：Seata 自动代理数据源，业务只需 {@code @GlobalTransactional}。
         */
        AT,

        /**
         * 两阶段提交：不代理数据源，业务实现 {@code @TwoPhaseBusinessAction} 的 try/confirm/cancel。
         */
        TCC
    }
}
