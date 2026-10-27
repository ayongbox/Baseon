package io.github.ayongbox.baseon.starter.log.aspect;

import io.github.ayongbox.baseon.core.annotation.OperateLog;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.starter.log.config.BaseonLogProperties;
import io.github.ayongbox.baseon.starter.log.support.OperateLogHandler;
import io.github.ayongbox.baseon.starter.log.support.OperateLogRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * {@code @OperateLog} 切面：环绕采集「谁在哪个模块做了什么操作、结果如何」，
 * 组装 {@link OperateLogRecord} 交给 {@link OperateLogHandler} 消费。
 *
 * <p>职责边界（三者分工）：
 * <ul>
 *   <li>本模块只做<b>业务语义审计</b>——只记标注了 {@code @OperateLog} 的关键操作；</li>
 *   <li>不管全局 logback 配置与 traceId 生成——那是 starter-trace 的职责
 *       （这里仅从 MDC 读 traceId 做关联）；</li>
 *   <li>不管全量请求流水——那是 starter-web 请求日志的职责（每个请求一条访问日志）。</li>
 * </ul>
 *
 * <p>异常路径：记录 failed 后<b>原样上抛</b>，切面绝不吞异常、不改变业务语义。
 */
@Aspect
public class OperateLogAspect {

    /** 异常简要消息最大长度，防止超长堆栈文本撑爆审计存储 */
    private static final int ERROR_MAX_LENGTH = 200;

    private final OperateLogHandler handler;
    private final BaseonLogProperties properties;

    public OperateLogAspect(OperateLogHandler handler, BaseonLogProperties properties) {
        this.handler = handler;
        this.properties = properties;
    }

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint pjp, OperateLog operateLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            publish(pjp, operateLog, System.currentTimeMillis() - start, true, null);
            return result;
        } catch (Throwable ex) {
            publish(pjp, operateLog, System.currentTimeMillis() - start, false, briefError(ex));
            throw ex;
        }
    }

    /** 组装审计记录并交给 SPI 处理器；操作人 / 租户 / traceId 无上下文时为 null */
    private void publish(ProceedingJoinPoint pjp, OperateLog operateLog, long costMs,
                         boolean success, String error) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + '.' + method.getName();
        OperateLogRecord record = new OperateLogRecord(
                operateLog.module(),
                operateLog.action(),
                SecurityUtil.getUserId(),
                SecurityUtil.getUsername(),
                SecurityUtil.getTenantId(),
                MDC.get("traceId"),
                methodName,
                summarizeArgs(pjp.getArgs(), operateLog),
                costMs,
                success,
                error);
        handler.handle(record);
    }

    /** 入参摘要：recordArgs=false 时不采集返回 null，否则 deepToString 后按 argMaxLength 截断 */
    private String summarizeArgs(Object[] args, OperateLog operateLog) {
        if (!operateLog.recordArgs()) {
            return null;
        }
        return truncate(Arrays.deepToString(args), properties.getArgMaxLength());
    }

    /** 异常简要消息：异常类简单名 + message，整体截断 200 */
    private String briefError(Throwable ex) {
        String message = ex.getMessage();
        String brief = message == null
                ? ex.getClass().getSimpleName()
                : ex.getClass().getSimpleName() + ": " + message;
        return truncate(brief, ERROR_MAX_LENGTH);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
