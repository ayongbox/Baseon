package io.github.ayongbox.baseon.starter.tenant.support;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.starter.tenant.config.BaseonTenantProperties;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseonTenantLineHandlerTest {

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    void returnsCurrentTenantWhenPresent() {
        LoginUser user = new LoginUser();
        user.setTenantId(1001L);
        SecurityUtil.set(user);

        BaseonTenantLineHandler handler = new BaseonTenantLineHandler(new BaseonTenantProperties());

        assertThat(((LongValue) handler.getTenantId()).getValue()).isEqualTo(1001L);
    }

    @Test
    void fallsBackToImpossibleTenantWhenAbsent() {
        BaseonTenantLineHandler handler = new BaseonTenantLineHandler(new BaseonTenantProperties());

        assertThat(((LongValue) handler.getTenantId()).getValue()).isEqualTo(-1L);
    }

    @Test
    void throwsInStrictModeWhenAbsent() {
        BaseonTenantProperties properties = new BaseonTenantProperties();
        properties.setStrict(true);
        BaseonTenantLineHandler handler = new BaseonTenantLineHandler(properties);

        assertThatThrownBy(handler::getTenantId).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ignoreTableWhitelistWorks() {
        BaseonTenantProperties properties = new BaseonTenantProperties();
        properties.setIgnoreTables(List.of("sys_dict"));
        BaseonTenantLineHandler handler = new BaseonTenantLineHandler(properties);

        assertThat(handler.ignoreTable("sys_dict")).isTrue();
        assertThat(handler.ignoreTable("t_order")).isFalse();
    }
}
