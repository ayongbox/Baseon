package io.github.ayongbox.baseon.starter.oss.minio.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.spi.ObjectInfo;
import io.github.ayongbox.baseon.starter.oss.minio.config.BaseonOssProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MinIO 实现：默认桶解析、上传参数、异常语义（NoSuchKey → NOT_FOUND / false）。
 */
class MinioObjectStorageServiceTest {

    private final MinioClient client = mock(MinioClient.class);
    private final BaseonOssProperties properties = new BaseonOssProperties();
    private final MinioObjectStorageService service = new MinioObjectStorageService(client, properties);

    @Test
    void uploadShouldUseDefaultBucketWhenBucketOmitted() throws Exception {
        properties.setDefaultBucket("attachment");
        ObjectWriteResponse response = mock(ObjectWriteResponse.class);
        when(response.etag()).thenReturn("etag-1");
        when(client.putObject(any(PutObjectArgs.class))).thenReturn(response);

        ObjectInfo info = service.upload("a/b.txt",
                new ByteArrayInputStream("hi".getBytes(StandardCharsets.UTF_8)), "text/plain");

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("attachment");
        assertThat(captor.getValue().object()).isEqualTo("a/b.txt");
        assertThat(info.getEtag()).isEqualTo("etag-1");
        assertThat(info.getSize()).as("流式上传长度未知").isEqualTo(-1L);
    }

    @Test
    void explicitBucketShouldOverrideDefault() throws Exception {
        properties.setDefaultBucket("attachment");
        ObjectWriteResponse response = mock(ObjectWriteResponse.class);
        when(client.putObject(any(PutObjectArgs.class))).thenReturn(response);

        service.upload("other", "k", new ByteArrayInputStream(new byte[0]), null);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("other");
    }

    @Test
    void shouldFailWhenNeitherBucketNorDefaultConfigured() {
        assertThatThrownBy(() -> service.upload("k", new ByteArrayInputStream(new byte[0]), null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(CommonErrorCode.PARAM_INVALID.getCode()));
    }

    @Test
    void existsShouldReturnFalseWhenNoSuchKey() throws Exception {
        ErrorResponseException notFound = noSuchKey();
        when(client.statObject(any(StatObjectArgs.class))).thenThrow(notFound);

        assertThat(service.exists("bucket", "missing")).isFalse();
    }

    @Test
    void existsShouldReturnTrueWhenStatSucceeds() throws Exception {
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(null);

        assertThat(service.exists("bucket", "found")).isTrue();
    }

    @Test
    void downloadShouldTranslateNoSuchKeyToNotFound() throws Exception {
        ErrorResponseException notFound = noSuchKey();
        when(client.getObject(any(io.minio.GetObjectArgs.class))).thenThrow(notFound);

        assertThatThrownBy(() -> service.download("bucket", "missing"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(CommonErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void deleteShouldDelegate() throws Exception {
        service.delete("bucket", "k");
        verify(client).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void presignedUrlShouldUseConfiguredDefaultExpiry() throws Exception {
        properties.setPresignedExpirySeconds(600);
        when(client.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("http://signed");

        String url = service.presignedUrl("bucket", "k", null);

        ArgumentCaptor<GetPresignedObjectUrlArgs> captor = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(client).getPresignedObjectUrl(captor.capture());
        assertThat(url).isEqualTo("http://signed");
        assertThat(captor.getValue().expiry()).isEqualTo(600);
    }

    @Test
    void presignedUrlShouldRespectExplicitExpiry() throws Exception {
        when(client.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("http://signed");

        service.presignedUrl("bucket", "k", Duration.ofMinutes(10));

        ArgumentCaptor<GetPresignedObjectUrlArgs> captor = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(client).getPresignedObjectUrl(captor.capture());
        assertThat(captor.getValue().expiry()).isEqualTo(600);
    }

    @Test
    void clientFailureShouldWrapAsRemoteCallFailed() throws Exception {
        when(client.statObject(any(StatObjectArgs.class)))
                .thenThrow(new RuntimeException("connection reset"));

        assertThatThrownBy(() -> service.exists("bucket", "k"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(CommonErrorCode.REMOTE_CALL_FAILED.getCode()));
    }

    private static ErrorResponseException noSuchKey() {
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        ErrorResponseException exception = mock(ErrorResponseException.class);
        when(exception.errorResponse()).thenReturn(errorResponse);
        return exception;
    }
}
