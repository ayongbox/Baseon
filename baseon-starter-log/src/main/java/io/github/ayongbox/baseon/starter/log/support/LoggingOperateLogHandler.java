package io.github.ayongbox.baseon.starter.log.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认审计处理器：单行日志输出，成功 INFO、失败 WARN（含异常简要消息）。
 *
 * <p>仅作开箱即用的兜底实现，生产通常替换为写库 / MQ 的自定义实现。
 */
public class LoggingOperateLogHandler implements OperateLogHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingOperateLogHandler.class);

    @Override
    public void handle(OperateLogRecord record) {
        if (record.isSuccess()) {
            log.info("操作审计 | module={} action={} user={}/{} tenant={} trace={} method={} cost={}ms result=success args={}",
                    record.getModule(), record.getAction(), record.getUserId(), record.getUsername(),
                    record.getTenantId(), record.getTraceId(), record.getMethod(), record.getCostMs(),
                    record.getArgs());
        } else {
            log.warn("操作审计 | module={} action={} user={}/{} tenant={} trace={} method={} cost={}ms result=failed error={} args={}",
                    record.getModule(), record.getAction(), record.getUserId(), record.getUsername(),
                    record.getTenantId(), record.getTraceId(), record.getMethod(), record.getCostMs(),
                    record.getError(), record.getArgs());
        }
    }
}
