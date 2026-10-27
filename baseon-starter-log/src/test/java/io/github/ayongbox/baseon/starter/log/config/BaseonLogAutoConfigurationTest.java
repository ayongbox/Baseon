package io.github.ayongbox.baseon.starter.log.config;

import io.github.ayongbox.baseon.core.annotation.OperateLog;
import io.github.ayongbox.baseon.starter.log.aspect.OperateLogAspect;
import io.github.ayongbox.baseon.starter.log.support.LoggingOperateLogHandler;
import io.github.ayongbox.baseon.starter.log.support.OperateLogHandler;
import io.github.ayongbox.baseon.starter.log.support.OperateLogRecord;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class BaseonLogAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseonLogAutoConfiguration.class));

    @Test
    void defaultLoggingHandlerIsRegisteredWhenNoCustomHandler() {
        // Given：容器中无自定义 OperateLogHandler
        // When + Then：装配默认 LoggingOperateLogHandler 与切面
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OperateLogHandler.class);
            assertThat(context.getBean(OperateLogHandler.class)).isInstanceOf(LoggingOperateLogHandler.class);
            assertThat(context).hasSingleBean(OperateLogAspect.class);
        });
    }

    @Test
    void customHandlerReplacesDefaultAndReceivesRecord() {
        // Given：业务声明自己的 OperateLogHandler（SPI 替换语义）
        contextRunner.withUserConfiguration(CustomHandlerConfiguration.class)
                .run(context -> {
                    // Then：容器中只有自定义实现，切面拿到的也是它
                    assertThat(context).hasSingleBean(OperateLogHandler.class);
                    assertThat(context.getBean(OperateLogHandler.class))
                            .isInstanceOf(RecordingHandler.class)
                            .isNotInstanceOf(LoggingOperateLogHandler.class);

                    // When：用容器内切面织入代理并调用标注方法
                    AspectJProxyFactory factory = new AspectJProxyFactory(new DemoService());
                    factory.addAspect(context.getBean(OperateLogAspect.class));
                    factory.<DemoService>getProxy().create("张三");

                    // Then：自定义 handler 收到审计记录
                    RecordingHandler recording = (RecordingHandler) context.getBean(OperateLogHandler.class);
                    assertThat(recording.records).hasSize(1);
                    OperateLogRecord record = recording.records.get(0);
                    assertThat(record.getModule()).isEqualTo("user");
                    assertThat(record.isSuccess()).isTrue();
                });
    }

    @Test
    void moduleIsDisabledWhenEnabledSetFalse() {
        // Given：总开关关闭
        // When + Then：handler 与切面都不装配，@OperateLog 不生效
        contextRunner.withPropertyValues("baseon.log.operate.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OperateLogHandler.class);
                    assertThat(context).doesNotHaveBean(OperateLogAspect.class);
                });
    }

    /** 自定义 handler：收集记录到内存，模拟业务写库 / MQ 的替换实现 */
    static class RecordingHandler implements OperateLogHandler {

        private final List<OperateLogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void handle(OperateLogRecord record) {
            records.add(record);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomHandlerConfiguration {

        @Bean
        OperateLogHandler operateLogHandler() {
            return new RecordingHandler();
        }
    }

    /** 演示业务类 */
    static class DemoService {

        @OperateLog(module = "user", action = "创建用户")
        public String create(String name) {
            return "created:" + name;
        }
    }
}
