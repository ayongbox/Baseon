package io.github.ayongbox.baseon.core.boot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringContextHolderTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("dev");
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("baseon.test.flag", "on")));
        context.register(SpringContextHolder.class, SampleEventListener.class);
        context.registerBean("sampleBean", SampleBean.class, SampleBean::new);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void should_lookup_bean_by_type_and_name() {
        assertNotNull(SpringContextHolder.getBean(SampleBean.class));
        assertEquals("sample", SpringContextHolder.getBean(SampleBean.class).getName());
        assertEquals("sample", SpringContextHolder.getBean("sampleBean", SampleBean.class).getName());
        assertEquals("sample", ((SampleBean) SpringContextHolder.getBean("sampleBean")).getName());
        assertTrue(SpringContextHolder.containsBean("sampleBean"));
    }

    @Test
    void missing_bean_should_return_null_instead_of_throwing() {
        assertNull(SpringContextHolder.getBeanOrNull(Runnable.class));
    }

    @Test
    void should_publish_event_to_listener() {
        SpringContextHolder.publishEvent("hello");
        assertEquals("hello", SampleEventListener.last.get());
    }

    @Test
    void should_expose_profiles_and_properties() {
        assertArrayEquals(new String[]{"dev"}, SpringContextHolder.getActiveProfiles());
        assertEquals("on", SpringContextHolder.getProperty("baseon.test.flag"));
        assertEquals("fallback", SpringContextHolder.getProperty("baseon.test.missing", "fallback"));
        assertEquals(8080, SpringContextHolder.getProperty("baseon.test.port", Integer.class, 8080));
    }

    static class SampleBean {

        String getName() {
            return "sample";
        }
    }

    static class SampleEventListener {

        static final AtomicReference<Object> last = new AtomicReference<>();

        @EventListener
        void onEvent(String event) {
            last.set(event);
        }
    }
}
