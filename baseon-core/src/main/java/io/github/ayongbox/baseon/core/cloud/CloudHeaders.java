package io.github.ayongbox.baseon.core.cloud;

/**
 * 跨服务协作 Header 与实例元数据常量，全链路统一收口，避免各 Starter 各自硬编码。
 *
 * <p>常量值固定、不提供配置项：灰度链路涉及「入口解析 → Feign 透传 → 负载均衡过滤」
 * 三个独立 Starter，若允许各自配置会出现同链路名称不一致的隐蔽故障。
 */
public final class CloudHeaders {

    /** 灰度标签请求头：网关/调用方声明本次请求要路由到的实例标签 */
    public static final String GRAY_TAG_HEADER = "X-Gray-Tag";

    /** 实例元数据 key：注册中心实例上标记的灰度标签，与 {@link #GRAY_TAG_HEADER} 匹配 */
    public static final String GRAY_TAG_METADATA = "gray-tag";

    /** 实例元数据 key：标记本实例为本地调试实例，默认不参与其他服务的负载均衡 */
    public static final String LOCAL_METADATA = "baseon.local";

    private CloudHeaders() {
    }
}
