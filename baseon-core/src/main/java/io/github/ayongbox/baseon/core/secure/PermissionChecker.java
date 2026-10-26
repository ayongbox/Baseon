package io.github.ayongbox.baseon.core.secure;

/**
 * 权限校验契约：判断当前登录上下文是否拥有指定权限串。
 *
 * <p>starter-auth 提供默认实现（比对登录上下文中的权限集合）；
 * 权限模型复杂的业务（角色继承、数据权限、权限中心实时查询）可自行实现覆盖。
 */
public interface PermissionChecker {

    /**
     * 当前请求是否拥有指定权限。未登录视为无任何权限。
     *
     * @param permission 权限标识，如 {@code "user:create"}
     * @return true 表示放行
     */
    boolean hasPermission(String permission);
}
