# 交付资产

> 本目录存放**不打包进 Jar、不参与 Maven 构建**的交付模板：CI 流水线、容器化、告警规则、架构决策记录。
> 所有文件均为模板，复制到业务工程后按实际环境替换占位值。

## 目录

| 目录 | 内容 | 复制到业务工程的位置 |
|---|---|---|
| `ci/` | GitLab CI 安全扫描 / 性能回归模板 + k6 脚本 | 仓库根 `.gitlab-ci.yml` 的 `include` 或 `ci/` |
| `container/` | 多阶段 Dockerfile、docker-compose、K8s 资源清单 | 仓库根 `Dockerfile`、`deploy/k8s/` |
| `alerting/` | Prometheus 抓取配置、告警规则、AlertManager 路由 | 监控仓库或 `deploy/monitoring/` |
| `adr/` | ADR 模板与已采纳决策、版本 Roadmap、反模式清单 | 仅阅读，按模板在业务工程新增 ADR |

## 使用约束

1. **不提交真实凭据**：模板中的密码、镜像仓库、Webhook、Nacos Token 全部为示例值；
2. **不改变构建**：本目录没有 `pom.xml`，不参与 reactor，`mvn verify` 不受影响；
3. **不要直接复制到本仓库根**：`Dockerfile`、`.gitlab-ci.yml` 放在根目录会被平台自动识别，模板保留在原位避免误触发；
4. **与代码同步演进**：模板引用的配置项（`baseon.*`、actuator 端点、服务端口）变更时，同一提交内更新本目录。
