package io.github.ayongbox.baseon.core.launch;

import org.springframework.core.env.Environment;

/**
 * 启动前置校验 SPI：业务通过注册 Bean 提供自定义检查，框架只负责编排与失败处理。
 *
 * <p>实现必须是无副作用的只读检查（验证配置、端口、外部依赖可达性等），
 * 禁止在这里做初始化写操作——启动阶段顺序不稳定，写操作可能依赖未就绪的 Bean。
 */
@FunctionalInterface
public interface LaunchCheck {

    /**
     * 检查名称，用于日志与失败摘要，必须稳定且唯一（建议 kebab-case）。
     *
     * @return 检查名称
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * 执行检查。检查失败通过抛出异常表示，框架统一收集并按 fail-fast 策略处理。
     *
     * @param environment 当前环境，可读取配置；禁止使用 ApplicationContext 获取业务 Bean
     * @throws Exception 检查失败原因（会进入失败摘要）
     */
    void check(Environment environment) throws Exception;
}
