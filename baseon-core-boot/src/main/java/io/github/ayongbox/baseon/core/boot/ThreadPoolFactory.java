package io.github.ayongbox.baseon.core.boot;

import io.github.ayongbox.baseon.core.context.ContextWrapper;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池统一创建入口：强制线程命名前缀、默认包装登录上下文、提供拒绝策略与运行快照。
 *
 * <p>所有通过本工厂创建的线程池都会在提交任务时捕获当前 {@link io.github.ayongbox.baseon.core.model.LoginUser}
 * 并在执行时注入，等价于自动调用 {@link ContextWrapper#wrap(Runnable)}；
 * 业务禁止直接使用 {@code Executors.newXxx}，否则上下文丢失且线程名无法定位。
 *
 * <p>线程池由调用方负责关闭（{@link ThreadPoolExecutor#shutdown()} / {@code shutdownNow()}），
 * 框架不会托管它们的生命周期。
 */
public final class ThreadPoolFactory {

    /**
     * 默认队列容量：有界队列，避免无界队列掩盖任务积压。
     */
    public static final int DEFAULT_QUEUE_CAPACITY = 1024;

    /**
     * 默认空闲存活时间（秒）。
     */
    public static final long DEFAULT_KEEP_ALIVE_SECONDS = 60L;

    private ThreadPoolFactory() {
    }

    /**
     * 按 CPU 核数创建通用线程池：核心数 = CPU，最大数 = CPU * 2。
     */
    public static ThreadPoolExecutor build(String threadNamePrefix) {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        return build(threadNamePrefix, corePoolSize, corePoolSize * 2, DEFAULT_QUEUE_CAPACITY);
    }

    public static ThreadPoolExecutor build(String threadNamePrefix, int corePoolSize,
                                           int maximumPoolSize, int queueCapacity) {
        return build(threadNamePrefix, corePoolSize, maximumPoolSize, queueCapacity,
                DEFAULT_KEEP_ALIVE_SECONDS, RejectionPolicy.CALLER_RUNS);
    }

    /**
     * @param queueCapacity 小于等于 0 时使用 {@link SynchronousQueue}（不排队，直接交给线程或拒绝）
     */
    public static ThreadPoolExecutor build(String threadNamePrefix, int corePoolSize, int maximumPoolSize,
                                           int queueCapacity, long keepAliveSeconds,
                                           RejectionPolicy rejectionPolicy) {
        String prefix = requirePrefix(threadNamePrefix);
        validatePoolSize(corePoolSize, maximumPoolSize);
        if (keepAliveSeconds < 0) {
            throw new IllegalArgumentException("keepAliveSeconds 不能为负数: " + keepAliveSeconds);
        }
        Objects.requireNonNull(rejectionPolicy, "rejectionPolicy");
        BlockingQueue<Runnable> workQueue = queueCapacity > 0
                ? new LinkedBlockingQueue<>(queueCapacity)
                : new SynchronousQueue<>();
        return new ContextAwareThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveSeconds,
                TimeUnit.SECONDS, workQueue, new NamedThreadFactory(prefix), rejectionPolicy.handler());
    }

    /**
     * 创建周期/延时任务线程池，提交任务时同样捕获当前上下文。
     */
    public static ScheduledThreadPoolExecutor buildScheduled(String threadNamePrefix, int corePoolSize) {
        String prefix = requirePrefix(threadNamePrefix);
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("corePoolSize 必须大于 0: " + corePoolSize);
        }
        return new ContextAwareScheduledExecutor(corePoolSize, new NamedThreadFactory(prefix));
    }

    /**
     * 生成运行快照；调用方按需上报日志或监控端点。
     */
    public static ThreadPoolStats snapshot(ThreadPoolExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        BlockingQueue<Runnable> queue = executor.getQueue();
        return new ThreadPoolStats(
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                queue.size(),
                queue.remainingCapacity(),
                executor.getCompletedTaskCount(),
                executor.getTaskCount(),
                executor.getLargestPoolSize());
    }

    private static String requirePrefix(String threadNamePrefix) {
        if (threadNamePrefix == null || threadNamePrefix.isBlank()) {
            throw new IllegalArgumentException("threadNamePrefix 不能为空：线程名必须可定位到业务");
        }
        return threadNamePrefix.trim();
    }

    private static void validatePoolSize(int corePoolSize, int maximumPoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException("corePoolSize 不能为负数: " + corePoolSize);
        }
        if (maximumPoolSize <= 0) {
            throw new IllegalArgumentException("maximumPoolSize 必须大于 0: " + maximumPoolSize);
        }
        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException(
                    "maximumPoolSize(" + maximumPoolSize + ") 不能小于 corePoolSize(" + corePoolSize + ")");
        }
    }

    /**
     * 拒绝策略枚举，避免业务直接依赖 {@link ThreadPoolExecutor} 的内部策略类。
     */
    public enum RejectionPolicy {

        /**
         * 抛 {@link java.util.concurrent.RejectedExecutionException}，由调用方感知并降级。
         */
        ABORT,

        /**
         * 由提交线程执行，形成天然背压（默认）。
         */
        CALLER_RUNS,

        /**
         * 静默丢弃新任务。
         */
        DISCARD,

        /**
         * 丢弃队列中最旧的任务后重试。
         */
        DISCARD_OLDEST;

        RejectedExecutionHandler handler() {
            switch (this) {
                case ABORT:
                    return new ThreadPoolExecutor.AbortPolicy();
                case CALLER_RUNS:
                    return new ThreadPoolExecutor.CallerRunsPolicy();
                case DISCARD:
                    return new ThreadPoolExecutor.DiscardPolicy();
                case DISCARD_OLDEST:
                    return new ThreadPoolExecutor.DiscardOldestPolicy();
                default:
                    throw new IllegalStateException("未知拒绝策略: " + this);
            }
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {

        private final String prefix;
        private final AtomicInteger sequence = new AtomicInteger(1);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + sequence.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }

    /**
     * 执行时注入登录上下文：submit / invokeAll 最终都会走 {@link #execute(Runnable)}，无需逐个重写。
     */
    private static final class ContextAwareThreadPoolExecutor extends ThreadPoolExecutor {

        private ContextAwareThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                               TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                               ThreadFactory threadFactory,
                                               RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        @Override
        public void execute(Runnable command) {
            super.execute(ContextWrapper.wrap(command));
        }
    }

    /**
     * ScheduledThreadPoolExecutor 的 {@code execute/submit} 内部转发到 {@code schedule}，
     * 因此只需覆盖四个调度入口即可覆盖全部提交路径。
     */
    private static final class ContextAwareScheduledExecutor extends ScheduledThreadPoolExecutor {

        private ContextAwareScheduledExecutor(int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return super.schedule(ContextWrapper.wrap(command), delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return super.schedule(ContextWrapper.wrap(callable), delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                      long period, TimeUnit unit) {
            return super.scheduleAtFixedRate(ContextWrapper.wrap(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                         long delay, TimeUnit unit) {
            return super.scheduleWithFixedDelay(ContextWrapper.wrap(command), initialDelay, delay, unit);
        }
    }
}
