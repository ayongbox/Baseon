package io.github.ayongbox.baseon.starter.oss.minio.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.spi.ObjectInfo;
import io.github.ayongbox.baseon.core.spi.ObjectStorageService;
import io.github.ayongbox.baseon.core.tool.StrUtil;
import io.github.ayongbox.baseon.starter.oss.minio.config.BaseonOssProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.InputStream;
import java.time.Duration;

/**
 * {@link ObjectStorageService} 的 MinIO/S3 实现。
 *
 * <p>所有 S3 兼容端点（MinIO / AWS S3 / 阿里云 OSS / 腾讯云 COS / 华为云 OBS）
 * 共用同一套调用，差异仅在 endpoint/凭据/region 配置。
 *
 * <p>异常语义：对象不存在抛 {@code BizException(NOT_FOUND)}；其余服务端/网络异常
 * 统一包装为 {@code BizException(REMOTE_CALL_FAILED)}。
 */
public class MinioObjectStorageService implements ObjectStorageService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(MinioObjectStorageService.class);

    /** 流式上传分片大小 10MiB（S3 分片下限 5MiB），未知长度流按分片累积上传 */
    private static final long PART_SIZE_BYTES = 10L * 1024 * 1024;

    /** 未知长度上传时 ObjectInfo.size 的占位值 */
    private static final long UNKNOWN_SIZE = -1L;

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /** S3 对象不存在的错误码 */
    private static final String CODE_NO_SUCH_KEY = "NoSuchKey";

    private final MinioClient client;
    private final BaseonOssProperties properties;

    public MinioObjectStorageService(MinioClient client, BaseonOssProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (properties.isAutoCreateBucket() && StrUtil.isNotBlank(properties.getDefaultBucket())) {
            String bucket = properties.getDefaultBucket();
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("OSS 默认桶不存在，已自动创建: {}", bucket);
            }
        }
    }

    @Override
    public ObjectInfo upload(String bucket, String objectKey, InputStream in, String contentType) {
        String targetBucket = resolveBucket(bucket);
        String type = StrUtil.isBlank(contentType) ? DEFAULT_CONTENT_TYPE : contentType;
        try {
            io.minio.ObjectWriteResponse response = client.putObject(PutObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectKey)
                    .stream(in, UNKNOWN_SIZE, PART_SIZE_BYTES)
                    .contentType(type)
                    .build());
            return new ObjectInfo(targetBucket, objectKey, UNKNOWN_SIZE, response.etag(), type);
        } catch (Exception e) {
            throw failure("上传对象", e);
        }
    }

    @Override
    public InputStream download(String bucket, String objectKey) {
        String targetBucket = resolveBucket(bucket);
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectKey)
                    .build());
        } catch (ErrorResponseException e) {
            throw translateNotFound(e, objectKey);
        } catch (Exception e) {
            throw failure("下载对象", e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        String targetBucket = resolveBucket(bucket);
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw failure("删除对象", e);
        }
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        String targetBucket = resolveBucket(bucket);
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectKey)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if (isNoSuchKey(e)) {
                return false;
            }
            throw failure("查询对象", e);
        } catch (Exception e) {
            throw failure("查询对象", e);
        }
    }

    @Override
    public String presignedUrl(String bucket, String objectKey, Duration expiry) {
        String targetBucket = resolveBucket(bucket);
        long expirySeconds = expiry == null ? properties.getPresignedExpirySeconds() : expiry.getSeconds();
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(targetBucket)
                    .object(objectKey)
                    .expiry((int) expirySeconds)
                    .build());
        } catch (Exception e) {
            throw failure("生成预签名 URL", e);
        }
    }

    /** 桶解析：显式传参优先，缺省回落到默认桶，两者皆空视为配置错误 */
    private String resolveBucket(String bucket) {
        if (StrUtil.isNotBlank(bucket)) {
            return bucket;
        }
        if (StrUtil.isNotBlank(properties.getDefaultBucket())) {
            return properties.getDefaultBucket();
        }
        throw new BizException(CommonErrorCode.PARAM_INVALID,
                "未指定存储桶且未配置 baseon.oss.minio.default-bucket");
    }

    private BizException translateNotFound(ErrorResponseException e, String objectKey) {
        if (isNoSuchKey(e)) {
            return new BizException(CommonErrorCode.NOT_FOUND, "对象不存在: " + objectKey);
        }
        return failure("访问对象", e);
    }

    private boolean isNoSuchKey(ErrorResponseException e) {
        return e.errorResponse() != null && CODE_NO_SUCH_KEY.equals(e.errorResponse().code());
    }

    private BizException failure(String action, Exception e) {
        return new BizException(CommonErrorCode.REMOTE_CALL_FAILED, "OSS " + action + "失败: " + e.getMessage());
    }
}
