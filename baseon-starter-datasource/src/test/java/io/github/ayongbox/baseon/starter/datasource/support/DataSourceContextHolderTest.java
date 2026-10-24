package io.github.ayongbox.baseon.starter.datasource.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceContextHolderTest {

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clear();
    }

    @Test
    void defaultsToNull() {
        assertThat(DataSourceContextHolder.get()).isNull();
    }

    @Test
    void setThenGetReturnsValue() {
        DataSourceContextHolder.set("slave1");
        assertThat(DataSourceContextHolder.get()).isEqualTo("slave1");
    }

    @Test
    void setOverwritesPreviousValue() {
        DataSourceContextHolder.set("slave1");
        DataSourceContextHolder.set("master");
        assertThat(DataSourceContextHolder.get()).isEqualTo("master");
    }

    @Test
    void clearRemovesValue() {
        DataSourceContextHolder.set("slave1");
        DataSourceContextHolder.clear();
        assertThat(DataSourceContextHolder.get()).isNull();
    }

    @Test
    void setBlankEquivalentToClear() {
        DataSourceContextHolder.set("slave1");
        DataSourceContextHolder.set(" ");
        assertThat(DataSourceContextHolder.get()).isNull();
    }
}
