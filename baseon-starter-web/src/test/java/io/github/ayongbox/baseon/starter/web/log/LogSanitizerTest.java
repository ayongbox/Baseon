package io.github.ayongbox.baseon.starter.web.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogSanitizerTest {

    @Test
    void should_mask_sensitive_json_fields() {
        String body = "{\"username\":\"alice\",\"password\":\"p@ssw0rd\",\"mobile\":\"13800001111\"}";
        String masked = LogSanitizer.sanitize(body, 1000);

        assertTrue(masked.contains("\"username\":\"alice\""));
        assertFalse(masked.contains("p@ssw0rd"));
        assertFalse(masked.contains("13800001111"));
        assertTrue(masked.contains("\"***\""));
    }

    @Test
    void should_mask_bearer_token() {
        String masked = LogSanitizer.sanitize("Authorization: Bearer abc.def.ghi", 1000);
        assertFalse(masked.contains("abc.def.ghi"));
        assertTrue(masked.contains("Bearer ***"));
    }

    @Test
    void should_truncate_oversized_body() {
        String masked = LogSanitizer.sanitize("{\"data\":\"" + "x".repeat(200) + "\"}", 50);
        assertTrue(masked.length() < 100);
        assertTrue(masked.endsWith("...(truncated)"));
    }

    @Test
    void should_return_empty_for_null_body() {
        assertEquals("", LogSanitizer.sanitize(null, 1000));
        assertEquals("", LogSanitizer.sanitize("", 1000));
    }
}
