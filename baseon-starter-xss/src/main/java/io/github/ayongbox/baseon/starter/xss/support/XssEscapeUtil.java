package io.github.ayongbox.baseon.starter.xss.support;

/**
 * XSS HTML 转义工具。
 *
 * <p>将 {@code & < > " ' `} 六个危险字符替换为 HTML 实体，破坏脚本与标签结构，
 * 覆盖输出到 HTML 页面时的反射型/存储型 XSS 场景。转义是兜底手段：
 * 富文本、URL 属性、JS 上下文等场景需要业务侧更精细的处理（见模块手册）。
 */
public final class XssEscapeUtil {

    private XssEscapeUtil() {
    }

    /**
     * 对输入做 HTML 转义；null 与空串原样返回，不引入额外分配。
     */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                // & 必须先于其他字符处理，否则会把已转义实体的 & 二次转义
                case '&' -> builder.append("&amp;");
                case '<' -> builder.append("&lt;");
                case '>' -> builder.append("&gt;");
                case '"' -> builder.append("&quot;");
                // ' 与 ` 用于防御属性注入（IE 会把反引号当引号用），用数值实体兼容性最好
                case '\'' -> builder.append("&#x27;");
                case '`' -> builder.append("&#x60;");
                default -> builder.append(c);
            }
        }
        return builder.toString();
    }
}
