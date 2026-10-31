package io.github.ayongbox.baseon.test.core;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockFactoryTest {

    @Test
    void loginUserCarriesDefaults() {
        LoginUser user = MockFactory.loginUser();

        assertThat(user.getTenantId()).isEqualTo(TestConstants.DEFAULT_TENANT_ID);
        assertThat(user.getUserId()).isEqualTo(TestConstants.DEFAULT_USER_ID);
        assertThat(user.getUsername()).isEqualTo(TestConstants.DEFAULT_USERNAME);
    }

    @Test
    void loginUserInContextWritesSecurityUtil() {
        LoginUser user = MockFactory.loginUserInContext(2002L, 9L);

        assertThat(SecurityUtil.get()).isSameAs(user);
        assertThat(user.getTenantId()).isEqualTo(2002L);
        assertThat(user.getUserId()).isEqualTo(9L);

        SecurityUtil.clear();
    }
}
