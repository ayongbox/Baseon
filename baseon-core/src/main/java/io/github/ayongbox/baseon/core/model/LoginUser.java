package io.github.ayongbox.baseon.core.model;

import java.util.Set;

/**
 * 登录用户上下文模型，由 starter-web 的拦截器解析请求后填充。
 *
 * <p>业务代码只能通过 core.context 的 SecurityUtil 读取，禁止自行解析 Header。
 */
public class LoginUser {

    private Long userId;
    private String username;
    private Long tenantId;
    private Integer userType;

    /** 权限标识集合（如 {@code "user:create"}），由 starter-auth 校验 Token 时填充 */
    private Set<String> permissions;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getUserType() {
        return userType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
