package io.github.ayongbox.baseon.starter.trace.async;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled 任务 MDC 切面：为每个定时任务执行周期生成独立 TraceId。
 *
 * <p>定时任务没有 HTTP 入口，MDC 不会自动携带 traceId；
 * 本切面在任务执行前后设置/清理，确保日志可追踪。
 */
@Aspect
public class ScheduledTraceAspect {

    @Around("@annotation(scheduled)")
    public Object around(ProceedingJoinPoint joinPoint, Scheduled scheduled) throws Throwable {
        String traceId = io.github.ayongbox.baseon.starter.trace.TraceIdUtil.generate();
        MDC.put(io.github.ayongbox.baseon.starter.trace.TraceIdUtil.TRACE_ID_KEY, traceId);
        try {
            return joinPoint.proceed();
        } finally {
            MDC.remove(io.github.ayongbox.baseon.starter.trace.TraceIdUtil.TRACE_ID_KEY);
        }
    }
}
