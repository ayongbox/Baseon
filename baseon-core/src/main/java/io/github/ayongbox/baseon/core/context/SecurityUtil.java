package io.github.ayongbox.baseon.core.context;

import io.github.ayongbox.baseon.core.model.LoginUser;

/**
 * 登录上下文存取工具，纯 ThreadLocal，零 Spring 依赖。
 *
 * <p>写入与清理由 starter-web 的拦截器负责，业务只读。
 * 异步场景必须使用 {@link ContextWrapper} 包装任务，否则子线程拿不到上下文。
 */
public final class SecurityUtil {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    private SecurityUtil() {
    }

    public static LoginUser get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        LoginUser user = get();
        return user == null ? null : user.getUserId();
    }

    public static Long getTenantId() {
        LoginUser user = get();
        return user == null ? null : user.getTenantId();
    }

    public static String getUsername() {
        LoginUser user = get();
        return user == null ? null : user.getUsername();
    }

    public static boolean isLogin() {
        return get() != null;
    }

    public static void set(LoginUser user) {
        if (user == null) {
            CONTEXT.remove();
        } else {
            CONTEXT.set(user);
        }
    }

    /**
     * 请求结束必须调用，否则在线程池复用场景会造成上下文串号。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
