package io.github.ayongbox.baseon.starter.oss.minio.config;

import io.github.ayongbox.baseon.core.spi.ObjectStorageService;
import io.github.ayongbox.baseon.core.tool.StrUtil;
import io.github.ayongbox.baseon.starter.oss.minio.support.MinioObjectStorageService;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MinIO/S3 对象存储装配。
 *
 * <p>未配置 {@code baseon.oss.minio.endpoint} 时整体不装配；业务声明自己的
 * {@link ObjectStorageService} Bean 即可替换默认实现（如对接非 S3 协议存储）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "baseon.oss.minio", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BaseonOssProperties.class)
public class BaseonOssAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.oss.minio", name = "endpoint")
    public MinioClient minioClient(BaseonOssProperties properties) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey());
        if (StrUtil.isNotBlank(properties.getRegion())) {
            builder.region(properties.getRegion());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "baseon.oss.minio", name = "endpoint")
    public ObjectStorageService objectStorageService(MinioClient minioClient, BaseonOssProperties properties) {
        return new MinioObjectStorageService(minioClient, properties);
    }
}
