package io.github.ayongbox.baseon.core.context;

import io.github.ayongbox.baseon.core.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContextWrapperTest {

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    void child_thread_should_see_captured_context() throws Exception {
        SecurityUtil.set(loginUser(1L, 100L));

        AtomicReference<Long> seen = new AtomicReference<>();
        Thread thread = new Thread(ContextWrapper.wrap(() -> seen.set(SecurityUtil.getUserId())));
        thread.start();
        thread.join();

        assertEquals(1L, seen.get());
        // 主线程上下文不受子线程影响
        assertEquals(1L, SecurityUtil.getUserId());
    }

    @Test
    void callable_should_see_captured_context() throws Exception {
        SecurityUtil.set(loginUser(2L, 200L));

        Callable<Long> callable = ContextWrapper.wrap(SecurityUtil::getTenantId);
        assertEquals(200L, callable.call());
    }

    @Test
    void supplier_should_see_captured_context() {
        SecurityUtil.set(loginUser(3L, 300L));

        Supplier<String> supplier = ContextWrapper.wrapSupplier(SecurityUtil::getUsername);
        assertEquals("user-3", supplier.get());
    }

    @Test
    void wrapper_should_capture_empty_context() throws Exception {
        AtomicReference<Boolean> loggedIn = new AtomicReference<>(true);
        Thread thread = new Thread(ContextWrapper.wrap(() -> loggedIn.set(SecurityUtil.isLogin())));
        thread.start();
        thread.join();

        assertFalse(loggedIn.get());
        assertNull(SecurityUtil.get());
    }

    private static LoginUser loginUser(Long userId, Long tenantId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        user.setUsername("user-" + userId);
        return user;
    }
}
