package io.github.ayongbox.baseon.test.integration.it;

import io.github.ayongbox.baseon.test.integration.H2IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link H2IntegrationTest} 能在零外部依赖下加载 Spring 上下文。
 */
class H2IntegrationTestTest extends H2IntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithH2() {
        assertThat(context).isNotNull();
        assertThat(context.getBean(TestApplication.class)).isNotNull();
    }
}
