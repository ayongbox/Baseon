package io.github.ayongbox.baseon.starter.xss.filter;

import io.github.ayongbox.baseon.starter.xss.support.XssEscapeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * XSS 请求包装器：对读取到的请求参数与请求头逐个做 HTML 转义。
 *
 * <p>只覆盖 query/form 参数与 Header 两条入口；JSON 请求体由
 * {@code XssStringDeserializer} 在 Jackson 反序列化阶段处理，二者互补。
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    public XssRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        return XssEscapeUtil.escape(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] escaped = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            escaped[i] = XssEscapeUtil.escape(values[i]);
        }
        return escaped;
    }

    @Override
    public String getHeader(String name) {
        return XssEscapeUtil.escape(super.getHeader(name));
    }
}
