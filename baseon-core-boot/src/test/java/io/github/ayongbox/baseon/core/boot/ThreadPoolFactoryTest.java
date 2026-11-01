package io.github.ayongbox.baseon.core.boot;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadPoolFactoryTest {

    private ThreadPoolExecutor executor;
    private ScheduledThreadPoolExecutor scheduledExecutor;

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
        if (executor != null) {
            executor.shutdownNow();
        }
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

    @Test
    void threads_should_be_named_with_forced_prefix() throws Exception {
        executor = ThreadPoolFactory.build("order-async", 2, 2, 16);

        Set<String> names = ConcurrentHashMap.newKeySet();
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            futures.add(executor.submit(() -> Thread.currentThread().getName()));
        }
        for (Future<String> future : futures) {
            names.add(future.get());
        }

        assertEquals(2, names.size());
        assertTrue(names.stream().allMatch(name -> name.startsWith("order-async-")));
    }

    @Test
    void submitted_tasks_should_see_captured_context_and_worker_should_be_clean() throws Exception {
        SecurityUtil.set(loginUser(100L));
        executor = ThreadPoolFactory.build("ctx", 1, 1, 8);

        assertEquals(100L, executor.submit(SecurityUtil::getUserId).get());

        SecurityUtil.clear();
        assertFalse(executor.submit(SecurityUtil::isLogin).get());
    }

    @Test
    void abort_policy_should_reject_when_saturated() throws Exception {
        executor = ThreadPoolFactory.build("reject", 1, 1, 0, 60, ThreadPoolFactory.RejectionPolicy.ABORT);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        executor.execute(() -> {
            started.countDown();
            awaitQuietly(release);
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));

        assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> {
        }));
        release.countDown();
    }

    @Test
    void caller_runs_policy_should_execute_task_in_submitter_thread() throws Exception {
        executor = ThreadPoolFactory.build("caller", 1, 1, 0, 60, ThreadPoolFactory.RejectionPolicy.CALLER_RUNS);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        executor.execute(() -> {
            started.countDown();
            awaitQuietly(release);
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));

        AtomicReference<String> runner = new AtomicReference<>();
        executor.execute(() -> runner.set(Thread.currentThread().getName()));
        assertFalse(runner.get().startsWith("caller-"));

        release.countDown();
    }

    @Test
    void snapshot_should_expose_pool_state() throws Exception {
        executor = ThreadPoolFactory.build("stats", 1, 2, 4);
        assertEquals("done", executor.submit(() -> "done").get());

        ThreadPoolStats stats = ThreadPoolFactory.snapshot(executor);
        assertEquals(1, stats.corePoolSize());
        assertEquals(2, stats.maximumPoolSize());
        assertEquals(4, stats.queueCapacity());
        assertEquals(1, stats.poolSize());
    }

    @Test
    void scheduled_tasks_should_be_named_and_context_aware() throws Exception {
        SecurityUtil.set(loginUser(100L));
        scheduledExecutor = ThreadPoolFactory.buildScheduled("sched", 1);

        ScheduledFuture<String> nameFuture = scheduledExecutor.schedule(
                () -> Thread.currentThread().getName(), 10, TimeUnit.MILLISECONDS);
        assertTrue(nameFuture.get().startsWith("sched-"));

        AtomicBoolean contextVisible = new AtomicBoolean(true);
        CountDownLatch runs = new CountDownLatch(3);
        ScheduledFuture<?> periodic = scheduledExecutor.scheduleAtFixedRate(() -> {
            if (!SecurityUtil.isLogin()) {
                contextVisible.set(false);
            }
            runs.countDown();
        }, 0, 20, TimeUnit.MILLISECONDS);
        assertTrue(runs.await(2, TimeUnit.SECONDS));
        periodic.cancel(false);

        assertTrue(contextVisible.get());
    }

    @Test
    void build_should_reject_invalid_arguments() {
        assertThrows(IllegalArgumentException.class, () -> ThreadPoolFactory.build(" "));
        assertThrows(IllegalArgumentException.class, () -> ThreadPoolFactory.build("bad", 2, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> ThreadPoolFactory.buildScheduled("bad", 0));
    }

    private static LoginUser loginUser(long userId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setTenantId(1L);
        user.setUsername("user-" + userId);
        return user;
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
