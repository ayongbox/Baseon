package io.github.ayongbox.baseon.core.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginUserTest {

    @Test
    void permissionsShouldRoundTripThroughAccessors() {
        LoginUser user = new LoginUser();
        assertNull(user.getPermissions());

        user.setPermissions(Set.of("user:create", "user:list"));
        assertEquals(2, user.getPermissions().size());
        assertTrue(user.getPermissions().contains("user:create"));
    }

    @Test
    void baseFieldsShouldRoundTripThroughAccessors() {
        LoginUser user = new LoginUser();
        user.setUserId(1001L);
        user.setUsername("admin");
        user.setTenantId(2002L);
        user.setUserType(1);

        assertEquals(1001L, user.getUserId());
        assertEquals("admin", user.getUsername());
        assertEquals(2002L, user.getTenantId());
        assertEquals(1, user.getUserType());
    }
}
