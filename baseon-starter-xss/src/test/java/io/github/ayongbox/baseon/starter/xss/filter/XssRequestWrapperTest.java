package io.github.ayongbox.baseon.starter.xss.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * XssRequestWrapper 的参数/多值参数/请求头转义测试。
 */
class XssRequestWrapperTest {

    @Test
    void escapesParameterValuesAndHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/demo/xss/echo");
        request.addParameter("name", "<script>alert(1)</script>");
        request.addParameter("tags", "<b>", "a&b");
        request.addHeader("X-Custom", "\"><img src=x>");

        XssRequestWrapper wrapper = new XssRequestWrapper(request);

        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", wrapper.getParameter("name"));
        assertArrayEquals(new String[]{"&lt;b&gt;", "a&amp;b"}, wrapper.getParameterValues("tags"));
        assertEquals("&quot;&gt;&lt;img src=x&gt;", wrapper.getHeader("X-Custom"));
        // 不存在的参数/请求头保持 null，不引入空串
        assertNull(wrapper.getParameter("missing"));
        assertNull(wrapper.getParameterValues("missing"));
        assertNull(wrapper.getHeader("X-Missing"));
    }
}
