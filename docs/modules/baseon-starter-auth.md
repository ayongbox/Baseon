# baseon-starter-auth · JWT 鉴权（双 Token + Redis 吊销 + 权限注解）

## 定位

认证鉴权能力底座，core.secure 契约的默认实现。`AuthTokenFilter` 校验 Bearer Token 并写入登录上下文，`@RequiresPermission` 切面做方法级鉴权，refresh token 会话与 access 黑名单托管在 Redis（可吊销、可登出）。

能力边界：本 starter 只负责「凭证签发/校验/刷新/吊销 + 权限串比对」。用户体系、登录密码校验、角色权限模型属于业务域，不在本模块；登录接口由业务自己写（调用 `TokenService.issueTokenPair` 即可）。

引入方式（版本由 BOM 管控，依赖 baseon-starter-web 与 baseon-starter-redis）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-auth</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `JwtTokenProvider` | HS256 签发/解析 JWT，claims 含 userId/username/tenantId/perms/jti/type/sid | 默认装配；`secret` 缺失或不足 32 字节时启动快速失败 |
| `TokenService` | `AuthProvider` 默认实现：双 Token 签发、刷新、黑名单校验、登出吊销 | 以 `AuthProvider` 注册；业务声明自己的 `AuthProvider` Bean 时整体退避 |
| `DefaultPermissionChecker` | `PermissionChecker` 默认实现：比对当前登录用户 Token 中的权限集合 | 默认装配，业务可覆盖（接权限中心等） |
| `RequiresPermissionAspect` | `@RequiresPermission` 切面：未登录 1002，权限不足 1003 | 默认装配 |
| `AuthTokenFilter` | 解析 Bearer Token → 校验 → 写 `SecurityUtil`；失败自写 401 JSON | `baseon.auth.enabled=false` 时不注册；order = `HIGHEST_PRECEDENCE + 20` |

注册方式：`AutoConfiguration.imports`。

## 配置项

```yaml
baseon:
  auth:
    enabled: true                          # 总开关：false 时过滤器不注册，全链路免认证
    secret: ${JWT_SECRET}                  # HS256 密钥，必填，UTF-8 编码后至少 32 字节
    access-expire-seconds: 7200            # access token 有效期，默认 2 小时
    refresh-expire-seconds: 604800         # refresh token 有效期，默认 7 天
    ignore-paths:                          # 不校验 Token 的放行路径（Ant 风格）
      - /auth/login
      - /auth/refresh
    blacklist-key-prefix: "auth:blacklist:"  # access 黑名单 Redis key 前缀
    refresh-key-prefix: "auth:refresh:"      # refresh 会话 Redis key 前缀
```

## 快速上手

### 登录签发双 Token

业务自己实现账号密码校验，通过后签发：

```java
@PostMapping("/auth/login")
public R<TokenPair> login(@RequestBody LoginRequest request) {
    LoginUser user = userService.authenticate(request.getUsername(), request.getPassword());
    // 校验通过后由业务填充 permissions（如从角色表聚合）
    user.setPermissions(Set.of("user:create", "user:list"));
    return R.ok(tokenService.issueTokenPair(user));
}
```

前端之后每次请求携带 `Authorization: Bearer <accessToken>`；access 过期后用 refresh 调 `POST /auth/refresh`（内部走 `AuthProvider.refresh`）换新 access；登出调 `AuthProvider.invalidate(accessToken)`，该 access 立即进黑名单、对应 refresh 会话一并删除。

### 方法级鉴权

```java
@RequiresPermission("user:create")
@PostMapping
public R<Long> create(@RequestBody @Valid UserCreateRequest request) { ... }
```

未登录抛 1002（HTTP 401），已登录但权限不足抛 1003（HTTP 403），均由 starter-web 的 `GlobalExceptionHandler` 统一映射。**未标注解的接口不强制登录**——过滤器对无 Token 请求一律放行，是否拦理由注解决定，免登录接口天然可用。

### 替换认证体系

接入企业 SSO / OAuth2 时，声明自己的 `AuthProvider` Bean 即可，默认 `TokenService` 整体退避，`@RequiresPermission` 与上下文写入逻辑不变：

```java
@Bean
public AuthProvider authProvider() {
    return new SsoAuthProvider(...);
}
```

同理，声明自己的 `PermissionChecker` Bean 可替换权限串比对逻辑（角色继承、数据权限等）。

## 与 starter-web 信任模式的职责分界

两者都能写登录上下文，但信任来源不同，**同时引入时以 starter-auth 为准**：

| | starter-web（信任模式） | starter-auth（认证模式） |
|---|---|---|
| 上下文来源 | 网关注入的 `X-User-Id` 等 Header | Bearer Token 验签后的 claims |
| 安全前提 | 服务不直连公网，Header 已被网关验过 | 自带验签，服务可直连公网 |
| 权限集合 | 不携带（Header 无 perms） | Token claims 携带，`@RequiresPermission` 可用 |

`AuthTokenFilter` 先于 `UserContextInterceptor` 执行：认证成功写入上下文后，拦截器发现 `SecurityUtil.isLogin()` 便跳过 Header 覆盖，不会被伪造 Header 顶掉；请求结束两侧各自 `clear()`，幂等共存。

## 注意事项

1. **secret 必须够长且保密**：不足 32 字节启动直接失败（HS256 的硬性要求，弱密钥等于没有鉴权）；生产环境走环境变量或配置中心，禁止写死在代码库。
2. **Redis 是吊销状态的唯一载体**：refresh 会话与 access 黑名单都依赖 Redis，Redis 不可用等同于「登出/吊销不生效」，需纳入监控；key 自动带 starter-redis 的全局前缀。
3. **黑名单 TTL 只覆盖剩余有效期**：access token 自然过期后黑名单记录自动清出，不膨胀；代价是「登出后过期 token 再被捡拾」在理论上不可感知——该 token 本就失效，无实际风险。
4. **refresh 不轮换**：换新只重签 access（绑定同一会话 sid），refresh token 本体不变、到期即须重新登录；登出会删除会话，旧 refresh 立即作废。
5. **ignore-paths 是安全配置**：登录/刷新接口必须在内；把业务接口配进去等于公开暴露，Review 时逐条盯防。
6. **Filter 层的 401 是手写的 JSON**：认证失败不经过 `GlobalExceptionHandler`（Filter 在 DispatcherServlet 之前），响应格式固定为 `{"code":1002,"msg":...}`，与全局返回体一致。
