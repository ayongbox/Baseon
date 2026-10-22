package io.github.ayongbox.baseon.starter.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Web 基础能力配置，前缀 {@code baseon.web}。
 */
@ConfigurationProperties(prefix = "baseon.web")
public class BaseonWebProperties {

    private final Jackson jackson = new Jackson();
    private final Cors cors = new Cors();
    private final RequestLog requestLog = new RequestLog();
    private final UserContext userContext = new UserContext();

    public Jackson getJackson() {
        return jackson;
    }

    public Cors getCors() {
        return cors;
    }

    public RequestLog getRequestLog() {
        return requestLog;
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public static class Jackson {

        /** Long 序列化为字符串，避免前端 JS 精度丢失 */
        private boolean longToString = true;

        /** java.time 日期时间格式 */
        private String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";

        /** java.time 日期格式 */
        private String dateFormat = "yyyy-MM-dd";

        /** java.time 时间格式 */
        private String timeFormat = "HH:mm:ss";

        public boolean isLongToString() {
            return longToString;
        }

        public void setLongToString(boolean longToString) {
            this.longToString = longToString;
        }

        public String getDateTimeFormat() {
            return dateTimeFormat;
        }

        public void setDateTimeFormat(String dateTimeFormat) {
            this.dateTimeFormat = dateTimeFormat;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }

        public String getTimeFormat() {
            return timeFormat;
        }

        public void setTimeFormat(String timeFormat) {
            this.timeFormat = timeFormat;
        }
    }

    public static class Cors {

        /** 为空表示不开启跨域；生产环境禁止使用 * 与 allowCredentials 组合 */
        private List<String> allowedOrigins = new ArrayList<>();

        private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

        private List<String> exposedHeaders = new ArrayList<>();

        private boolean allowCredentials = false;

        private long maxAge = 3600;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    public static class RequestLog {

        private boolean enabled = true;

        /** 请求/响应体最大打印长度，超出截断 */
        private int maxBodyLength = 1000;

        /** 不打印请求体的路径，默认放过健康检查 */
        private List<String> excludePaths = new ArrayList<>(List.of("/actuator/**"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxBodyLength() {
            return maxBodyLength;
        }

        public void setMaxBodyLength(int maxBodyLength) {
            this.maxBodyLength = maxBodyLength;
        }

        public List<String> getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(List<String> excludePaths) {
            this.excludePaths = excludePaths;
        }
    }

    public static class UserContext {

        private boolean enabled = true;

        private String userIdHeader = "X-User-Id";

        private String tenantIdHeader = "X-Tenant-Id";

        private String usernameHeader = "X-Username";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUserIdHeader() {
            return userIdHeader;
        }

        public void setUserIdHeader(String userIdHeader) {
            this.userIdHeader = userIdHeader;
        }

        public String getTenantIdHeader() {
            return tenantIdHeader;
        }

        public void setTenantIdHeader(String tenantIdHeader) {
            this.tenantIdHeader = tenantIdHeader;
        }

        public String getUsernameHeader() {
            return usernameHeader;
        }

        public void setUsernameHeader(String usernameHeader) {
            this.usernameHeader = usernameHeader;
        }
    }
}
