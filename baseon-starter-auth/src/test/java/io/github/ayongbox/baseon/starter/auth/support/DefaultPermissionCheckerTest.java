package io.github.ayongbox.baseon.starter.auth.support;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPermissionCheckerTest {

    private final DefaultPermissionChecker checker = new DefaultPermissionChecker();

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    void returnsTrueWhenLoginUserHasPermission() {
        LoginUser user = new LoginUser();
        user.setUserId(1001L);
        user.setPermissions(Set.of("user:create", "user:list"));
        SecurityUtil.set(user);

        assertThat(checker.hasPermission("user:create")).isTrue();
    }

    @Test
    void returnsFalseWhenNotLogin() {
        assertThat(checker.hasPermission("user:create")).isFalse();
    }

    @Test
    void returnsFalseWhenPermissionMissing() {
        LoginUser user = new LoginUser();
        user.setUserId(1001L);
        user.setPermissions(Set.of("user:list"));
        SecurityUtil.set(user);

        assertThat(checker.hasPermission("user:create")).isFalse();
    }
}
