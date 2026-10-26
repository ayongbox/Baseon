package io.github.ayongbox.baseon.core.annotation;

/**
 * 降级策略：远程调用失败后的兜底取值方式。
 */
public enum DegradeStrategy {

    /**
     * 返回该方法最近一次成功结果（本地缓存）。适合读多写少、可容忍短时不一致的数据。
     */
    CACHE,

    /**
     * 返回类型默认值（引用类型 null，数值 0，布尔 false）。适合「查不到就当没有」的场景。
     */
    DEFAULT,

    /**
     * 不兜底，直接抛「服务已降级」业务异常。适合调用方必须感知失败的场景。
     */
    FAIL_FAST
}
