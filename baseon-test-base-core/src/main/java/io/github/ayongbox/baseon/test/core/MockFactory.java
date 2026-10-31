package io.github.ayongbox.baseon.test.core;

import io.github.ayongbox.baseon.core.model.LoginUser;

/**
 * 常用测试对象工厂：减少各测试重复构造 {@link LoginUser} 等上下文对象。
 */
public final class MockFactory {

    private MockFactory() {
    }

    /**
     * 构造一个带默认租户与用户 ID 的 {@link LoginUser}。
     */
    public static LoginUser loginUser() {
        return loginUser(TestConstants.DEFAULT_TENANT_ID, TestConstants.DEFAULT_USER_ID);
    }

    /**
     * 构造指定租户与用户 ID 的 {@link LoginUser}。
     */
    public static LoginUser loginUser(long tenantId, long userId) {
        LoginUser user = new LoginUser();
        user.setTenantId(tenantId);
        user.setUserId(userId);
        user.setUsername(TestConstants.DEFAULT_USERNAME);
        return user;
    }

    /**
     * 构造指定租户与用户 ID 的 {@link LoginUser} 并写入 {@code SecurityUtil}，
     * 返回构造好的对象便于后续断言。
     */
    public static LoginUser loginUserInContext(long tenantId, long userId) {
        LoginUser user = loginUser(tenantId, userId);
        io.github.ayongbox.baseon.core.context.SecurityUtil.set(user);
        return user;
    }
}
