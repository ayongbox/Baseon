# baseon-starter-web · Web 基础能力

## 定位

所有对外提供 HTTP 接口的服务都必须引入。提供「统一返回、统一异常、统一上下文、统一日志」四件事，
业务代码不再重复写 `@RestControllerAdvice`、Jackson 配置和请求日志切面。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-web</artifactId>
</dependency>
```

## 自动装配清单

| Bean | 作用 | 默认行为 |
|---|---|---|
| `GlobalExceptionHandler` | `@RestControllerAdvice` 统一异常处理 | 按 `ErrorType` 映射 HTTP 状态 |
| `UserContextInterceptor` | 解析用户 Header 写入 `SecurityUtil` | 请求结束自动清理 |
| `RequestLogFilter` | 出入参日志、脱敏、截断、耗时 | 默认开启 |
| `WebMvcConfigurer`（匿名） | 注册拦截器与 CORS | CORS 默认关闭 |
| `Jackson2ObjectMapperBuilderCustomizer` | Long → String、时间格式 | 默认开启 |

全部 Bean 带 `@ConditionalOnMissingBean`，业务声明同类型 Bean 即可覆盖。

装配入口：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，
**业务不需要 `@Import` 或 `@ComponentScan`**。

## 配置项

```yaml
baseon:
  web:
    jackson:
      long-to-string: true            # 雪花 ID 按字符串输出，防前端精度丢失
      date-time-format: yyyy-MM-dd HH:mm:ss
      date-format: yyyy-MM-dd
      time-format: HH:mm:ss
    user-context:
      enabled: true
      user-id-header: X-User-Id
      tenant-id-header: X-Tenant-Id
      username-header: X-Username
    request-log:
      enabled: true
      max-body-length: 1000           # 超出截断并追加 ...(truncated)
      exclude-paths:
        - /actuator/**
    cors:
      allowed-origins: []             # 为空表示不开启跨域
      allowed-methods: [GET, POST, PUT, DELETE, PATCH, OPTIONS]
      allowed-headers: ["*"]
      allow-credentials: false
      max-age: 3600
```

## 使用场景

### 场景 1：业务异常自动转换

```java
@GetMapping("/{id}")
public R<UserVO> detail(@PathVariable Long id) {
    UserPO user = userMapper.selectById(id);
    if (user == null) {
        throw new BizException(CommonErrorCode.NOT_FOUND, "用户不存在");
    }
    return R.ok(userService.toVO(user));
}
```

客户端收到：

```
HTTP/1.1 404
{"code":1004,"msg":"用户不存在","data":null}
```

业务代码不写 `try-catch`，也不决定 HTTP 状态码。

### 场景 2：覆盖默认异常处理

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomExceptionHandler {
    @ExceptionHandler(StockException.class)
    public ResponseEntity<R<Void>> handle(StockException e) { ... }
}
```

### 场景 3：读取网关注入的用户信息

```java
@GetMapping("/mine")
public R<List<OrderVO>> myOrders() {
    Long userId = SecurityUtil.getUserId();   // 由 X-User-Id 解析
    return R.ok(orderService.listByUser(userId));
}
```

请求链路：网关校验 JWT → 注入 `X-User-Id` / `X-Tenant-Id` → 本 Starter 写入 ThreadLocal → 业务读取。

### 场景 4：大文件上传不被日志拖累

`RequestLogFilter` 检测到 `multipart/*` 或 `application/octet-stream` 时只记录一行访问日志，
不缓存请求体，避免大文件占用堆内存。无需额外配置。

### 场景 5：关闭请求日志

```yaml
baseon:
  web:
    request-log:
      enabled: false
```

生产环境如需接入统一网关日志，可关闭本地日志以降低磁盘开销。

## 注意事项

1. **用户 Header 必须由网关注入**。服务直连公网时任何人都能伪造身份，这是安全红线。
2. **CORS 默认关闭**，需要跨域时显式配置 `allowed-origins`；不要使用 `*` 搭配 `allow-credentials: true`。
3. **脱敏是兜底不是保障**：正则覆盖 `password/token/mobile/idCard` 等常见字段与 `Bearer` 头，
   真正的敏感数据治理依赖字段级加密。
4. **未知异常统一返回 500 + 通用文案**，堆栈只进日志不出响应体，避免泄露内部地址与实现细节。
5. **异步任务拿不到上下文**：拦截器只负责当前请求线程，异步场景仍需 `ContextWrapper`。
