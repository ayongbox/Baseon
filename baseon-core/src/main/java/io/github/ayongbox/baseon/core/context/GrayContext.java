package io.github.ayongbox.baseon.core.context;

/**
 * 灰度标签上下文，纯 ThreadLocal，零 Spring 依赖。
 *
 * <p>写入与清理由 starter-loadbalance 的过滤器负责：请求入口解析
 * {@code X-Gray-Tag} 请求头写入，请求结束清理。Feign 调用时由
 * starter-cloud-feign 拦截器读取并透传到下游服务。
 */
public final class GrayContext {

    private static final ThreadLocal<String> TAG = new ThreadLocal<>();

    private GrayContext() {
    }

    /**
     * 读取当前灰度标签，未设置时返回 null。
     */
    public static String get() {
        return TAG.get();
    }

    public static boolean hasTag() {
        String tag = TAG.get();
        return tag != null && !tag.isBlank();
    }

    /**
     * 写入灰度标签；null 或空白等价于 {@link #clear()}。
     */
    public static void set(String tag) {
        if (tag == null || tag.isBlank()) {
            TAG.remove();
        } else {
            TAG.set(tag.trim());
        }
    }

    /**
     * 请求结束必须调用，防止线程池复用时标签串号。
     */
    public static void clear() {
        TAG.remove();
    }
}
