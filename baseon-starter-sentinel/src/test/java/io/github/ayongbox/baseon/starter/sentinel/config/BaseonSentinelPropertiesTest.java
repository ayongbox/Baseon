package io.github.ayongbox.baseon.starter.sentinel.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BaseonSentinelProperties} 默认值与 setter 用例。
 */
class BaseonSentinelPropertiesTest {

    @Test
    void defaultValuesAreEnabled() {
        BaseonSentinelProperties properties = new BaseonSentinelProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isBlockHandlerEnabled()).isTrue();
    }

    @Test
    void settersUpdateValues() {
        BaseonSentinelProperties properties = new BaseonSentinelProperties();
        properties.setEnabled(false);
        properties.setBlockHandlerEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isBlockHandlerEnabled()).isFalse();
    }
}
