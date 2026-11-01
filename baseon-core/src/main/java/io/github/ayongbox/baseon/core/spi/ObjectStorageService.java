package io.github.ayongbox.baseon.core.spi;

import java.io.InputStream;
import java.time.Duration;

/**
 * 对象存储服务 SPI：业务侧唯一文件存取入口，默认实现由 starter-oss-minio 装配。
 *
 * <p>bucket 参数的约定：传 {@code null} 或空白串时使用实现侧配置的默认桶；
 * 既未指定桶也未配置默认桶时抛 {@code BizException(PARAM_INVALID)}。
 *
 * <p>S3 协议本身即聚合层：默认实现（MinIO SDK）可直连 AWS S3、阿里云 OSS、
 * 腾讯云 COS、华为云 OBS 等所有 S3 兼容端点，更换厂商只需改配置；
 * 非 S3 协议的存储由业务实现本接口注册 Bean 覆盖默认装配。
 */
public interface ObjectStorageService {

    /**
     * 流式上传：按分片策略读取流，不要求预知长度；覆盖同名对象。
     *
     * @param bucket      目标桶，null 用默认桶
     * @param objectKey   对象键（路径风格，如 {@code "2026/11/attachment.zip"}）
     * @param in          输入流，调用方负责关闭
     * @param contentType 内容类型，null 时由实现按 application/octet-stream 处理
     * @return 对象信息（流式上传 size 为 -1）
     */
    ObjectInfo upload(String bucket, String objectKey, InputStream in, String contentType);

    /** 便捷上传：使用默认桶 */
    default ObjectInfo upload(String objectKey, InputStream in, String contentType) {
        return upload(null, objectKey, in, contentType);
    }

    /**
     * 下载：返回对象输入流，<b>调用方必须关闭</b>（连接随流释放）。
     * 对象不存在时抛 {@code BizException(NOT_FOUND)}。
     *
     * @param bucket    目标桶，null 用默认桶
     * @param objectKey 对象键
     */
    InputStream download(String bucket, String objectKey);

    /** 便捷下载：使用默认桶 */
    default InputStream download(String objectKey) {
        return download(null, objectKey);
    }

    /**
     * 删除对象；删除不存在的对象按成功处理（对象存储语义天然幂等）。
     *
     * @param bucket    目标桶，null 用默认桶
     * @param objectKey 对象键
     */
    void delete(String bucket, String objectKey);

    /** 便捷删除：使用默认桶 */
    default void delete(String objectKey) {
        delete(null, objectKey);
    }

    /**
     * 判断对象是否存在。
     *
     * @param bucket    目标桶，null 用默认桶
     * @param objectKey 对象键
     */
    boolean exists(String bucket, String objectKey);

    /** 便捷判断：使用默认桶 */
    default boolean exists(String objectKey) {
        return exists(null, objectKey);
    }

    /**
     * 生成下载预签名 URL：在有效期内无需凭据即可直接 GET 下载，
     * 适用于浏览器直链、第三方回源等场景。
     *
     * @param bucket    目标桶，null 用默认桶
     * @param objectKey 对象键
     * @param expiry    有效期；null 时使用实现侧默认（baseon.oss.minio.presigned-expiry-seconds）
     */
    String presignedUrl(String bucket, String objectKey, Duration expiry);

    /** 便捷预签名：使用默认桶 */
    default String presignedUrl(String objectKey, Duration expiry) {
        return presignedUrl(null, objectKey, expiry);
    }
}
