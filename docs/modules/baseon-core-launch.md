# baseon-core-launch · 应用启动治理

## 定位

承载 `core.launch` 中**必须依赖 Spring Boot 生命周期**的能力：启动横幅、启动前置校验、优雅停机钩子。

`baseon-core` 由 ArchUnit 强制零 Spring 依赖，因此该包物理上独立成 `baseon-core-launch` 构件，
逻辑包名仍为 `io.github.ayongbox.baseon.core.launch`。只允许依赖 `spring-boot` /
`spring-boot-autoconfigure` / `slf4j-api`，禁止依赖 `spring-cloud-*` 与任何 Starter。

机制（编排、失败摘要、fail-fast 策略、停机钩子遍历）由框架承载；策略（检查什么、停机时清理什么）
由业务通过 SPI 注入，与数据权限的 Provider 分层一致。

## 自动装配清单

| 类 | 作用 |
|---|---|
| `BaseonLaunchAutoConfiguration` | 注册横幅打印器、前置校验执行器、优雅停机监听器与内置必选配置检查 |

登记文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

## 配置项

| 配置 | 默认 | 说明 |
|---|---|---|
| `baseon.launch.enabled` | `true` | 整体开关；关闭后四个 Bean 全部不注册 |
| `baseon.launch.banner-enabled` | `true` | 关闭后只跳过横幅打印，不影响校验与停机钩子 |
| `baseon.launch.fail-fast` | `true` | 任一 `LaunchCheck` 失败是否抛 `LaunchCheckException` 终止启动；`false` 降级为 WARN 日志，仅限本地联调 |
| `baseon.launch.required-environment-variables` | 空 | 必选环境变量清单，缺失即视为校验失败 |
| `baseon.launch.required-properties` | 空 | 必选配置项清单（点号路径），缺失即视为校验失败 |

## 使用场景

### 场景 1：声明必选配置，把文档约定变成启动期硬约束

```yaml
baseon:
  launch:
    required-properties:
      - spring.datasource.url
      - baseon.auth.jwt-secret
    required-environment-variables:
      - NACOS_ADDR
```

缺失时启动直接失败，摘要说清缺哪一项：

```
启动前置校验失败（1 项）：required-configuration: 缺少必选配置：env:NACOS_ADDR, property:baseon.auth.jwt-secret
```

### 场景 2：业务自定义启动检查（SPI）

实现 `LaunchCheck` 注册为 Bean 即可，框架在 `ApplicationReadyEvent` 统一编排：

```java
@Component
public class InventoryServiceCheck implements LaunchCheck {

    @Override
    public String name() {
        return "inventory-service-reachable";
    }

    @Override
    public void check(Environment environment) {
        // 只读检查：配置、端口、下游可达性；禁止初始化写操作
    }
}
```

检查失败抛异常即视为不通过；`name()` 用于失败摘要与日志，必须稳定唯一（建议 kebab-case）。
检查在应用完全就绪后、对外提供服务前执行（K8s readiness 探针切换前）。

### 场景 3：优雅停机钩子（SPI）

```java
@Component
public class LocalCacheFlushHook implements ShutdownHook {

    @Override
    public void onShutdown() {
        cache.flushToDisk();
    }
}
```

容器收到关闭信号（`ContextClosedEvent`）时以最高优先级统一执行，先于普通 `Lifecycle` /
`DisposableBean` 回调；单个钩子异常只记 error 日志，不影响其余钩子。

## 注意事项

1. **`LaunchCheck` 必须只读**：执行时机在启动阶段，写操作可能依赖未就绪的 Bean；初始化逻辑放 `ApplicationRunner`，不要塞进检查。
2. **横幅刻意不走 `banner.txt`**：classpath 根的 banner.txt 会覆盖业务自定义横幅；`BaseonBannerPrinter` 用 `ApplicationRunner` 输出，与业务横幅互不冲突，可用 `baseon.launch.banner-enabled=false` 独立关闭。
3. **`fail-fast=false` 仅限本地联调**：生产环境关闭会让配置缺失的服务带病上线，缺失问题被推迟到首次调用才暴露。
4. **内置检查可被覆盖**：注册名为 `requiredConfigurationCheck` 的 `LaunchCheck` Bean 即可替换内置实现；执行器本身也支持同类型覆盖。
5. **停机钩子的线程池清理归业务**：`ThreadPoolFactory` 创建的线程池非守护、不托管，必须在 `ShutdownHook` 里 `shutdown()`，否则 JVM 无法退出。
6. **Spring 依赖为 optional**：本构件不强制传递 spring-boot；非 Boot 工程引入时自动装配不生效，类可独立使用。
