package io.github.ayongbox.baseon.core.model;

import java.time.LocalDateTime;

/**
 * 实体父类：只放全表通用的审计字段，不携带任何 ORM 注解。
 *
 * <p>ORM 映射由 starter-mybatis-plus 通过全局配置与命名约定完成，
 * 内核不引入 MyBatis-Plus，避免持久层技术绑死在基础模型上。
 */
public abstract class BaseEntity {

    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }
}
