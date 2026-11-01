package io.github.ayongbox.baseon.core.spi;

/**
 * 对象存储的对象信息：{@link ObjectStorageService#upload} 的返回载体。
 *
 * <p>流式上传（长度未知）时 {@link #getSize()} 为 -1，表示实现侧无法预知大小；
 * 需要精确大小请在上传后调用 stat 类能力（由各实现扩展）或在上传前自行记录。
 */
public class ObjectInfo {

    private final String bucket;
    private final String objectKey;
    private final long size;
    private final String etag;
    private final String contentType;

    public ObjectInfo(String bucket, String objectKey, long size, String etag, String contentType) {
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.size = size;
        this.etag = etag;
        this.contentType = contentType;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }

    /** 对象字节数；流式上传返回 -1 表示未知 */
    public long getSize() {
        return size;
    }

    public String getEtag() {
        return etag;
    }

    public String getContentType() {
        return contentType;
    }
}
