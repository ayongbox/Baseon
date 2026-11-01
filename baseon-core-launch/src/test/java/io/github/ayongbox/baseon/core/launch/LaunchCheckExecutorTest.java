package io.github.ayongbox.baseon.core.launch;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchCheckExecutorTest {

    private final MockEnvironment environment = new MockEnvironment();

    @Test
    void should_pass_when_all_checks_succeed() {
        AtomicInteger invoked = new AtomicInteger();
        LaunchCheckExecutor executor = new LaunchCheckExecutor(List.of(
                env -> invoked.incrementAndGet(),
                env -> invoked.incrementAndGet()), true, environment);
        assertDoesNotThrow(executor::execute);
        assertEquals(2, invoked.get());
    }

    @Test
    void should_throw_with_failure_summary_when_fail_fast() {
        LaunchCheck failing = new LaunchCheck() {
            @Override
            public String name() {
                return "datasource-check";
            }

            @Override
            public void check(org.springframework.core.env.Environment env) {
                throw new IllegalStateException("缺少必选配置：spring.datasource.url");
            }
        };
        LaunchCheckExecutor executor = new LaunchCheckExecutor(List.of(failing), true, environment);
        LaunchCheckException ex = assertThrows(LaunchCheckException.class, executor::execute);
        assertTrue(ex.getMessage().contains("datasource-check"));
        assertTrue(ex.getMessage().contains("spring.datasource.url"));
    }

    @Test
    void should_degrade_to_warn_when_not_fail_fast() {
        LaunchCheck failing = env -> {
            throw new IllegalStateException("downstream unreachable");
        };
        LaunchCheckExecutor executor = new LaunchCheckExecutor(List.of(failing), false, environment);
        assertDoesNotThrow(executor::execute);
    }

    @Test
    void should_continue_remaining_checks_after_failure() {
        AtomicInteger invoked = new AtomicInteger();
        LaunchCheck failing = env -> {
            throw new IllegalStateException("boom");
        };
        LaunchCheck after = env -> invoked.incrementAndGet();
        LaunchCheckExecutor executor = new LaunchCheckExecutor(List.of(failing, after), true, environment);
        assertThrows(LaunchCheckException.class, executor::execute);
        assertEquals(1, invoked.get());
    }

    @Test
    void should_noop_when_no_checks_registered() {
        LaunchCheckExecutor executor = new LaunchCheckExecutor(List.of(), true, environment);
        assertDoesNotThrow(executor::execute);
    }
}
