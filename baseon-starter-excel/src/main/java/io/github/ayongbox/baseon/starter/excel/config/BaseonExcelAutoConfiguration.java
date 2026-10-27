package io.github.ayongbox.baseon.starter.excel.config;

import io.github.ayongbox.baseon.starter.excel.support.ExcelTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Excel 导入导出装配。
 *
 * <p>仅注册一个 {@link ExcelTemplate} Bean，业务注入后调用导出/导入方法；
 * Bean 带 {@link ConditionalOnMissingBean}，业务可整体替换默认实现。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.excel", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonExcelProperties.class)
public class BaseonExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExcelTemplate excelTemplate() {
        return new ExcelTemplate();
    }
}
