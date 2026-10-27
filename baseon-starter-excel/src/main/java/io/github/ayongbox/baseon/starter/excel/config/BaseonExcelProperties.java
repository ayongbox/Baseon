package io.github.ayongbox.baseon.starter.excel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Excel 导入导出配置，前缀 {@code baseon.excel}。
 */
@ConfigurationProperties(prefix = "baseon.excel")
public class BaseonExcelProperties {

    /**
     * 总开关：false 时不注册 {@code ExcelTemplate} Bean。默认 true。
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
