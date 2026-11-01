# 版本 Roadmap

> 本文件只记录**已交付事实**与**明确挂起项**。「计划」不等于承诺，进入开发前需单独评审。

## 当前状态

- 版本：`1.0.0-SNAPSHOT`（`${revision}`）
- 运行时：JDK 17 / Spring Boot 3.2.12 / Spring Cloud 2023.0.5 / MyBatis-Plus 3.5.9
- 已交付：0~12 期（12 期为本文档资产，非 Maven 构件）

## 已交付期次

| 期 | 内容 | 关键产出 |
|---|---|---|
| 0 | 根 POM + BOM + demo | 空工程可 install，enforcer 生效 |
| 1 | baseon-core + ArchUnit | `R<T>`、分页、错误码、上下文、注解、SPI |
| 2 | starter-web + starter-trace | 全局异常、Jackson、TraceId、MDC |
| 3 | cloud-nacos / cloud-feign / loadbalance | 注册发现、上下文透传、灰度路由 |
| 4 | datasource / mybatis-plus / redis / cache / tenant | 多租户读写与缓存隔离 |
| 5 | auth / xss / sentinel / resilience / idempotent | 鉴权、限流熔断、幂等闭环 |
| 6 | excel / swagger / log / debug-tools | 通用工具批 |
| 7 | generator + archetype ×2 + demo-gateway | 5 分钟起服务、网关聚合 |
| 8 | tracing-otel + starter-trace 协同 | 跨服务链路 traceId 一致 |
| 9 | data-scope | 行列级数据权限 |
| 10 | test-base ×2 | 单元/集成测试基座 |
| 11 | core-boot | 容器持有者、编程式事务、线程池工厂 |
| 12 | docs/assets | CI、容器、告警、ADR 模板 |

## 按需挂起项

以下构件在设计总纲中列出，但**尚未落地**，不要当作可用能力：

| 构件 | 触发条件 | 备注 |
|---|---|---|
| `baseon-starter-sharding` | 单表容量或分片需求明确 | 依赖 `starter-mybatis-plus`，分片规则业务自定 |
| `baseon-starter-kms` | 需要 KMS/Vault 托管密钥与证书轮换 | 与 `core.secure.CryptoService` 契约配合 |
| `baseon-starter-seata` | 跨库强一致事务成为必需 | 默认不引入，避免全局代理成本 |
| `baseon-starter-mq-*` | 需要统一 MQ 抽象 | 建议先做 RocketMQ，抽象先行 |
| `baseon-starter-context-agent` | 遗留线程池无法统一改造 | TTL javaagent 默认关闭，风险需评估 |
| `core.db` / `core.launch` | 出现稳定的通用需求 | 当前仍为规划态，不要引用 |

## 1.0.0 GA 出口条件

1. 全量 `mvn -q -T 1C clean verify` 稳定通过，ArchUnit 与 Checkstyle 无豁免；
2. 每个发布构件有模块手册、配置项与边界说明；
3. 至少一个由 archetype 生成的示例工程通过启动、鉴权、租户、缓存、追踪联调；
4. 依赖漏洞扫描无 HIGH/CRITICAL 未处置项（本仓库 `docs/assets/ci` 模板可直接接入）；
5. 容器镜像、K8s 清单、告警规则经一次真实环境演练；
6. 版本发布流程（flatten、deploy、BOM 校验）有可复现脚本或流水线。

## 版本策略

- 同一 `revision` 下保持二进制兼容；破坏性变更升主版本；
- BOM 与构件版本永远一致，禁止单独发某构件而不更新 BOM；
- 已发布版本的模块手册保留，不随新版本覆盖历史结论。
