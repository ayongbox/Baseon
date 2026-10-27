package io.github.ayongbox.baseon.demo.dto;

import cn.idev.excel.annotation.ExcelProperty;

import java.time.LocalDateTime;

/**
 * Excel 导入导出行模型：与 TenantUser 实体解耦，避免表结构变更直接影响导入模板。
 * FastExcel 按 getter 取值/按 setter 回填，属性必须有标准 getter/setter。
 */
public class TenantUserExcelRow {

    @ExcelProperty("用户ID")
    private Long id;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
