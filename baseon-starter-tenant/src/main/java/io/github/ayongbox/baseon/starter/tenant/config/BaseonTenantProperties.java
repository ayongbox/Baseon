package io.github.ayongbox.baseon.starter.tenant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 租户配置，前缀 {@code baseon.tenant}。
 */
@ConfigurationProperties(prefix = "baseon.tenant")
public class BaseonTenantProperties {

    /** 总开关：false 时租户 SQL 过滤与缓存前缀均不装配 */
    private boolean enabled = true;

    /** 租户列名，全库表统一 */
    private String column = "tenant_id";

    /** 不参与租户过滤的表（如字典表、租户表自身） */
    private List<String> ignoreTables = new ArrayList<>();

    /**
     * 严格模式：true 时缺少租户上下文的请求直接抛异常；
     * false（默认）时拼入不可能命中的租户值 -1，查不到数据但接口不报错，
     * 适合登录前接口、系统任务等无登录态场景。
     */
    private boolean strict = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public List<String> getIgnoreTables() {
        return ignoreTables;
    }

    public void setIgnoreTables(List<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}
