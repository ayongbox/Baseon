package io.github.ayongbox.baseon.starter.oss.minio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO/S3 对象存储配置，前缀 {@code baseon.oss.minio}。
 *
 * <p>{@code endpoint} 是唯一必填项：未配置时不装配（视为未启用对象存储）。
 * 接入 S3 兼容云厂商时只需替换 endpoint/凭据，如阿里云 OSS 为
 * {@code https://oss-cn-hangzhou.aliyuncs.com}。
 */
@ConfigurationProperties(prefix = "baseon.oss.minio")
public class BaseonOssProperties {

    /** 总开关：false 时本模块所有 Bean 不装配 */
    private boolean enabled = true;

    /** S3 端点（含协议与端口），如 {@code "http://127.0.0.1:9000"}；为空则不装配 */
    private String endpoint;

    /** 访问凭据 AccessKey */
    private String accessKey;

    /** 访问凭据 SecretKey */
    private String secretKey;

    /** 区域：MinIO 可留空；AWS S3 与部分云厂商必须（如 {@code "oss-cn-hangzhou"}） */
    private String region;

    /** 默认桶：接口 bucket 参数传 null 时使用；为空且未指定桶则调用失败 */
    private String defaultBucket;

    /** 启动时自动创建默认桶（不存在才建）：仅建议本地与私有化联调开启 */
    private boolean autoCreateBucket = false;

    /** 预签名 URL 默认有效期（秒）：接口 expiry 参数传 null 时使用 */
    private long presignedExpirySeconds = 3600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public void setDefaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }

    public boolean isAutoCreateBucket() {
        return autoCreateBucket;
    }

    public void setAutoCreateBucket(boolean autoCreateBucket) {
        this.autoCreateBucket = autoCreateBucket;
    }

    public long getPresignedExpirySeconds() {
        return presignedExpirySeconds;
    }

    public void setPresignedExpirySeconds(long presignedExpirySeconds) {
        this.presignedExpirySeconds = presignedExpirySeconds;
    }
}
