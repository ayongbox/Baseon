package io.github.ayongbox.baseon.core.model;

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
}
