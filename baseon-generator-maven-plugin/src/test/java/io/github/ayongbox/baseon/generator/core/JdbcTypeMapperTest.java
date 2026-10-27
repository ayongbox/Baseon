package io.github.ayongbox.baseon.generator.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcTypeMapperTest {

    @Test
    void should_map_bigint_to_long() {
        assertEquals("Long", JdbcTypeMapper.toJavaType("bigint", 20));
        assertEquals("Long", JdbcTypeMapper.toJavaType("BIGINT", 20));
    }

    @Test
    void should_map_int_family_to_integer() {
        assertEquals("Integer", JdbcTypeMapper.toJavaType("int", 11));
        assertEquals("Integer", JdbcTypeMapper.toJavaType("integer", 11));
        assertEquals("Integer", JdbcTypeMapper.toJavaType("smallint", 6));
        assertEquals("Integer", JdbcTypeMapper.toJavaType("INT UNSIGNED", 10));
    }

    @Test
    void should_map_tinyint_by_length() {
        assertEquals("Boolean", JdbcTypeMapper.toJavaType("tinyint", 1));
        assertEquals("Integer", JdbcTypeMapper.toJavaType("tinyint", 4));
    }

    @Test
    void should_map_char_text_json_to_string() {
        assertEquals("String", JdbcTypeMapper.toJavaType("varchar", 255));
        assertEquals("String", JdbcTypeMapper.toJavaType("char", 1));
        assertEquals("String", JdbcTypeMapper.toJavaType("text", 65535));
        assertEquals("String", JdbcTypeMapper.toJavaType("longtext", 0));
        assertEquals("String", JdbcTypeMapper.toJavaType("json", 0));
    }

    @Test
    void should_map_decimal_to_big_decimal() {
        assertEquals("BigDecimal", JdbcTypeMapper.toJavaType("decimal", 10));
        assertEquals("BigDecimal", JdbcTypeMapper.toJavaType("numeric", 10));
    }

    @Test
    void should_map_datetime_family_to_java_time() {
        assertEquals("LocalDateTime", JdbcTypeMapper.toJavaType("datetime", 0));
        assertEquals("LocalDateTime", JdbcTypeMapper.toJavaType("timestamp", 0));
        assertEquals("LocalDate", JdbcTypeMapper.toJavaType("date", 0));
    }

    @Test
    void should_reject_unknown_type() {
        assertThrows(IllegalArgumentException.class, () -> JdbcTypeMapper.toJavaType("blob", 0));
        assertThrows(IllegalArgumentException.class, () -> JdbcTypeMapper.toJavaType(" ", 0));
    }

    @Test
    void should_expose_import_for_non_jdk_lang_types() {
        assertTrue(JdbcTypeMapper.importOf("BigDecimal").isPresent());
        assertTrue(JdbcTypeMapper.importOf("LocalDateTime").isPresent());
        assertTrue(JdbcTypeMapper.importOf("LocalDate").isPresent());
        assertTrue(JdbcTypeMapper.importOf("String").isEmpty());
        assertTrue(JdbcTypeMapper.importOf("Long").isEmpty());
    }
}
