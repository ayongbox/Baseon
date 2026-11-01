# baseon-starter-oss-minio · 对象存储抽象（core.spi）的 MinIO/S3 实现

## 定位

统一对象存储能力底座。core.spi 定义与厂商无关的契约 `ObjectStorageService`，本模块用 MinIO SDK 8.5.17 实现。MinIO SDK 走标准 S3 协议，而 AWS S3、阿里云 OSS、腾讯 COS、华为 OBS 均 S3 兼容——**S3 协议本身就是聚合层**，切换厂商只需改 endpoint 与凭据，代码零改动。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-oss-minio</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `MinioClient` | S3 客户端（按 endpoint/凭据/region 构建） | 配置了 `endpoint` 才装配 |
| `MinioObjectStorageService` | `ObjectStorageService` 契约实现 | 同上，`@ConditionalOnMissingBean` |

注册方式：`AutoConfiguration.imports`。`baseon.oss.minio.enabled=false` 或未配置 `endpoint` 时整体不装配（视为未启用对象存储）。

## 配置项

```yaml
baseon:
  oss:
    minio:
      enabled: true                         # 总开关
      endpoint: http://127.0.0.1:9000       # 唯一必填，S3 端点（含协议与端口）
      access-key: minioadmin                # 访问凭据
      secret-key: minioadmin
      region:                               # MinIO 可留空；AWS S3 与部分云厂商必填
      default-bucket: app-files             # 默认桶：接口 bucket 传 null 时使用
      auto-create-bucket: false             # 启动时自动建默认桶：仅本地/私有化联调开启
      presigned-expiry-seconds: 3600        # 预签名 URL 默认有效期
```

接入云厂商示例（阿里云 OSS，其他厂商同构）：

```yaml
baseon:
  oss:
    minio:
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      region: oss-cn-hangzhou
      access-key: ${ALIYUN_AK}
      secret-key: ${ALIYUN_SK}
      default-bucket: my-prod-bucket
```

## 使用

注入 core 契约 `ObjectStorageService`（不要直接注入 MinioClient）：

```java
// 上传（bucket 传 null 走默认桶）；返回 ObjectInfo（bucket/objectKey/size/etag/contentType）
ObjectInfo info = oss.upload("avatar/u1001.png", inputStream, "image/png");

// 下载：不存在抛 BizException(NOT_FOUND)；返回流需调用方关闭
InputStream in = oss.download("avatar/u1001.png");

// 存在性 / 删除
boolean exists = oss.exists("avatar/u1001.png");
oss.delete("avatar/u1001.png");

// 预签名 URL：前端直传/直下，expiry 传 null 用默认有效期
String url = oss.presignedUrl("avatar/u1001.png", Duration.ofMinutes(10));

// 显式指定桶（覆盖默认桶）
oss.upload("other-bucket", "doc/a.pdf", in, "application/pdf");
```

- 上传为流式（`InputStream`），大小未知按 `-1 + 10MiB` 分片，不把文件全量读进内存；
- `default-bucket` 未配置且接口 bucket 传 null 时，调用抛 `BizException(PARAM_INVALID)`；
- 除 download 的 NOT_FOUND 外，其余 S3 异常统一包装为 `BizException(REMOTE_CALL_FAILED)`。

## 注意事项

1. **`auto-create-bucket` 仅限联调环境**：生产桶应由运维预创建并完成权限/生命周期配置；自动建桶需要凭据有建桶权限，生产凭据通常不应授予。
2. **预签名 URL 即临时授权**：任何拿到 URL 的人在有效期内都可访问，有效期按业务最小化设置，敏感文件配合防盗链或审计。
3. **download 返回的是连接流**：必须 try-with-resources 关闭，否则连接泄漏；大文件下载建议直接给前端预签名 URL，不经过应用中转。
4. **对象 key 自行规划层级**：用 `/` 分层（如 `租户/业务/日期/文件名`），便于生命周期规则与清单导出；key 内避免中文与特殊字符。
5. **厂商切换只改配置**：endpoint/region/凭据三要素；个别厂商的归档存储、自定义域名等特性不在抽象契约内，需要时业务自行扩展实现并用 `@ConditionalOnMissingBean` 覆盖默认 Bean。
6. **凭据不要写死在 yml**：生产用 Nacos + Jasypt（starter-cloud-nacos）或环境变量注入。
