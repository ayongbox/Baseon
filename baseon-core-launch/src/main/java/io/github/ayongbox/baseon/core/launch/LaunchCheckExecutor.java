package io.github.ayongbox.baseon.core.launch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动前置校验执行器：在应用就绪时统一执行所有 {@link LaunchCheck} Bean。
 *
 * <p>机制（编排、失败摘要、fail-fast 策略）由框架承载；策略（检查什么）由业务通过
 * SPI 注入，与数据权限的 Provider 分层一致。任一检查失败时，
 * {@code baseon.launch.fail-fast=true}（默认）抛出异常终止启动，
 * 否则降级为 WARN 日志，仅用于本地联调。
 */
public class LaunchCheckExecutor {

    private static final Logger log = LoggerFactory.getLogger(LaunchCheckExecutor.class);

    private final List<LaunchCheck> checks;

    private final boolean failFast;

    private final Environment environment;

    public LaunchCheckExecutor(List<LaunchCheck> checks, boolean failFast, Environment environment) {
        this.checks = checks == null ? List.of() : List.copyOf(checks);
        this.failFast = failFast;
        this.environment = environment;
    }

    /**
     * 在应用完全启动后、对外提供服务前执行校验。
     * 此时 Environment 已就绪，且早于流量进入（readiness 探针由 K8s 控制）。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void execute() {
        if (checks.isEmpty()) {
            return;
        }
        List<String> failures = new ArrayList<>();
        for (LaunchCheck check : checks) {
            try {
                check.check(environment);
            } catch (Exception ex) {
                failures.add(check.name() + ": " + ex.getMessage());
                log.error("[baseon] 启动前置校验失败：{}", check.name(), ex);
            }
        }
        if (failures.isEmpty()) {
            log.info("[baseon] 启动前置校验通过（{} 项）", checks.size());
            return;
        }
        String message = "启动前置校验失败（" + failures.size() + " 项）：" + String.join("；", failures);
        if (failFast) {
            throw new LaunchCheckException(message);
        }
        log.warn(message);
    }
}
