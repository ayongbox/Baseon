package io.github.ayongbox.baseon.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {

    @Test
    void should_use_default_paging() {
        PageQuery query = new PageQuery();
        assertEquals(1, query.getCurrent());
        assertEquals(10, query.getSize());
        assertEquals(0, query.offset());
    }

    @Test
    void should_reset_invalid_values_to_default() {
        PageQuery query = new PageQuery();
        query.setCurrent(0);
        query.setSize(-5);
        assertEquals(PageQuery.DEFAULT_CURRENT, query.getCurrent());
        assertEquals(PageQuery.DEFAULT_SIZE, query.getSize());
    }

    @Test
    void should_cap_size_to_max() {
        PageQuery query = new PageQuery();
        query.setCurrent(2);
        query.setSize(100_000);
        assertEquals(PageQuery.MAX_SIZE, query.getSize());
        assertEquals(PageQuery.MAX_SIZE, query.offset());
    }
}
