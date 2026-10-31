package io.github.ayongbox.baseon.starter.datascope.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限配置，前缀 {@code baseon.data-scope}。
 */
@ConfigurationProperties(prefix = "baseon.data-scope")
public class BaseonDataScopeProperties {

    /** 总开关：false 时行级过滤、字段级脱敏、切面均不装配 */
    private boolean enabled = true;

    /** 默认过滤列名（@DataScope 未显式指定 column 时使用） */
    private String column = "dept_id";

    /** 不参与数据范围过滤的表（如字典表、系统配置表） */
    private List<String> ignoreTables = new ArrayList<>();

    /**
     * 严格模式：true 时业务未提供 DataScopeProvider 实现则启动失败，
     * 避免上线后忘记接入权限中心导致行级过滤静默放行；
     * false（默认）时缺实现退化为放行，行为与未引入本 starter 一致。
     */
    private boolean strict = false;

    /** 字段级过滤开关：true 时序列化阶段仅保留 DataScopeProvider#visibleFields 内的字段 */
    private boolean fieldFilterEnabled = false;

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

    public boolean isFieldFilterEnabled() {
        return fieldFilterEnabled;
    }

    public void setFieldFilterEnabled(boolean fieldFilterEnabled) {
        this.fieldFilterEnabled = fieldFilterEnabled;
    }
}
