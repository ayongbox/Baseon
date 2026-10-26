package io.github.ayongbox.baseon.starter.xss.filter;

import io.github.ayongbox.baseon.starter.xss.config.BaseonXssProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * XssFilter 的放行/包装分支测试：排除路径与开关关闭时原样放行，
 * 普通路径包装为 XssRequestWrapper。
 */
class XssFilterTest {

    private BaseonXssProperties properties;
    private XssFilter filter;

    @BeforeEach
    void setUp() {
        properties = new BaseonXssProperties();
        filter = new XssFilter(properties);
    }

    @Test
    void excludedPathPassesThroughWithoutWrapping() throws Exception {
        properties.setExcludePaths(List.of("/demo/rich-text/**"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/demo/rich-text/save");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // 原请求对象直接透传，未包装
        assertSame(request, chain.getRequest());
    }

    @Test
    void disabledFilterPassesThroughWithoutWrapping() throws Exception {
        properties.setEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/demo/xss/echo");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertSame(request, chain.getRequest());
    }

    @Test
    void normalPathWrapsRequestAndEscapesParameters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/demo/xss/echo");
        request.addParameter("keyword", "<script>alert(1)</script>");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // 下游拿到的是包装器，读取参数即得转义结果
        assertInstanceOf(XssRequestWrapper.class, chain.getRequest());
        XssRequestWrapper wrapped = (XssRequestWrapper) chain.getRequest();
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", wrapped.getParameter("keyword"));
    }
}
