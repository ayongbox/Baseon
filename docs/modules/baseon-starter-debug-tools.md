# baseon-starter-debug-tools · 开发调试端点

## 定位

开发联调期的自检端点：一键查看当前请求的登录上下文（userId/username/tenantId/permissions）、traceId 与生效配置，用于确认「拦截器/认证链路是否正确填充了上下文」。零业务侵入，引入依赖即用。

这类端点在生产环境是**信息泄露面**（暴露用户标识、权限集合、激活 profile），因此生产失效设计为**硬失效**：命中生产 profile 即不装配、运行期二次拦截，而不是一个可被配置中心误改回去的普通开关。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-debug-tools</artifactId>
</dependency>
```

## 端点清单

| 端点 | 返回内容 | 说明 |
|---|---|---|
| `GET /debug/context` | `traceId`、`userId`、`username`、`tenantId`、`permissions`、请求头摘要（仅 Host/UA） | 无登录上下文时用户字段为 null，不抛异常 |
| `GET /debug/profiles` | `activeProfiles`、`baseon.debug.enabled` 生效值 | 确认 profile 与开关状态 |

响应体为统一的 `R<Map>`，无需专用 DTO。

## 三层硬失效机制

生产判定的唯一逻辑在 `DebugProdProfileGuard.isProdActive(Environment, List<String>)`：**activeProfiles 与 prodProfiles 存在交集即为生产**，装配期与运行期共用同一判定，避免逻辑漂移。

1. **装配期（不注册）**：`@ConditionalOnProperty(baseon.debug.enabled)` 与自定义 `NotProdProfileCondition` 叠加，任一不满足则端点 Bean 不存在——不是「存在但拒绝」，而是「端点不存在」。Condition 内用 `Binder` 单独绑定 `baseon.debug.prod-profiles`，保证与运行期使用同一份名单。
2. **运行期（二次拦截）**：即使装配被误判放行，`DebugEndpointController` 每个端点入口再执行一次 `isProdActive`，命中即抛 `BizException(CommonErrorCode.NOT_FOUND)`，由 starter-web 的 `GlobalExceptionHandler` 映射为 HTTP 404，对外与「端点不存在」无差别。
3. **留痕（WARN 日志）**：装配被生产 profile 拦截时输出 `检测到生产 profile，debug 端点已硬失效`，避免生产排障时「404 却查不到原因」。

## 配置项

```yaml
baseon:
  debug:
    enabled: true          # 总开关，false 时端点 Bean 不注册
    prod-profiles:         # 生产 profile 名单，activeProfiles 命中其一即硬失效
      - prod
```

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `baseon.debug.enabled` | boolean | `true` | 总开关；即使为 true，命中 prod-profiles 仍强制失效 |
| `baseon.debug.prod-profiles` | List\<String\> | `[prod]` | 生产 profile 名单，可追加 `production`、`prd` 等；置空等于放弃硬失效保护，不建议 |

## 验证方式

```bash
# 默认启动：端点可访问
curl http://localhost:8080/debug/context
# => {"code":0,"msg":"操作成功","data":{"traceId":"...","userId":null,...}}

# 生产 profile 启动：端点 404（Bean 未注册），启动日志出现 WARN 留痕
java -jar app.jar --spring.profiles.active=prod
curl -i http://localhost:8080/debug/context
# => HTTP 404
```

单元测试：`mvn -q -pl baseon-starter-debug-tools -am test`，覆盖 `isProdActive` 全部分支（空 profile / dev / prod / 并存 / 自定义名单）、Condition 在 prod 不匹配与 dev 匹配、Controller 在 prod 下的二次拦截。

## 注意事项

1. **端点类不参与组件扫描**：`DebugEndpointController` 位于自动配置包下，由 `BaseonDebugAutoConfiguration` 以 `@Bean` 注册，业务扫描不到它，保证「未启用时端点不存在」。
2. **生产名单不要随意清空**：`prod-profiles` 置空意味着任何 profile 都判定为非生产，硬失效机制整体失效，等同把信息泄露面的闸门交还给运气。
3. **请求头摘要刻意收敛**：只返回 Host/UA，不回显 Authorization 等敏感头，避免调试端点本身变成凭证泄露通道。
4. **业务可替换端点实现**：Bean 带 `@ConditionalOnMissingBean`，如需扩展调试信息可自行注册同名类型 Bean 覆盖默认实现（生产判定逻辑请复用 `DebugProdProfileGuard`，不要另写一套）。
