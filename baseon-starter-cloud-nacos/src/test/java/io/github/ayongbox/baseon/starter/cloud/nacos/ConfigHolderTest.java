package io.github.ayongbox.baseon.starter.cloud.nacos;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigHolderTest {

    @Test
    void readsPropertyWithDefaultFallback() {
        MockEnvironment environment = new MockEnvironment().withProperty("demo.greeting", "hi");
        ConfigHolder holder = new ConfigHolder(environment);

        assertThat(holder.get("demo.greeting")).isEqualTo("hi");
        assertThat(holder.get("missing", "fallback")).isEqualTo("fallback");
        assertThat(holder.get("missing-int", Integer.class, 42)).isEqualTo(42);
    }
}
