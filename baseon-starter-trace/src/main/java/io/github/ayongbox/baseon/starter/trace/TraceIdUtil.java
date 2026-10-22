package io.github.ayongbox.baseon.starter.trace;

import java.util.UUID;

/**
 * TraceId 存取工具，基于 MDC（Mapped Diagnostic Context）。
 *
 * <p>MDC 是 slf4j 的 ThreadLocal 映射，logback/log4j2 的 pattern 中通过 %X{traceId} 输出。
 * 本类只负责读写，清理由调用方（Filter / 拦截器）在请求结束时执行。
 */
public final class TraceIdUtil {

    /** MDC 中的 key，与 log pattern 中的 %X{traceId} 对应 */
    public static final String TRACE_ID_KEY = "traceId";

    private TraceIdUtil() {
    }

    /**
     * 生成新 TraceId：UUID 去掉横线，32 位小写十六进制。
     */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从 MDC 读取当前 TraceId，不存在时返回 null。
     */
    public static String get() {
        return org.slf4j.MDC.get(TRACE_ID_KEY);
    }

    /**
     * 写入 MDC；value 为 null 或空时生成新 TraceId。
     *
     * @return 实际写入的 TraceId
     */
    public static String set(String value) {
        String traceId = (value == null || value.isBlank()) ? generate() : value;
        org.slf4j.MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }

    /**
     * 请求结束必须调用，防止线程池复用时串号。
     */
    public static void clear() {
        org.slf4j.MDC.remove(TRACE_ID_KEY);
    }
}
