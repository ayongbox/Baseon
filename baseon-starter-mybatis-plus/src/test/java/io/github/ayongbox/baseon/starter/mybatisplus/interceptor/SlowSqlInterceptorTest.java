package io.github.ayongbox.baseon.starter.mybatisplus.interceptor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlowSqlInterceptorTest {

    private Logger interceptorLogger;
    private ListAppender<ILoggingEvent> logAppender;
    private Map<String, Long> listenerCalls;
    private BiConsumer<String, Long> listener;

    @BeforeEach
    void setUp() {
        listenerCalls = new ConcurrentHashMap<>();
        listener = (statementId, elapsedMs) -> listenerCalls.put(statementId, elapsedMs);
        interceptorLogger = (Logger) LoggerFactory.getLogger(SlowSqlInterceptor.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        interceptorLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        interceptorLogger.detachAppender(logAppender);
        SecurityUtil.clear();
        MDC.clear();
    }

    @Test
    void slowSqlLogsWarnAndNotifiesListener() throws Throwable {
        Invocation invocation = slowInvocation("com.demo.UserMapper.selectById", 120, null);
        SlowSqlInterceptor interceptor = createInterceptor(50, 0, 0, listener);

        Object result = interceptor.intercept(invocation);

        assertThat(result).isEqualTo("ok");
        assertThat(listenerCalls).containsKey("com.demo.UserMapper.selectById");
        assertThat(listenerCalls.get("com.demo.UserMapper.selectById")).isGreaterThanOrEqualTo(50L);
        assertThat(logAppender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("com.demo.UserMapper.selectById");
        });
    }

    @Test
    void fastSqlIsSilent() throws Throwable {
        Invocation invocation = slowInvocation("com.demo.UserMapper.selectAll", 5, null);
        SlowSqlInterceptor interceptor = createInterceptor(500, 0, 0, listener);

        interceptor.intercept(invocation);

        assertThat(listenerCalls).isEmpty();
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void logsWarnWhenNoListener() throws Throwable {
        Invocation invocation = slowInvocation("com.demo.UserMapper.updateById", 120, null);
        SlowSqlInterceptor interceptor = createInterceptor(50, 0, 0, null);

        interceptor.intercept(invocation);

        assertThat(logAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("com.demo.UserMapper.updateById"));
    }

    @Test
    void warmupExecutionsAreNotJudged() throws Throwable {
        SlowSqlInterceptor interceptor = createInterceptor(50, 2, 0, listener);

        // 前 2 次属于预热，即使超阈值也不打日志、不上报
        interceptor.intercept(slowInvocation("com.demo.UserMapper.insert", 120, null));
        interceptor.intercept(slowInvocation("com.demo.UserMapper.insert", 120, null));
        assertThat(logAppender.list).isEmpty();
        assertThat(listenerCalls).isEmpty();

        // 第 3 次开始正常判定
        interceptor.intercept(slowInvocation("com.demo.UserMapper.insert", 120, null));
        assertThat(logAppender.list).hasSize(1);
        assertThat(listenerCalls).containsKey("com.demo.UserMapper.insert");
    }

    @Test
    void logThrottledButMetricsCountedWithinInterval() throws Throwable {
        AtomicInteger metricCalls = new AtomicInteger();
        SlowSqlInterceptor interceptor = createInterceptor(50, 0, 60_000, (id, elapsed) -> metricCalls.incrementAndGet());

        interceptor.intercept(slowInvocation("com.demo.UserMapper.insert", 120, null));
        interceptor.intercept(slowInvocation("com.demo.UserMapper.insert", 120, null));
        interceptor.intercept(slowInvocation("com.demo.UserMapper.insert", 120, null));

        assertThat(logAppender.list).hasSize(1);
        assertThat(metricCalls).hasValue(3);
    }

    @Test
    void parameterSummaryRendersFieldsAndContext() throws Throwable {
        LoginUser user = new LoginUser();
        user.setTenantId(1001L);
        SecurityUtil.set(user);
        MDC.put("traceId", "trace-abc");

        Invocation invocation = slowInvocation("com.demo.TenantUserMapper.insert", 120,
                new DemoEntity(1L, "zhangsan"));
        SlowSqlInterceptor interceptor = createInterceptor(50, 0, 0, null);

        interceptor.intercept(invocation);

        assertThat(logAppender.list).singleElement().satisfies(event -> {
            String message = event.getFormattedMessage();
            assertThat(message).contains("tenantId=1001");
            assertThat(message).contains("traceId=trace-abc");
            assertThat(message).contains("DemoEntity{id=1, name=zhangsan}");
        });
    }

    /**
     * 构造真实 MappedStatement（final 类），Invocation 用 mock 控制执行耗时。
     */
    private SlowSqlInterceptor createInterceptor(long thresholdMs, int warmupExecutions,
                                                 long logIntervalMs, BiConsumer<String, Long> slowSqlListener) {
        return new SlowSqlInterceptor(thresholdMs, warmupExecutions, logIntervalMs, slowSqlListener);
    }

    private Invocation slowInvocation(String statementId, long sleepMs, Object parameter) {
        Configuration configuration = new Configuration();
        MappedStatement mappedStatement = new MappedStatement.Builder(configuration, statementId,
                new StaticSqlSource(configuration, "SELECT 1"), SqlCommandType.SELECT).build();
        Invocation invocation = mock(Invocation.class);
        List<Object> args = new CopyOnWriteArrayList<>();
        args.add(mappedStatement);
        args.add(parameter);
        when(invocation.getArgs()).thenReturn(args.toArray());
        try {
            when(invocation.proceed()).thenAnswer(invocationOnMock -> {
                Thread.sleep(sleepMs);
                return "ok";
            });
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
        return invocation;
    }

    /**
     * 模拟内核实体：不重写 toString，验证摘要走字段渲染而非 Object@hashCode。
     */
    private static final class DemoEntity {

        private final Long id;
        private final String name;

        private DemoEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
