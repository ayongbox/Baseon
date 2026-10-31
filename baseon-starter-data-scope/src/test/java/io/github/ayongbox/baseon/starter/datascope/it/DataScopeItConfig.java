package io.github.ayongbox.baseon.starter.datascope.it;

import io.github.ayongbox.baseon.core.spi.DataScopeProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 集成测试 Spring 容器配置：用可变 Provider 在不同用例间切换可见 ID 集合，
 * 避免每个用例都重启容器。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "io.github.ayongbox.baseon.starter.datascope.it")
public class DataScopeItConfig {

    /** 当前用例期望的可见 ID 集合；null 表示不限制 */
    static final AtomicReference<Set<Long>> VISIBLE_IDS = new AtomicReference<>(Set.of(10L));

    @Bean
    public DataScopeProvider dataScopeProvider() {
        return new DataScopeProvider() {
            @Override
            public Set<Long> visibleIds() {
                return VISIBLE_IDS.get();
            }

            @Override
            public String column() {
                return "dept_id";
            }
        };
    }
}
