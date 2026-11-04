# baseon-starter-swagger · OpenAPI3 文档（Knife4j UI）与生产硬关闭

## 定位

接口文档的一站式封装：基于 **Knife4j 4.5.0**（OpenAPI3 UI 增强，自带 springdoc 2.3.0 传递依赖，父 POM 锁定同版本对齐），统一文档 info 配置、内置全局 Bearer（JWT）认证方案与 baseon-starter-auth 联动，并兜底解决「文档被误带上生产」这一经典安全事故——激活生产 profile 时，定制 Bean、springdoc 端点与 Knife4j 三层关闭。

Knife4j 相比原生 Swagger UI 的增强：离线导出 Markdown/Word、接口排序与分组、全局参数、更友好的调试面板；文档入口为 `/doc.html`。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-swagger</artifactId>
</dependency>
```

## 自动装配清单

| 组件                                                  | 作用                                                                                                                                                                      | 默认行为                                                                                   |
| --------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `baseonOpenAPI`（`io.swagger.v3.oas.models.OpenAPI`） | 定制文档 info（title/description/version 从 `baseon.swagger.*` 读取）+ 注册名为 `bearerAuth` 的 HTTP Bearer/JWT 安全方案，并以 `SecurityRequirement` 全局生效                                    | 默认装配（Servlet Web 应用且 `baseon.swagger.enabled!=false` 且未命中生产 profile），业务可定义同名 Bean 整体替换 |
| `baseonServerDescriptionCustomizer`（`OpenApiCustomizer`） | springdoc 补全 servers 后执行，把 Servers 下拉项的说明文字从 `Generated server url` 换成服务名，多服务聚合时便于分辨文档归属                                          | 默认装配；说明文字取 `baseon.swagger.server-description`，留空回退 `spring.application.name`，两者皆空则跳过 |
| `SwaggerEnvironmentPostProcessor`                   | `EnvironmentPostProcessor`：统一管理 springdoc 与 Knife4j 启停。非生产显式打开 `knife4j.enable`（Knife4j 4.x 默认 false）；生产强制关闭 api-docs / swagger-ui / `knife4j.enable` 并切 `knife4j.production` | 随 spring.factories 注册即生效，`Ordered.LOWEST_PRECEDENCE` 最后执行，覆盖一切误配置 |

注册方式：自动配置走 `AutoConfiguration.imports`；`EnvironmentPostProcessor` 走 `META-INF/spring.factories`（Boot 3 对 EnvironmentPostProcessor 仍保留 spring.factories 通道）。

## 配置项

```yaml
baseon:
  swagger:
    enabled: true            # 总开关：false 时不装配 OpenAPI 定制 Bean，并关闭 Knife4j
    title: Baseon API        # 文档标题
    description: Baseon 脚手架接口文档
    version: 1.0.0           # 文档版本号
    server-description: 订单中心   # Servers 下拉的说明文字，留空取 spring.application.name
    prod-profiles:           # 生产 profile 名单，命中即触发三层关闭
      - prod
```

| 配置项                            | 类型            | 默认值              | 说明                                                          |
| ------------------------------ | ------------- | ---------------- | ----------------------------------------------------------- |
| `baseon.swagger.enabled`       | boolean       | `true`           | 总开关，关定制 Bean 与 Knife4j；不管 springdoc 端点本身                          |
| `baseon.swagger.title`         | String        | `Baseon API`     | 文档标题                                                        |
| `baseon.swagger.description`   | String        | `Baseon 脚手架接口文档` | 文档描述                                                        |
| `baseon.swagger.version`       | String        | `1.0.0`          | 文档版本号                                                       |
| `baseon.swagger.server-description` | String   | 空（回退服务名）        | 文档 UI 中 Servers 下拉项的说明文字，替代 `Generated server url`                     |
| `baseon.swagger.prod-profiles` | List\<String> | `[prod]`         | 生产 profile 名单，与激活 profile 有交集即视为生产，可追加 `production`、`pre` 等 |

## 快速上手

以 samples/demo-service 为例，引入依赖后直接启动：

1. 访问 `http://localhost:8080/doc.html`（Knife4j UI），页面标题/版本来自 `baseon.swagger.*`。
2. 点击右上角 **Authorize**，在 `bearerAuth` 弹窗里粘贴登录拿到的 JWT（只贴 token 本体即可，UI 会自动补 `Bearer `  前缀）。
3. 之后直接在 UI 里调试受保护接口，如 `/tenant/user/list`——请求会自动携带 `Authorization: Bearer <token>` 头，与 baseon-starter-auth 的过滤器链路无缝衔接。

业务代码零改动：springdoc 自动扫描 `@RestController`，无需加任何注解。

## 生产硬关闭的三层机制

本模块的核心价值。只要激活 profile 与 `prod-profiles` 名单存在交集（`prod` 与 `dev` 并存同样命中，宁可错杀），三层防护同时生效：

