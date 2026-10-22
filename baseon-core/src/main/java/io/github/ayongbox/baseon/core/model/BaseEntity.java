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

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     *
     * <p>字段定义在内核、切面行为在 starter-mybatis-plus 配置，两者必须成对存在，
     * 因此该字段不允许在各业务实体中重复声明。
     */
    private Integer deleted;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
