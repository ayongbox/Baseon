package io.github.ayongbox.baseon.starter.debug.config;

import org.springframework.core.env.Environment;

import java.util.List;

/**
 * 生产 profile 判定工具，debug 端点三层硬失效机制共用的唯一判定逻辑。
 *
 * <p>纯静态、无副作用，方便装配期（Condition）与运行期（Controller 入口）复用同一判定，
 * 避免两处逻辑漂移导致「装配拦了但运行期放行」或反之。
 */
public final class DebugProdProfileGuard {

    private DebugProdProfileGuard() {
    }

    /**
     * activeProfiles 与 prodProfiles 存在交集即判定为生产环境。
     * 任一入参为空时按非生产处理（放行），名单为空意味着业务未声明生产 profile。
     */
    public static boolean isProdActive(Environment environment, List<String> prodProfiles) {
        if (environment == null || prodProfiles == null || prodProfiles.isEmpty()) {
            return false;
        }
        for (String activeProfile : environment.getActiveProfiles()) {
            if (prodProfiles.contains(activeProfile)) {
                return true;
            }
        }
        return false;
    }
}
