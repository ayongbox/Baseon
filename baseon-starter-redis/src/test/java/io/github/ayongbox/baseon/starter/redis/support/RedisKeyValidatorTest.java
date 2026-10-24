package io.github.ayongbox.baseon.starter.redis.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RedisKeyValidator} 全规则正反用例。
 * strict=true 应抛 {@link IllegalArgumentException}，strict=false 应放行。
 */
class RedisKeyValidatorTest {

    @Test
    void acceptsStandardSegmentedKeys() {
        assertThatCode(() -> RedisKeyValidator.validate("order:detail:123", true)).doesNotThrowAnyException();
        assertThatCode(() -> RedisKeyValidator.validate("user_info.cache-01.v2", true)).doesNotThrowAnyException();
        assertThatCode(() -> RedisKeyValidator.validate("a", true)).doesNotThrowAnyException();
        assertThatCode(() -> RedisKeyValidator.validate("A0:Z9_z.a-b", true)).doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankKeys() {
        assertThatThrownBy(() -> RedisKeyValidator.validate(null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisKeyValidator.validate("", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisKeyValidator.validate("   ", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOverLengthKeys() {
        // 恰好 128 为边界合法值
        assertThatCode(() -> RedisKeyValidator.validate("a".repeat(RedisKeyValidator.MAX_LENGTH), true))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> RedisKeyValidator.validate("a".repeat(RedisKeyValidator.MAX_LENGTH + 1), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("长度");
    }

    @Test
    void rejectsIllegalCharacters() {
        // 空格、"#"、中文、"*" 均不在白名单内
        assertThatThrownBy(() -> RedisKeyValidator.validate("bad key", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisKeyValidator.validate("order#1", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisKeyValidator.validate("金额", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisKeyValidator.validate("a*b", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsConsecutiveColons() {
        assertThatThrownBy(() -> RedisKeyValidator.validate("a::b", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("冒号");
        assertThatThrownBy(() -> RedisKeyValidator.validate("a:::b", true))
                .isInstanceOf(IllegalArgumentException.class);
        // 单个冒号分段合法
        assertThatCode(() -> RedisKeyValidator.validate("a:b:c", true)).doesNotThrowAnyException();
    }

    @Test
    void rejectsLeadingOrTrailingColon() {
        assertThatThrownBy(() -> RedisKeyValidator.validate(":order", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisKeyValidator.validate("order:", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisKeyValidator.validate(":", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonStrictModeOnlyWarns() {
        // 同一违规 key：strict 抛异常，非 strict 放行（WARN 日志不落断言，属日志框架行为）
        assertThatThrownBy(() -> RedisKeyValidator.validate("a::b", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> RedisKeyValidator.validate("a::b", false)).doesNotThrowAnyException();
        assertThatCode(() -> RedisKeyValidator.validate(null, false)).doesNotThrowAnyException();
    }
}
