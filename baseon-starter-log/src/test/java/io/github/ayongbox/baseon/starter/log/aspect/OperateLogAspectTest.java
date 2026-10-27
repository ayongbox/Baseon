package io.github.ayongbox.baseon.starter.log.aspect;

import io.github.ayongbox.baseon.core.annotation.OperateLog;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.starter.log.config.BaseonLogProperties;
import io.github.ayongbox.baseon.starter.log.support.OperateLogHandler;
import io.github.ayongbox.baseon.starter.log.support.OperateLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OperateLogAspectTest {

    private final OperateLogHandler handler = mock(OperateLogHandler.class);
    private final BaseonLogProperties properties = new BaseonLogProperties();
    private final ArgumentCaptor<OperateLogRecord> captor = ArgumentCaptor.forClass(OperateLogRecord.class);

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
        MDC.clear();
    }

    @Test
    void successInvocationRecordsSuccessWithContextFields() {
        // Given：登录上下文与 traceId 就位，代理织入切面
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setTenantId(1001L);
        SecurityUtil.set(user);
        MDC.put("traceId", "t-abc");
        DemoService proxy = proxyOf(new DemoService());

        // When：调用成功路径
        String result = proxy.create("张三");

        // Then：业务结果原样返回，记录 success 且全字段采集到位
        assertThat(result).isEqualTo("created:张三");
        verify(handler).handle(captor.capture());
        OperateLogRecord record = captor.getValue();
        assertThat(record.isSuccess()).isTrue();
        assertThat(record.getModule()).isEqualTo("user");
        assertThat(record.getAction()).isEqualTo("创建用户");
        assertThat(record.getUserId()).isEqualTo(1L);
        assertThat(record.getUsername()).isEqualTo("admin");
        assertThat(record.getTenantId()).isEqualTo(1001L);
        assertThat(record.getTraceId()).isEqualTo("t-abc");
        assertThat(record.getMethod()).isEqualTo("DemoService.create");
        assertThat(record.getArgs()).isEqualTo("[张三]");
        assertThat(record.getError()).isNull();
        assertThat(record.getCostMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void failedInvocationRecordsFailedAndRethrowsOriginalException() {
        // Given：业务方法抛出预置异常
        DemoService proxy = proxyOf(new DemoService());

        // When + Then：异常实例原样上抛，切面不吞不换
        assertThatThrownBy(proxy::failing).isSameAs(DemoService.FAILURE);

        // Then：记录 failed + 异常简要消息（类简单名 + message）
        verify(handler).handle(captor.capture());
        OperateLogRecord record = captor.getValue();
        assertThat(record.isSuccess()).isFalse();
        assertThat(record.getError()).isEqualTo("IllegalStateException: boom");
        assertThat(record.getCostMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void recordArgsFalseSkipsArgsSummary() {
        // Given：注解显式关闭入参采集（敏感入参场景）
        DemoService proxy = proxyOf(new DemoService());

        // When：调用关闭入参采集的方法
        proxy.resetPassword("secret-plain");

        // Then：args 为 null，其余字段正常采集
        verify(handler).handle(captor.capture());
        OperateLogRecord record = captor.getValue();
        assertThat(record.isSuccess()).isTrue();
        assertThat(record.getArgs()).isNull();
        assertThat(record.getMethod()).isEqualTo("DemoService.resetPassword");
    }

    @Test
    void argsSummaryIsTruncatedAtArgMaxLength() {
        // Given：argMaxLength 收紧到 10，入参序列化后超长
        properties.setArgMaxLength(10);
        DemoService proxy = proxyOf(new DemoService());

        // When：调用成功路径
        proxy.create("一二三四五六七八九十一二三四五");

        // Then：入参摘要截断到 10 字符并追加省略号
        verify(handler).handle(captor.capture());
        assertThat(captor.getValue().getArgs()).isEqualTo("[一二三四五六七八九...");
    }

    /** 手工织入切面：每次新建以读取最新 properties（替代完整 Spring 容器，聚焦切面行为） */
    private DemoService proxyOf(DemoService target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new OperateLogAspect(handler, properties));
        return factory.getProxy();
    }

    /** 演示业务类：覆盖成功 / 失败 / 关闭入参采集三种注解形态 */
    static class DemoService {

        /** 预置异常：用于校验「原样上抛」是同一实例而非包装 */
        static final IllegalStateException FAILURE = new IllegalStateException("boom");

        @OperateLog(module = "user", action = "创建用户")
        public String create(String name) {
            return "created:" + name;
        }

        @OperateLog(module = "user", action = "删除用户")
        public String failing() {
            throw FAILURE;
        }

        @OperateLog(module = "user", action = "重置口令", recordArgs = false)
        public String resetPassword(String plainPassword) {
            return "reset";
        }
    }
}
