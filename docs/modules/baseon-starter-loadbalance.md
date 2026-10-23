# baseon-starter-loadbalance · 灰度路由负载均衡

## 定位

需要灰度发布 / 多环境隔离的调用方服务引入。对所有经过 Spring Cloud LoadBalancer 的调用生效（Feign、RestTemplate、WebClient），按灰度标签筛选下游实例。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-loadbalance</artifactId>
</dependency>
```

## 自动装配清单

| Bean | 作用 | 默认行为 |
|---|---|---|
| `GrayTagLoadBalancer` | 按灰度标签过滤实例，轮询选择 | 默认开启 |
| `GrayContextFilter` | Servlet 入口解析 `X-Gray-Tag` 写入 `GrayContext` | 仅 Servlet 环境装配 |

装配要点：`BaseonLoadBalancerConfiguration` 经 `@LoadBalancerClients(defaultConfiguration)` 引用，**不做组件扫描**，否则会对所有服务重复注册并覆盖业务自定义配置。

## 配置项

```yaml
baseon:
  cloud:
    loadbalance:
      enabled: true        # 灰度路由总开关；关闭后退化为普通轮询
      allow-local: false   # 是否允许路由到 baseon.local=true 的本地调试实例
```

## 路由规则

1. **请求带 `X-Gray-Tag: gray`**：只选元数据 `gray-tag=gray` 的实例；
   无匹配实例时**回退到全部实例并打 WARN**（宁可降级可用，不让灰度流量 503）。
2. **请求不带标签**：排除所有带 `gray-tag` 元数据的灰度实例（保护灰度环境），
   并排除 `baseon.local=true` 的本地调试实例（除非 `allow-local=true`）。
3. 候选实例多于一个时轮询。

实例打灰度标记的方式（启动参数或配置）：

```bash
--spring.cloud.nacos.discovery.metadata.gray-tag=gray
```

## 使用场景

### 场景 1：灰度发布验证

新版本以 `gray-tag=gray` 启动一个实例，测试请求带 `X-Gray-Tag: gray` 全部命中新实例；普通用户流量不进灰度实例。

### 场景 2：本地实例联调

本地服务用 `LOCAL_REGISTER_PRIVATE` 策略注册（自动打 `baseon.local=true`），其他服务的默认流量不会路由过来；自己联调时在调用方配 `allow-local=true` 放行。

## 注意事项

1. **回退是降级不是路由**：`无匹配实例，回退到全部 N 个实例` 的 WARN 是定位灰度发布遗漏的关键信号，不要屏蔽。
2. **标签名称不可配置**：`X-Gray-Tag` / `gray-tag` / `baseon.local` 固定于 core 的 `CloudHeaders`，灰度链路跨入口解析、Feign 透传、负载均衡三个 Starter，允许配置会出现同链路名称不一致的隐蔽故障。
3. **调用链中途的标签来源**：`GrayContextFilter` 只装配于 Servlet 环境；异步/MQ 场景需自行用 `GrayContext.set(tag)` 并在 finally 中 `clear()`。
