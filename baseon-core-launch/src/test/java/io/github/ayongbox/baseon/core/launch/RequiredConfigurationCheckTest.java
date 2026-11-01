package io.github.ayongbox.baseon.core.launch;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiredConfigurationCheckTest {

    private static final String MISSING_ENV = "BASEON_TEST_DEFINITELY_MISSING_ENV_9F3K";

    @Test
    void should_pass_when_all_required_items_present() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.application.name", "demo");
        LaunchProperties properties = new LaunchProperties();
        properties.setRequiredProperties(List.of("spring.application.name"));
        LaunchCheck check = RequiredConfigurationCheck.of(environment, properties);
        assertDoesNotThrow(() -> check.check(environment));
    }

    @Test
    void should_fail_when_required_property_missing() {
        MockEnvironment environment = new MockEnvironment();
        LaunchProperties properties = new LaunchProperties();
        properties.setRequiredProperties(List.of("spring.datasource.url"));
        LaunchCheck check = RequiredConfigurationCheck.of(environment, properties);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> check.check(environment));
        assertTrue(ex.getMessage().contains("property:spring.datasource.url"));
    }

    @Test
    void should_fail_when_required_environment_variable_missing() {
        MockEnvironment environment = new MockEnvironment();
        LaunchProperties properties = new LaunchProperties();
        properties.setRequiredEnvironmentVariables(List.of(MISSING_ENV));
        LaunchCheck check = RequiredConfigurationCheck.of(environment, properties);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> check.check(environment));
        assertTrue(ex.getMessage().contains("env:" + MISSING_ENV));
    }

    @Test
    void should_pass_when_checklist_empty() {
        MockEnvironment environment = new MockEnvironment();
        LaunchCheck check = RequiredConfigurationCheck.of(environment, new LaunchProperties());
        assertDoesNotThrow(() -> check.check(environment));
    }

    @Test
    void should_expose_stable_check_name() {
        MockEnvironment environment = new MockEnvironment();
        LaunchCheck check = RequiredConfigurationCheck.of(environment, new LaunchProperties());
        assertEquals("required-configuration", check.name());
    }
}
