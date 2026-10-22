package io.github.ayongbox.baseon.core.model;

/**
 * 多租户实体父类：仅用于需要租户隔离的表。
 *
 * <p>不需要多租户的服务直接继承 {@link BaseEntity}，不要为了统一而全部带上 tenantId。
 */
public abstract class TenantEntity extends BaseEntity {

    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
