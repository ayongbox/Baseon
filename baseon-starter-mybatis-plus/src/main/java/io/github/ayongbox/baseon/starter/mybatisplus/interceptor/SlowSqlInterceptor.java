package io.github.ayongbox.baseon.starter.mybatisplus.interceptor;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * 慢 SQL 拦截器：对 Executor 的 update / query 计时，
 * 耗时达到阈值即打 WARN 日志，并在 Micrometer 可用时上报计数指标。
 *
 * <p><b>计时口径</b>：包住整个 {@code invocation.proceed()}，因此测得的是
 * 「端到端 JDBC 调用耗时」——含连接获取、参数映射、结果集处理与冷启动开销，
 * 不等于数据库服务端的 SQL 执行时间。权威的慢 SQL 判定应看 MySQL 慢日志 /
 * {@code performance_schema} digest 聚合；本拦截器的价值是补上服务端监控拿不到的
 * <b>归因信息</b>：具体 statementId、traceId、租户与参数摘要。
 *
 * <p>为配合这个口径做了三项降噪：
 * <ul>
 *   <li>预热跳过：每个 statementId 的前 N 次执行不判定，消除建连/类加载/JIT 误报；</li>
 *   <li>日志限流：同一 statementId 的 WARN 按时间窗口收敛，指标仍逐次计数；</li>
 *   <li>参数摘要：实体按字段值渲染，避免 {@code Object@hashCode} 无信息量输出。</li>
 * </ul>
 *
 * <p>之所以用 MyBatis 原生 {@link Interceptor} 而不是 MP 的 {@code InnerInterceptor}：
 * InnerInterceptor 只有 SQL 解析前后（beforeQuery / beforePrepare）的切面时机，
 * 拿不到{@code proceed} 前后的完整执行耗时；原生插件包住整个调用，天然带计时区间。
 *
 * <p>同时拦截 query 的两个重载不会重复计数：4 参 query 内部通过 this 调用 6 参 query，
 * 不经过插件代理，每条 SQL 只会被最外层入口计时一次。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        CacheKey.class, BoundSql.class})
})
public class SlowSqlInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SlowSqlInterceptor.class);

    /**
     * 与 starter-trace 的 {@code TraceIdUtil.TRACE_ID_KEY} 保持一致；
     * 本模块不依赖 starter-trace，故直接读 MDC 而不引用其常量。
     */
    private static final String TRACE_ID_MDC_KEY = "traceId";

    /** 无上下文时的占位，避免日志出现 "null" 难以区分 */
    private static final String EMPTY_CONTEXT = "-";

    /**
     * 参数摘要最大长度，防止大对象 toString 撑爆日志。
     */
    private static final int PARAMETER_SUMMARY_MAX_LENGTH = 200;

    /** 参数摘要最多渲染的字段/条目数，避免批量参数刷屏 */
    private static final int PARAMETER_ITEM_MAX_COUNT = 10;

    /** 单个参数值的最大长度 */
    private static final int PARAMETER_VALUE_MAX_LENGTH = 40;

    private final long slowSqlThresholdMs;

    /** 每个 statementId 跳过的预热执行次数 */
    private final int warmupExecutions;

    /** 同一 statementId 的 WARN 最小间隔（毫秒），<=0 表示不限流 */
    private final long logIntervalMs;

    /**
     * 慢 SQL 回调（statementId, elapsedMs），用于对接指标系统。
     * 不直接依赖 Micrometer：消费方未引入 micrometer 时本类也必须可用，
     * 指标装配见 BaseonMybatisPlusAutoConfiguration 的 @ConditionalOnClass 守卫。
     */
    private final BiConsumer<String, Long> slowSqlListener;

    /** statementId → 已执行次数，用于识别预热期 */
    private final ConcurrentMap<String, AtomicInteger> executionCounters = new ConcurrentHashMap<>();

    /** statementId → 上次打印 WARN 的时间戳 */
    private final ConcurrentMap<String, Long> lastLogTimestamps = new ConcurrentHashMap<>();

    public SlowSqlInterceptor(long slowSqlThresholdMs, int warmupExecutions, long logIntervalMs,
                              BiConsumer<String, Long> slowSqlListener) {
        this.slowSqlThresholdMs = slowSqlThresholdMs;
        this.warmupExecutions = Math.max(warmupExecutions, 0);
        this.logIntervalMs = logIntervalMs;
        this.slowSqlListener = slowSqlListener;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startNanos = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            String statementId = ((MappedStatement) invocation.getArgs()[0]).getId();
            // 预热计数必须每次执行都累加（快 SQL 也计入），否则预热期永远走不完
            if (!isWarmupExecution(statementId) && elapsedMs >= slowSqlThresholdMs) {
                reportSlowSql(invocation, statementId, elapsedMs);
            }
        }
    }

    private boolean isWarmupExecution(String statementId) {
        int executionNo = executionCounters
                .computeIfAbsent(statementId, key -> new AtomicInteger())
                .incrementAndGet();
        return executionNo <= warmupExecutions;
    }

    /**
     * 慢 SQL 上报：指标逐次计数，WARN 日志受限流约束。
     */
    private void reportSlowSql(Invocation invocation, String statementId, long elapsedMs) {
        if (slowSqlListener != null) {
            slowSqlListener.accept(statementId, elapsedMs);
        }
        if (!shouldLog(statementId)) {
            return;
        }
        log.warn("[baseon] 慢 SQL：statement={}，耗时={}ms，阈值={}ms，tenantId={}，traceId={}，参数摘要={}",
                statementId, elapsedMs, slowSqlThresholdMs,
                currentTenantId(), currentTraceId(), summarizeParameter(invocation.getArgs()[1]));
    }

    private boolean shouldLog(String statementId) {
        if (logIntervalMs <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long lastLogAt = lastLogTimestamps.get(statementId);
        if (lastLogAt != null && now - lastLogAt < logIntervalMs) {
            return false;
        }
        // 并发下允许极小概率重复打印，限流只用于降噪，无需 CAS 强一致
        lastLogTimestamps.put(statementId, now);
        return true;
    }

    private String currentTenantId() {
        Long tenantId = SecurityUtil.getTenantId();
        return tenantId == null ? EMPTY_CONTEXT : tenantId.toString();
    }

    private String currentTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        return traceId == null || traceId.isBlank() ? EMPTY_CONTEXT : traceId;
    }

    private String summarizeParameter(Object parameter) {
        if (parameter == null) {
            return "null";
        }
        String text = describe(parameter);
        if (text.length() <= PARAMETER_SUMMARY_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, PARAMETER_SUMMARY_MAX_LENGTH) + "...";
    }

    private String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (isPlainValue(value)) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            return describeMap(map);
        }
        if (value instanceof Collection<?> collection) {
            return "collection(size=" + collection.size() + ")";
        }
        if (value.getClass().isArray()) {
            return "array(length=" + Array.getLength(value) + ")";
        }
        return describeFields(value);
    }

    /**
     * 单值类型直接输出；MyBatis 的 {@code MapperMethod.ParamMap} 走 Map 分支，
     * 可以把 {@code name=zhangsan} 这类真实参数打印出来。
     */
    private boolean isPlainValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof Temporal;
    }

    private String describeMap(Map<?, ?> map) {
        StringBuilder summary = new StringBuilder("{");
        int written = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (written >= PARAMETER_ITEM_MAX_COUNT) {
                summary.append(", ...");
                break;
            }
            if (written++ > 0) {
                summary.append(", ");
            }
            summary.append(entry.getKey()).append('=').append(describeBounded(entry.getValue()));
        }
        return summary.append('}').toString();
    }

    /**
     * 实体按字段值渲染：内核实体刻意不带 MP 注解、也常不重写 toString，
     * 直接 {@code String.valueOf} 只会得到 {@code TenantUser@29064fe3}，无法排查。
     * 反射失败（模块访问受限等）时跳过该字段，最差退化为类名。
     */
    private String describeFields(Object value) {
        StringBuilder summary = new StringBuilder(value.getClass().getSimpleName()).append('{');
        int written = 0;
        for (Class<?> type = value.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (written >= PARAMETER_ITEM_MAX_COUNT) {
                    summary.append(", ...");
                    return summary.append('}').toString();
                }
                Object fieldValue;
                try {
                    field.setAccessible(true);
                    fieldValue = field.get(value);
                } catch (ReflectiveOperationException | RuntimeException inaccessible) {
                    continue;
                }
                if (written++ > 0) {
                    summary.append(", ");
                }
                summary.append(field.getName()).append('=').append(describeBounded(fieldValue));
            }
        }
        return summary.append('}').toString();
    }

    private String describeBounded(Object value) {
        if (value == null) {
            return "null";
        }
        if (isPlainValue(value)) {
            return abbreviate(String.valueOf(value));
        }
        return value.getClass().getSimpleName();
    }

    private String abbreviate(String text) {
        if (text.length() <= PARAMETER_VALUE_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, PARAMETER_VALUE_MAX_LENGTH) + "...";
    }
}
