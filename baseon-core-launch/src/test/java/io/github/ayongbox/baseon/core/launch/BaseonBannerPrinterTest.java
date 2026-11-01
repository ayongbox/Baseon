package io.github.ayongbox.baseon.core.launch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseonBannerPrinterTest {

    @Test
    void should_print_banner_with_application_name_and_versions() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.application.name", "demo-service");
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            new BaseonBannerPrinter(environment).run(new DefaultApplicationArguments());
        } finally {
            System.setOut(original);
        }
        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(":: demo-service ::"));
        assertTrue(output.contains("Spring Boot"));
        assertTrue(output.contains("JDK"));
    }

    @Test
    void should_fallback_to_default_name_when_application_name_absent() {
        MockEnvironment environment = new MockEnvironment();
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            new BaseonBannerPrinter(environment).run(new DefaultApplicationArguments());
        } finally {
            System.setOut(original);
        }
        assertTrue(captured.toString(StandardCharsets.UTF_8).contains(":: baseon-service ::"));
    }
}
