package io.github.ayongbox.baseon.core.tool;

/**
 * 字符串工具，纯 JDK 实现，不依赖 commons-lang3。
 */
public final class StrUtil {

    private StrUtil() {
    }

    public static boolean isBlank(CharSequence text) {
        if (text == null || text.length() == 0) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence text) {
        return !isBlank(text);
    }

    public static String defaultIfBlank(CharSequence text, String defaultValue) {
        return isBlank(text) ? defaultValue : text.toString();
    }
}
