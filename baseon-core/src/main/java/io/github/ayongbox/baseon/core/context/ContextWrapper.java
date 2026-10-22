package io.github.ayongbox.baseon.core.context;

import io.github.ayongbox.baseon.core.model.LoginUser;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 异步任务上下文包装：提交任务时捕获当前上下文，执行时注入，结束后还原。
 *
 * <p>使用示例：
 * <pre>{@code
 * executor.submit(ContextWrapper.wrap(() -> orderService.create(req)));
 * }</pre>
 *
 * <p>框架托管的线程池（@Async、@Scheduled、线程池 Bean）由 Starter 自动包装，
 * 手写线程池与 CompletableFuture 必须显式使用本工具。
 */
public final class ContextWrapper {

    private ContextWrapper() {
    }

    public static Runnable wrap(Runnable task) {
        LoginUser captured = SecurityUtil.get();
        return () -> {
            LoginUser previous = SecurityUtil.get();
            try {
                SecurityUtil.set(captured);
                task.run();
            } finally {
                SecurityUtil.set(previous);
            }
        };
    }

    public static <T> Callable<T> wrap(Callable<T> task) {
        LoginUser captured = SecurityUtil.get();
        return () -> {
            LoginUser previous = SecurityUtil.get();
            try {
                SecurityUtil.set(captured);
                return task.call();
            } finally {
                SecurityUtil.set(previous);
            }
        };
    }

    public static <T> Supplier<T> wrap(Supplier<T> task) {
        LoginUser captured = SecurityUtil.get();
        return () -> {
            LoginUser previous = SecurityUtil.get();
            try {
                SecurityUtil.set(captured);
                return task.get();
            } finally {
                SecurityUtil.set(previous);
            }
        };
    }
}
