# baseon-starter-cloud-nacos · 注册发现 + 配置中心

## 定位

微服务必备。一个依赖同时获得 Nacos 注册发现与配置中心能力（基于 SCA 2023.0.3.4），并把「本地开发该不该注册进注册中心」收敛成三档策略。

引入方式（版本由 BOM 管控）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-cloud-nacos</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `RegisterStrategyEnvironmentPostProcessor` | 把三档注册策略翻译成 `spring.cloud.nacos.discovery.*` | 默认 `FULL_REGISTER`，不干预 |
| `ConfigHolder` | 直读 `Environment` 获取配置，刷新免 `@RefreshScope` | 默认装配 |

注册方式：`META-INF/spring.factories`（EnvironmentPostProcessor）+ `AutoConfiguration.imports`（ConfigHolder）。
策略派生值以 **addLast 最低优先级** 注入，业务在 application.yml 显式声明的 `spring.cloud.nacos.discovery.*` 始终优先。

## 配置项

```yaml
spring:
  cloud:
    nacos:
      server-addr: localhost:8848    # 主端口（客户端 API + gRPC），不是控制台端口
      username: nacos
      password: nacos
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml   # optional 前缀：配置不存在也能启动

baseon:
  cloud:
    nacos:
      register-strategy: FULL_REGISTER   # LOCAL_NO_REGISTER / LOCAL_REGISTER_PRIVATE / FULL_REGISTER
```

## 三档注册策略

| 档位 | 行为 | 适用场景 |
|---|---|---|
| `LOCAL_NO_REGISTER` | `register-enabled=false`，完全不注册 | 本地起服务调别人，不污染注册中心 |
| `LOCAL_REGISTER_PRIVATE` | 注册，但元数据打 `baseon.local=true` | 本地实例需要被别人调，但只应被自己显式调用 |
| `FULL_REGISTER` | 不注入任何派生配置，SCA 默认行为 | 生产 / 共享测试环境 |

`baseon.local=true` 的实例会被 `baseon-starter-loadbalance` 默认排除（除非 `allow-local=true`）。

## 使用场景

### 场景 1：读取配置中心配置

```java
String greeting = ConfigHolder.get("demo.greeting", "default-value");
```

`ConfigHolder` 每次直读 `Environment`，Nacos 配置推送后无需 `@RefreshScope`、无需重启即可读到新值。

### 场景 2：本地调试不注册

```yaml
baseon:
  cloud:
    nacos:
      register-strategy: LOCAL_NO_REGISTER
```

服务照常启动、能调别人，但不会出现在注册中心，不会抢占正式实例的流量。

## 注意事项

1. **server-addr 必须是主端口 8848**：Nacos 3.x 控制台端口（如 8181）只提供 UI，客户端连它会报 `Client not connected`（详见 pitfalls P-009）。
2. **必须声明 spring.config.import**：SCA 配置中心默认开启 import-check，未声明会启动失败；用 `optional:nacos:` 前缀允许配置不存在（详见 pitfalls P-008）。
3. **策略是默认值不是覆盖**：业务显式配置 `spring.cloud.nacos.discovery.*` 会赢，策略只兜底。
