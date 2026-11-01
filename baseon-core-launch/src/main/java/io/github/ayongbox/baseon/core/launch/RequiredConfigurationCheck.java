package io.github.ayongbox.baseon.core.launch;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置前置校验：必选环境变量与配置项是否齐全。
 *
 * <p>把「本服务依赖的外部配置」从文档约定变成启动期硬约束，
 * 避免启动后首次调用才暴露配置缺失（如 Nacos 地址、密钥、下游地址）。
 * 检查项来自 {@code baseon.launch.required-*}，框架不内置业务规则。
 */
public final class RequiredConfigurationCheck {

    private RequiredConfigurationCheck() {
    }

    /**
     * 基于当前环境与配置清单创建检查实例。
     *
     * @param environment 当前环境
     * @param properties 启动扩展配置
     * @return 检查实例；清单为空时也返回（执行结果为通过），保持编排简单
     */
    public static LaunchCheck of(ConfigurableEnvironment environment, LaunchProperties properties) {
        return new LaunchCheck() {
            @Override
            public String name() {
                return "required-configuration";
            }

            @Override
            public void check(Environment env) {
                List<String> missing = new ArrayList<>();
                for (String variable : properties.getRequiredEnvironmentVariables()) {
                    String value = System.getenv(variable);
                    if (value == null || value.isBlank()) {
                        missing.add("env:" + variable);
                    }
                }
                for (String property : properties.getRequiredProperties()) {
                    if (!environment.containsProperty(property)) {
                        missing.add("property:" + property);
                    }
                }
                if (!missing.isEmpty()) {
                    throw new IllegalStateException("缺少必选配置：" + String.join(", ", missing));
                }
            }
        };
    }
}
