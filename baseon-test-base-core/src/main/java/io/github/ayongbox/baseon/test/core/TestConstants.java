package io.github.ayongbox.baseon.test.core;

/**
 * 测试通用常量：避免各测试硬编码租户/用户 ID。
 */
public final class TestConstants {

    /** 默认租户 ID */
    public static final long DEFAULT_TENANT_ID = 1001L;

    /** 默认用户 ID */
    public static final long DEFAULT_USER_ID = 1L;

    /** 默认用户名 */
    public static final String DEFAULT_USERNAME = "tester";

    /** 默认 traceId，仅用于日志断言 */
    public static final String DEFAULT_TRACE_ID = "test-trace-id-0000000000000000";

    /** 默认灰度标签 */
    public static final String DEFAULT_GRAY_TAG = "test-gray";

    private TestConstants() {
    }
}
