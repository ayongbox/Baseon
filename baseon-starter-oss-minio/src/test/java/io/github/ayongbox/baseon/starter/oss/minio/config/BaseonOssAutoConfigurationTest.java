package io.github.ayongbox.baseon.starter.oss.minio.config;

import io.github.ayongbox.baseon.core.spi.ObjectStorageService;
import io.github.ayongbox.baseon.starter.oss.minio.support.MinioObjectStorageService;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 装配条件：endpoint 未配置不装配、enabled=false 不装配、默认装配走 MinIO 实现。
 */
class BaseonOssAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseonOssAutoConfiguration.class));

    @Test
    void shouldNotAssembleWhenEndpointMissing() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(MinioClient.class);
            assertThat(context).doesNotHaveBean(ObjectStorageService.class);
        });
    }

    @Test
    void shouldNotAssembleWhenDisabled() {
        runner.withPropertyValues(
                "baseon.oss.minio.enabled=false",
                "baseon.oss.minio.endpoint=http://127.0.0.1:9000")
                .run(context -> assertThat(context).doesNotHaveBean(ObjectStorageService.class));
    }

    @Test
    void shouldAssembleMinioImplementationWhenConfigured() {
        runner.withPropertyValues(
                "baseon.oss.minio.endpoint=http://127.0.0.1:9000",
                "baseon.oss.minio.access-key=minioadmin",
                "baseon.oss.minio.secret-key=minioadmin",
                "baseon.oss.minio.default-bucket=attachment")
                .run(context -> {
                    // MinioClient 仅构建连接配置，不发起网络请求
                    assertThat(context).hasSingleBean(MinioClient.class);
                    assertThat(context).hasSingleBean(ObjectStorageService.class);
                    assertThat(context.getBean(ObjectStorageService.class))
                            .isInstanceOf(MinioObjectStorageService.class);
                });
    }
}