1. **定制层**：`BaseonSwaggerAutoConfiguration` 上的条件在配置类解析阶段求值，命中生产时整个自动配置不生效，不装配 `baseonOpenAPI` 定制 Bean，日志输出 WARN「检测到生产 profile，Swagger 定制已禁用」。
2. **端点层（兜底）**：`SwaggerEnvironmentPostProcessor` 把 `springdoc.api-docs.enabled=false`、`springdoc.swagger-ui.enabled=false` 塞进 Environment **最前**位置——优先级高于 Nacos 配置与任何 yml。即使运维在 prod 命名空间里误开了 springdoc，端点也是死的。
3. **Knife4j 层**：同一处理器把 `knife4j.enable=false`、`knife4j.production=true` 塞入，确保 `/doc.html` 不渲染、Knife4j 增强功能全部关闭。

### 验证方式

```bash
java -jar demo-service.jar --spring.profiles.active=prod
```

启动后：

- `GET /v3/api-docs` 应返回 404（接入了 starter-auth 时为 401：端点未注册，请求落到认证过滤器被拦），而非 JSON 文档；
- `GET /doc.html` 与 `GET /swagger-ui/index.html` 同样不可访问；
- 启动日志可见「已强制关闭 springdoc api-docs / swagger-ui 与 Knife4j」WARN。

对照组：不带 profile 或 `--spring.profiles.active=dev` 启动，`/doc.html` 正常渲染。

## 注意事项

1. **Knife4j 4.x 的 `enable` 默认 false**：非生产环境由 `SwaggerEnvironmentPostProcessor` 自动设为 `true`，业务无需手动配；若绕过本 starter 直接使用 Knife4j，需显式 `knife4j.enable: true`。
2. **三层防护职责不同**：定制层保护「文档内容定制」；端点层与 Knife4j 层才是安全兜底。即使业务把 `baseon.swagger.enabled=true` 且定制 Bean 被绕过，端点层依然保证 `/v3/api-docs` 与 `/doc.html` 不可达。
3. **名单为空等于放弃硬关闭**：`baseon.swagger.prod-profiles: []` 后任何 profile 都不命中，三层防护同时失效，仅建议在全链路另有网关层屏蔽的团队使用。
4. **webmvc 线专用**：引入的是 `knife4j-openapi3-jakarta-spring-boot-starter`（webmvc 线），仅适配 Servlet 栈；`EnvironmentPostProcessor` 与栈无关依然生效。
5. **非 Web 应用不装配**：自动配置带 `@ConditionalOnWebApplication(SERVLET)`，纯定时任务类服务引入本依赖不会启动文档端点。
6. **网关聚合**：Knife4j 无官方 webflux starter，网关侧（webflux）通过单独引入 `knife4j-openapi3-ui` 纯静态资源包（0 个 Java 类、无传递依赖）提供 `/doc.html`，文档数据由 springdoc webflux 的 swagger-config 提供；原生 Swagger UI 路径（`/swagger-ui.html`、`/swagger-ui/**`、`/webjars/swagger-ui/**`）通过 Gateway 路由拦截为 404，网关仅保留 Knife4j 入口。
7. **`springdoc.swagger-ui.enabled` 必须为 true**：Knife4j 4.x 加载 `/doc.html` 时会请求 `/v3/api-docs/swagger-config` 定位 OpenAPI 规范地址，该端点由 springdoc swagger-ui 自动配置提供，`enabled=false` 会导致它返回 404，Knife4j 页面报「文档请求异常」。即使只想用 Knife4j 也不能关 swagger-ui。

## 网关聚合方案（7 期落地，samples/demo-gateway 为参考实现）

微服务环境下各服务通过网关统一聚合文档入口，各服务自身也保留 Knife4j UI 供本地调试：

- **业务服务侧（本模块，webmvc）**：使用 Knife4j，访问 `http://<service-host>:<port>/doc.html`；`springdoc.swagger-ui.enabled` 保持 `true`（Knife4j 依赖其 swagger-config 端点）；
- **网关侧（webflux）**：引 `springdoc-openapi-starter-webflux-ui`（提供 swagger-config 与聚合逻辑）+ `knife4j-openapi3-ui`（仅 Knife4j 前端静态资源）。`GatewaySwaggerDocConfig` 从 DiscoveryClient 动态生成各服务文档地址（`/{serviceId}/v3/api-docs`，经网关 discovery locator 路由转发）填入下拉列表；启动时初始化 + Nacos HeartbeatEvent 触发刷新，网关自身排除。

网关文档 UI 访问地址：`http://<gateway>/doc.html`（Knife4j 风格，左上角下拉切换服务）。原生 Swagger UI 路径已通过 Gateway 路由（`SetStatus=404`）屏蔽，不可访问。注意 `springdoc.swagger-ui.enabled` 必须保持 `true`——Knife4j 前端依赖 `/v3/api-docs/swagger-config` 端点获取服务列表，关掉会导致下拉为空。

注意两个坑：**网关侧不要配** **`springdoc.api-docs.enabled=false`**——webflux 线 UI 的配置 Bean 整体受 api-docs 开关门控，关掉 swagger-ui 也起不来（P-019）；springdoc 2.x 的聚合配置端点是 `/v3/api-docs/swagger-config`。服务上下线后下拉列表刷新有心跳延迟（约 30s），非实时。
