package io.github.ayongbox.baseon.starter.auth.support;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.secure.PermissionChecker;

/**
 * {@link PermissionChecker} 默认实现：比对当前登录用户 Token 中携带的权限集合。
 *
 * <p>权限模型复杂的业务（角色继承、数据权限、权限中心实时查询）可自行实现
 * {@link PermissionChecker} 覆盖本默认 Bean。
 */
public class DefaultPermissionChecker implements PermissionChecker {

    @Override
    public boolean hasPermission(String permission) {
        LoginUser user = SecurityUtil.get();
        return user != null
                && user.getPermissions() != null
                && user.getPermissions().contains(permission);
    }
}
