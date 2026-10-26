package io.github.ayongbox.baseon.starter.xss.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * XssEscapeUtil 的字符覆盖与边界值测试。
 */
class XssEscapeUtilTest {

    @Test
    void escapesAllSpecialCharacters() {
        // 六个危险字符全部转为 HTML 实体，且 & 不被二次转义
        assertEquals("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;",
                XssEscapeUtil.escape("<script>alert(\"x\")</script>"));
        assertEquals("&amp; &lt; &gt; &quot; &#x27; &#x60;",
                XssEscapeUtil.escape("& < > \" ' `"));
        // 已有实体不被破坏：&amp; 中的 & 会被转义一次，结果是 &amp;amp;
        assertEquals("&amp;amp;", XssEscapeUtil.escape("&amp;"));
    }

    @Test
    void nullAndEmptyValuesAreReturnedAsIs() {
        assertNull(XssEscapeUtil.escape(null));
        assertSame("", XssEscapeUtil.escape(""));
        // 无危险字符的字符串原样返回（内容相等即可）
        assertEquals("hello world", XssEscapeUtil.escape("hello world"));
    }
}
