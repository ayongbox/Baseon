package io.github.ayongbox.baseon.starter.debug.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DebugProdProfileGuard.isProdActive 的分支覆盖：
 * 空 profile / 仅 dev / 仅 prod / dev 与 prod 并存 / 自定义 prodProfiles。
 */
class DebugProdProfileGuardTest {

    @Test
    void noActiveProfileIsNotProd() {
        // Given：未激活任何 profile
        MockEnvironment environment = new MockEnvironment();

        // When：判定是否为生产环境
        boolean result = DebugProdProfileGuard.isProdActive(environment, List.of("prod"));

        // Then：不是生产
        assertFalse(result);
    }

    @Test
    void devProfileOnlyIsNotProd() {
        // Given：仅激活 dev
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        // When：判定
        boolean result = DebugProdProfileGuard.isProdActive(environment, List.of("prod"));

        // Then：不是生产
        assertFalse(result);
    }

    @Test
    void prodProfileIsProd() {
        // Given：激活 prod
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        // When：判定
        boolean result = DebugProdProfileGuard.isProdActive(environment, List.of("prod"));

        // Then：是生产
        assertTrue(result);
    }

    @Test
    void devAndProdCoexistingIsProd() {
        // Given：dev 与 prod 并存
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev", "prod");

        // When：判定
        boolean result = DebugProdProfileGuard.isProdActive(environment, List.of("prod"));

        // Then：存在交集即生产
        assertTrue(result);
    }

    @Test
    void customProdProfilesAreHonored() {
        // Given：自定义生产名单为 production，激活 production
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        // When：用自定义名单判定
        boolean result = DebugProdProfileGuard.isProdActive(environment, List.of("production", "prod"));

        // Then：是生产
        assertTrue(result);
    }

    @Test
    void customProdProfilesDoNotMatchDefaultProdName() {
        // Given：名单只含 production，但激活的是 prod
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        // When：用自定义名单判定
        boolean result = DebugProdProfileGuard.isProdActive(environment, List.of("production"));

        // Then：无交集，不是生产（名单由业务全权掌控）
        assertFalse(result);
    }

    @Test
    void emptyProdProfilesIsNotProd() {
        // Given：激活 prod，但生产名单为空
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        // When：判定
        boolean result = DebugProdProfileGuard.isProdActive(environment, List.of());

        // Then：名单为空视为未声明生产 profile，放行
        assertFalse(result);
    }
}
