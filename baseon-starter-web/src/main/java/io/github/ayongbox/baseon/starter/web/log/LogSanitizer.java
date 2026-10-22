package io.github.ayongbox.baseon.starter.web.log;

import java.util.regex.Pattern;

/**
 * 日志脱敏与截断工具。
 *
 * <p>只做「明显敏感字段」的正则替换，不追求完整覆盖；真正的敏感数据治理
 * 依赖字段级加密与日志平台规则。宁可多打码，不可漏打码。
 */
public final class LogSanitizer {

    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(\"(?:password|passwd|pwd|token|accessToken|refreshToken|secret|idCard|idNo|mobile|phone)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BEARER_TOKEN = Pattern.compile("(Bearer\\s+)[A-Za-z0-9\\-._~+/]+=*");

    private static final String MASK = "\"***\"";

    private static final String TRUNCATED_SUFFIX = "...(truncated)";

    private LogSanitizer() {
    }

    public static String sanitize(String body, int maxLength) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        String masked = BEARER_TOKEN.matcher(SENSITIVE_JSON_FIELD.matcher(body).replaceAll("$1" + MASK))
                .replaceAll("$1***");
        if (maxLength > 0 && masked.length() > maxLength) {
            return masked.substring(0, maxLength) + TRUNCATED_SUFFIX;
        }
        return masked;
    }
}
