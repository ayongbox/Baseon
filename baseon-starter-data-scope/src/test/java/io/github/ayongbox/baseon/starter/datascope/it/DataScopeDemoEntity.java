package io.github.ayongbox.baseon.starter.datascope.it;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 集成测试实体：与 schema.sql 中 t_data_scope_demo 表对应。
 */
@TableName("t_data_scope_demo")
public class DataScopeDemoEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deptId;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
