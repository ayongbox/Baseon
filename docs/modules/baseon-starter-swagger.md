# baseon-starter-swagger · OpenAPI3 文档与生产硬关闭

## 定位

接口文档的一站式封装：基于 springdoc-openapi 2.5.0（OpenAPI3 + Swagger UI），统一文档 info 配置、内置全局 Bearer（JWT）认证方案与 baseon-starter-auth 联动，并兜底解决「Swagger 被误带上生产」这一经典安全事故——激活生产 profile 时，定制 Bean 与 springdoc 端点双层关闭。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-swagger</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `baseonOpenAPI`（`io.swagger.v3.oas.models.OpenAPI`） | 定制文档 info（title/description/version 从 `baseon.swagger.*` 读取）+ 注册名为 `bearerAuth` 的 HTTP Bearer/JWT 安全方案，并以 `SecurityRequirement` 全局生效 | 默认装配（Servlet Web 应用且 `baseon.swagger.enabled!=false` 且未命中生产 profile），业务可定义同名 Bean 整体替换 |
| `ProdProfileSwaggerDisabler` | `EnvironmentPostProcessor`：命中生产 profile 时把 `springdoc.api-docs.enabled=false`、`springdoc.swagger-ui.enabled=false` 以 `MapPropertySource` addFirst 塞进 Environment，强制关闭端点 | 随 spring.factories 注册即生效，无 Bean |

注册方式：自动配置走 `AutoConfiguration.imports`；`EnvironmentPostProcessor` 走 `META-INF/spring.factories`（Boot 3 对 EnvironmentPostProcessor 仍保留 spring.factories 通道）。

## 配置项

```yaml
baseon:
  swagger:
    enabled: true            # 总开关：false 时不装配 OpenAPI 定制 Bean
    title: Baseon API        # 文档标题
    description: Baseon 脚手架接口文档
    version: 1.0.0           # 文档版本号
    prod-profiles:           # 生产 profile 名单，命中即触发双层关闭
      - prod
```

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `baseon.swagger.enabled` | boolean | `true` | 总开关，只管本模块定制 Bean，不管 springdoc 端点本身 |
| `baseon.swagger.title` | String | `Baseon API` | 文档标题 |
| `baseon.swagger.description` | String | `Baseon 脚手架接口文档` | 文档描述 |
| `baseon.swagger.version` | String | `1.0.0` | 文档版本号 |
| `baseon.swagger.prod-profiles` | List\<String\> | `[prod]` | 生产 profile 名单，与激活 profile 有交集即视为生产，可追加 `production`、`pre` 等 |

## 快速上手

以 samples/demo-service 为例，引入依赖后直接启动：

1. 访问 `http://localhost:8080/swagger-ui/index.html`，页面标题/版本来自 `baseon.swagger.*`。
2. 点击右上角 **Authorize**，在 `bearerAuth` 弹窗里粘贴登录拿到的 JWT（只贴 token 本体即可，UI 会自动补 `Bearer ` 前缀）。
3. 之后直接在 UI 里调试受保护接口，如 `/tenant/user/list`——请求会自动携带 `Authorization: Bearer <token>` 头，与 baseon-starter-auth 的过滤器链路无缝衔接。

业务代码零改动：springdoc 自动扫描 `@RestController`，无需加任何注解。

## 生产硬关闭的双层机制

本模块的核心价值。只要激活 profile 与 `prod-profiles` 名单存在交集（`prod` 与 `dev` 并存同样命中，宁可错杀），两层防护同时生效：

1. **定制层**：`BaseonSwaggerAutoConfiguration` 上的条件在配置类解析阶段求值，命中生产时整个自动配置不生效，不装配 `baseonOpenAPI` 定制 Bean，日志输出 WARN「检测到生产 profile，Swagger 定制已禁用」。
2. **端点层（兜底）**：`ProdProfileSwaggerDisabler`（`EnvironmentPostProcessor`，`Ordered.LOWEST_PRECEDENCE` 保证最后执行）把 `springdoc.api-docs.enabled=false`、`springdoc.swagger-ui.enabled=false` 塞进 Environment **最前**位置——优先级高于 Nacos 配置与任何 yml。即使运维在 prod 命名空间里误开了 springdoc，端点也是死的，日志输出 WARN「已强制关闭 springdoc api-docs 与 swagger-ui 端点」。

### 验证方式

```bash
java -jar demo-service.jar --spring.profiles.active=prod
```

启动后：

- `GET /v3/api-docs` 应返回 404（接入了 starter-auth 时为 401：端点未注册，请求落到认证过滤器被拦），而非 JSON 文档；
- `GET /swagger-ui/index.html` 同样不可访问；
- 启动日志可见上述两条 WARN。

对照组：不带 profile 或 `--spring.profiles.active=dev` 启动，两个端点正常返回。

## 注意事项

1. **两层防护职责不同**：定制层保护的是「文档内容定制」；端点层才是安全兜底。即使业务把 `baseon.swagger.enabled=true` 且定制 Bean 被绕过，端点层依然保证 `/v3/api-docs` 不可达。
2. **名单为空等于放弃硬关闭**：`baseon.swagger.prod-profiles: []` 后任何 profile 都不命中，两层防护同时失效，仅建议在全链路另有网关层屏蔽的团队使用。
3. **webmvc 线专用**：引入的是 `springdoc-openapi-starter-webmvc-ui`，仅适配 Servlet 栈；本模块的生产硬关闭逻辑（EnvironmentPostProcessor）与栈无关依然生效。
4. **非 Web 应用不装配**：自动配置带 `@ConditionalOnWebApplication(SERVLET)`，纯定时任务类服务引入本依赖不会启动文档端点。

## 网关聚合方案（7 期落地，samples/demo-gateway 为参考实现）

微服务环境下各服务不再单独暴露 swagger-ui，文档 UI 统一收敛到网关：

- **业务服务侧（本模块）**：只保留 `/v3/api-docs` 端点，yml 加 `springdoc.swagger-ui.enabled: false` 关闭本地 UI；
- **网关侧（webflux）**：引 `springdoc-openapi-starter-webflux-ui`（版本与 webmvc 线一致，父 POM 统一管控），`GatewaySwaggerDocConfig` 从 DiscoveryClient 动态生成各服务文档地址（`/{serviceId}/v3/api-docs`，经网关 discovery locator 路由转发）填入 Swagger UI 下拉列表；启动时初始化 + Nacos HeartbeatEvent 触发刷新，网关自身排除。

注意两个坑：**网关侧不要配 `springdoc.api-docs.enabled=false`**——webflux 线 UI 的配置 Bean 整体受 api-docs 开关门控，关掉 swagger-ui 也起不来（P-019）；springdoc 2.x 的聚合配置端点是 `/v3/api-docs/swagger-config`。服务上下线后下拉列表刷新有心跳延迟（约 30s），非实时。
