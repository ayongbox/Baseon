package io.github.ayongbox.baseon.starter.redis.support;

import io.github.ayongbox.baseon.core.tool.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Redis key 规范校验工具（无状态静态方法，与 core 的 StrUtil 同风格）。
 *
 * <p>规范约束：非空白、长度不超过 {@value #MAX_LENGTH}、字符仅允许字母数字与
 * {@code : _ - .} 四种符号、禁止连续两个及以上冒号、禁止以冒号开头或结尾。
 * 统一约束保证 key 可读、可治理（按冒号分段做命名空间统计与扫描）。
 *
 * <p>strict=true 违规直接抛 {@link IllegalArgumentException}（快速失败）；
 * strict=false 仅打印 WARN 放行，供存量系统平滑接入。
 */
public final class RedisKeyValidator {

    private static final Logger log = LoggerFactory.getLogger(RedisKeyValidator.class);

    /** key 最大长度（拼接前缀后的完整 key 一并受限） */
    public static final int MAX_LENGTH = 128;

    /** 允许的字符集：字母、数字、冒号、下划线、点、中划线 */
    private static final Pattern ALLOWED_CHARS = Pattern.compile("^[a-zA-Z0-9:_.\\-]+$");

    /** 连续两个及以上冒号（空分段会破坏命名空间语义） */
    private static final Pattern CONSECUTIVE_COLONS = Pattern.compile(":{2,}");

    private RedisKeyValidator() {
    }

    /**
     * 校验 key 是否符合规范。
     *
     * @param key    待校验的完整 key（调用方需先拼接全局前缀）
     * @param strict true 违规抛异常；false 仅 WARN 放行
     * @throws IllegalArgumentException strict 模式下 key 违规
     */
    public static void validate(String key, boolean strict) {
        String violation = findViolation(key);
        if (violation == null) {
            return;
        }
        if (strict) {
            throw new IllegalArgumentException(violation);
        }
        log.warn("[baseon-redis] Redis key 不规范，已放行：{}", violation);
    }

    /**
     * 返回违规原因，合规则返回 null。独立成私有方法让 validate 只做策略分发。
     */
    private static String findViolation(String key) {
        if (StrUtil.isBlank(key)) {
            return "key 不能为空或全为空白字符";
        }
        if (key.length() > MAX_LENGTH) {
            return "key 长度超过 " + MAX_LENGTH + "（实际 " + key.length() + "）";
        }
        if (!ALLOWED_CHARS.matcher(key).matches()) {
            return "key 含非法字符（仅允许字母数字与 : _ - .）：" + key;
        }
        if (CONSECUTIVE_COLONS.matcher(key).find()) {
            return "key 禁止连续两个及以上冒号：" + key;
        }
        if (key.startsWith(":") || key.endsWith(":")) {
            return "key 禁止以冒号开头或结尾：" + key;
        }
        return null;
    }
}
