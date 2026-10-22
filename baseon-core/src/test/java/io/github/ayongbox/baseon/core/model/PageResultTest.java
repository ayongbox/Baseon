package io.github.ayongbox.baseon.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    @Test
    void should_calculate_pages() {
        PageResult<String> result = PageResult.of(25, 2, 10, List.of("a", "b"));
        assertEquals(25, result.getTotal());
        assertEquals(3, result.getPages());
        assertEquals(2, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(2, result.getRecords().size());
    }

    @Test
    void should_return_zero_pages_when_size_invalid() {
        assertEquals(0, PageResult.of(25, 1, 0, List.of()).getPages());
    }

    @Test
    void empty_page_should_have_no_records() {
        PageResult<String> result = PageResult.empty(1, 10);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPages());
        assertTrue(result.getRecords().isEmpty());
    }
}
