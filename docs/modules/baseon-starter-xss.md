# baseon-starter-xss · XSS 转义防护

## 定位

Web 入参的 XSS 兜底防线。对请求参数、请求头、JSON 请求体的字符串字段统一做 HTML 转义（`& < > " ' \`` 转为 HTML 实体），破坏脚本与标签结构，覆盖反射型/存储型 XSS 的常见入口。业务代码零侵入。

引入方式（版本由 BOM 管控，依赖 baseon-starter-web）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-xss</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `XssFilter` + `XssRequestWrapper` | 过滤器内包装请求：`getParameter` / `getParameterValues` / `getHeader` 读取时逐个转义，覆盖 query/form 参数与请求头 | 默认装配，`FilterRegistrationBean` 注册，order = `Ordered.HIGHEST_PRECEDENCE + 30`（晚于 trace/日志过滤器，早于业务过滤器） |
| `XssStringDeserializer` | Jackson String 反序列化器：JSON 请求体字符串字段绑定到对象前完成转义 | 默认装配，注册为 `com.fasterxml.jackson.databind.Module` Bean，Boot 自动纳入全局 ObjectMapper |
| `XssEscapeUtil` | 转义工具：`& < > " ' \`` → HTML 实体，null/空串原样返回 | 纯静态工具，无 Bean |

注册方式：`AutoConfiguration.imports`。两条防线互补：过滤器包装不到请求体反序列化过程，JSON body 必须由 Jackson 层处理；反过来 Jackson 管不到 query/form 参数与请求头。

## 配置项

```yaml
baseon:
  xss:
    enabled: true                  # 总开关：false 时过滤器原样放行所有请求
    exclude-paths:                 # 排除路径（Ant 风格），命中的请求不做转义
      - /demo/rich-text/**
```

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `baseon.xss.enabled` | boolean | `true` | 总开关，关闭后过滤器零包装直通 |
| `baseon.xss.exclude-paths` | List\<String\> | `[]` | 排除路径（Ant 风格），用于富文本提交等由业务自行处理 XSS 的接口 |

## 快速上手

引入依赖后即生效，无需任何代码改动：

```java
// GET /demo/xss/echo?keyword=<script>alert(1)</script>
@GetMapping("/demo/xss/echo")
public String echo(String keyword) {
    return keyword;   // 实际拿到 &lt;script&gt;alert(1)&lt;/script&gt;
}
```

```java
// POST /demo/xss/echo {"content": "<script>alert(1)</script>"}
@PostMapping("/demo/xss/echo")
public EchoDTO echo(@RequestBody EchoDTO dto) {
    return dto;   // content 字段已在 Jackson 反序列化时转义
}
```

## 能力边界

**转义式防护是取舍后的兜底方案，不是银弹**，接入前必须理解以下边界：

1. **富文本场景冲突**：编辑器提交的 HTML 内容（`<p>`、`<img>` 等）会被无差别转义成实体，回显时变成纯文本。这类接口必须加入 `exclude-paths`，由业务自行做白名单过滤（如 OWASP Java HTML Sanitizer / jsoup）——白名单过滤与全局转义是两套机制，本 starter 只提供后者。
2. **入库数据已被改写**：转义发生在入参阶段，落库的是转义后的实体串。这保证了「存储型 XSS 在输出侧绝对安全」，代价是原始输入不可还原，且数据被下游非 HTML 场景（导出 Excel、全文检索、对接第三方）复用时看到的是实体串。需要原文的场景同样走 `exclude-paths`。
3. **只覆盖三条入口**：query/form 参数、请求头、JSON 请求体字符串字段。URL 路径变量（`@PathVariable`）、multipart 文件内容、XML 请求体不在防护范围内。
4. **JS/URL 上下文不适用**：HTML 实体转义防御的是 HTML 文档与属性上下文；把用户输入拼进 `<script>` 块或 `javascript:` URL 的场景，实体转义不足以防御，前端模板引擎的上下文感知转义才是正解。
5. **不替代输出侧防护**：本 starter 是输入侧兜底，前端渲染时仍应使用模板引擎默认转义，避免依赖「入库一定干净」这一假设。

## 注意事项

1. **排除路径是例外通道**：命中 `exclude-paths` 的请求完全不包装、零损耗直通，但 XSS 责任也随之转移给业务，配置前确认该接口确有自行防护。
2. **已有实体会被再转义一次**：输入 `&amp;` 会变成 `&amp;amp;`（`&` 本身转义）。这是转义式防护的固有行为，前端原样渲染时仍显示 `&amp;`，语义不丢失。
3. **过滤器顺序固定**：order = `HIGHEST_PRECEDENCE + 30`，保证 trace/日志类过滤器先拿到原始请求（日志记录的是原文），业务过滤器与控制器拿到的是转义后的值。
4. **与 starter-web 的 Jackson 配置兼容**：`XssStringDeserializer` 以 `Module` Bean 形式注册，与 starter-web 的 `Jackson2ObjectMapperBuilderCustomizer` 共存不冲突，业务自定义 ObjectMapper Bean 时需自行注册该 Module。
