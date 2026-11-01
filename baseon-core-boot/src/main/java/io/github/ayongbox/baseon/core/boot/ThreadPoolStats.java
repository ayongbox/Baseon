package io.github.ayongbox.baseon.core.boot;

/**
 * 线程池运行快照，用于接入日志/监控端点。
 *
 * @param poolSize             当前线程数
 * @param activeCount          正在执行任务的线程数
 * @param corePoolSize         核心线程数
 * @param maximumPoolSize      最大线程数
 * @param queueSize            队列中等待的任务数
 * @param queueRemainingCapacity 队列剩余容量
 * @param completedTaskCount   已完成任务数（近似值）
 * @param taskCount            已提交任务数（近似值）
 * @param largestPoolSize      历史最大线程数
 */
public record ThreadPoolStats(
        int poolSize,
        int activeCount,
        int corePoolSize,
        int maximumPoolSize,
        int queueSize,
        int queueRemainingCapacity,
        long completedTaskCount,
        long taskCount,
        int largestPoolSize) {

    /**
     * 队列总容量；同步队列（容量 0）返回 0，无界队列返回 {@link Integer#MAX_VALUE}。
     */
    public int queueCapacity() {
        return queueSize + queueRemainingCapacity;
    }
}
