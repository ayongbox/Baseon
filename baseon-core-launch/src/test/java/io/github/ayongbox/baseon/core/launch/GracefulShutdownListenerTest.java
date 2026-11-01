package io.github.ayongbox.baseon.core.launch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GracefulShutdownListenerTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void should_execute_all_hooks_even_when_one_throws() {
        List<String> executed = new CopyOnWriteArrayList<>();
        context.registerBean("failingHook", ShutdownHook.class, () -> () -> {
            executed.add("failing");
            throw new IllegalStateException("flush failed");
        });
        context.registerBean("normalHook", ShutdownHook.class, () -> () -> executed.add("normal"));
        context.refresh();

        new GracefulShutdownListener().onApplicationEvent(new ContextClosedEvent(context));

        assertEquals(2, executed.size());
        org.junit.jupiter.api.Assertions.assertTrue(executed.containsAll(List.of("failing", "normal")));
    }

    @Test
    void should_run_at_highest_precedence() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE, new GracefulShutdownListener().getOrder());
    }
}
