package io.github.ayongbox.baseon.starter.cloud.nacos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nacos 治理能力配置，前缀 {@code baseon.cloud.nacos}。
 *
 * <p>注意：server-addr / namespace / 账号密码等连接参数仍走 SCA 原生配置
 * {@code spring.cloud.nacos.*}，本类只承载脚手架增强能力。
 */
@ConfigurationProperties(prefix = "baseon.cloud.nacos")
public class BaseonCloudNacosProperties {

    /**
     * 注册策略，默认 FULL_REGISTER。
     *
     * <ul>
     *   <li>{@code LOCAL_NO_REGISTER}：本地调试，不向注册中心注册，其他服务看不到本实例</li>
     *   <li>{@code LOCAL_REGISTER_PRIVATE}：注册但打上 {@code baseon.local=true} 元数据标记，
     *       配合 starter-loadbalance 的 local 实例过滤，其他服务默认不会路由到本实例，
     *       本实例自己发出的调用不受影响</li>
     *   <li>{@code FULL_REGISTER}：正常注册，参与集群负载均衡（生产档位）</li>
     * </ul>
     */
    private RegisterStrategy registerStrategy = RegisterStrategy.FULL_REGISTER;

    public RegisterStrategy getRegisterStrategy() {
        return registerStrategy;
    }

    public void setRegisterStrategy(RegisterStrategy registerStrategy) {
        this.registerStrategy = registerStrategy;
    }

    public enum RegisterStrategy {
        LOCAL_NO_REGISTER,
        LOCAL_REGISTER_PRIVATE,
        FULL_REGISTER
    }
}
