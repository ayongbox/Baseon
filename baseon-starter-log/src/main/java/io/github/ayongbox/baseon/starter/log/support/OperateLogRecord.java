package io.github.ayongbox.baseon.starter.log.support;

/**
 * 一次操作审计的不可变记录，由切面组装后交给 {@link OperateLogHandler} 消费。
 *
 * <p>{@link #getArgs()} 与 {@link #getError()} 允许为 null：
 * 前者在注解 {@code recordArgs=false} 时不采集，后者仅失败路径有值。
 */
public final class OperateLogRecord {

    /** 业务模块名，来自注解 {@code module()} */
    private final String module;

    /** 操作描述，来自注解 {@code action()} */
    private final String action;

    /** 操作人 ID，无登录上下文时为 null */
    private final Long userId;

    /** 操作人账号，无登录上下文时为 null */
    private final String username;

    /** 操作人租户，无登录上下文时为 null */
    private final Long tenantId;

    /** 链路追踪 ID（MDC 中的 traceId），未引入 starter-trace 或非请求线程时为 null */
    private final String traceId;

    /** 方法签名，格式：类简单名.方法名 */
    private final String method;

    /** 入参摘要（已按 argMaxLength 截断），注解 {@code recordArgs=false} 时为 null */
    private final String args;

    /** 方法执行耗时（毫秒） */
    private final long costMs;

    /** 执行结果：true 成功，false 抛异常 */
    private final boolean success;

    /** 异常简要消息（异常类简单名 + message，截断 200），成功时为 null */
    private final String error;

    public OperateLogRecord(String module, String action, Long userId, String username, Long tenantId,
                            String traceId, String method, String args, long costMs,
                            boolean success, String error) {
        this.module = module;
        this.action = action;
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.traceId = traceId;
        this.method = method;
        this.args = args;
        this.costMs = costMs;
        this.success = success;
        this.error = error;
    }

    public String getModule() {
        return module;
    }

    public String getAction() {
        return action;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getMethod() {
        return method;
    }

    public String getArgs() {
        return args;
    }

    public long getCostMs() {
        return costMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}
