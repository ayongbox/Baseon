package io.github.ayongbox.baseon.starter.web.log;

import io.github.ayongbox.baseon.starter.web.config.BaseonWebProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 请求日志过滤器：输出去敏后的出入参、状态码与耗时。
 *
 * <p>仅缓存文本类请求体；multipart 与二进制流只记录访问日志，避免大文件占用内存。
 * 响应体通过 {@link ContentCachingResponseWrapper} 缓存后必须 copyBodyToResponse，
 * 否则客户端会收到空响应。
 */
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    private final BaseonWebProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequestLogFilter(BaseonWebProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        BaseonWebProperties.RequestLog config = properties.getRequestLog();
        if (!config.isEnabled() || isExcluded(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        if (!isTextRequest(request)) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                log.info("http method={} uri={} status={} cost={}ms request=- response=- (binary)",
                        request.getMethod(), request.getRequestURI(), response.getStatus(),
                        System.currentTimeMillis() - start);
            }
            return;
        }

        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            try {
                String requestBody = LogSanitizer.sanitize(
                        read(cachedRequest.getContentAsByteArray(), request.getCharacterEncoding()),
                        config.getMaxBodyLength());
                String responseBody = LogSanitizer.sanitize(
                        // JSON 响应固定按 UTF-8 解码：Tomcat 的 getCharacterEncoding() 默认返回 ISO-8859-1，
                        // 直接使用会导致日志中文乱码
                        read(cachedResponse.getContentAsByteArray(), StandardCharsets.UTF_8.name()),
                        config.getMaxBodyLength());
                log.info("http method={} uri={} status={} cost={}ms request={} response={}",
                        request.getMethod(), request.getRequestURI(), cachedResponse.getStatus(),
                        System.currentTimeMillis() - start,
                        requestBody.isEmpty() ? "-" : requestBody,
                        responseBody.isEmpty() ? "-" : responseBody);
            } finally {
                cachedResponse.copyBodyToResponse();
            }
        }
    }

    private boolean isExcluded(String uri) {
        return properties.getRequestLog().getExcludePaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    private boolean isTextRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return true;
        }
        String lowerCase = contentType.toLowerCase(Locale.ROOT);
        return !lowerCase.startsWith("multipart/") && !lowerCase.startsWith("application/octet-stream");
    }

    private String read(byte[] content, String encoding) {
        if (content == null || content.length == 0) {
            return "";
        }
        Charset charset = StandardCharsets.UTF_8;
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            } catch (Exception ignored) {
                // 非法编码回退 UTF-8
            }
        }
        return new String(content, charset);
    }
}
