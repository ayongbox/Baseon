# 告警资产

## 文件

| 文件 | 作用 |
|---|---|
| `prometheus.yml` | 抓取 `/actuator/prometheus`、加载规则、指向 AlertManager |
| `prometheus-rules.yaml` | 8 条告警：可用性、5xx、P95、堆、GC、连接池等待/超时、慢 SQL |
| `alertmanager.yml` | 按 severity 路由、同源告警抑制、Webhook 接收端模板 |

## 前置条件

1. 业务工程引入 `micrometer-registry-prometheus`（Spring Boot 3 不默认带入）；
2. 暴露端点：`management.endpoints.web.exposure.include=health,info,prometheus`；
3. 接入 `baseon-starter-auth` 时放行 `/actuator/prometheus`，否则抓取 401、`up=0`；
4. K8s 环境设置 `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true` 才有 readiness/liveness 子端点。

## 接入方式

### docker-compose（配合 container 模板）

```bash
docker compose -f docs/assets/container/docker-compose.yml --profile monitoring up -d
```

### 独立 Prometheus

把 `prometheus.yml` 与 `prometheus-rules.yaml` 挂载到 Prometheus 容器：

```bash
docker run -d --name prometheus -p 9090:9090 \
  -v "$PWD/docs/assets/alerting/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  -v "$PWD/docs/assets/alerting/prometheus-rules.yaml:/etc/prometheus/rules/baseon-rules.yaml:ro" \
  prom/prometheus:v2.51.0
```

### K8s + Prometheus Operator

不用本目录的 `prometheus.yml`，改为创建 `ServiceMonitor` 指向服务端口与 `/actuator/prometheus`，规则用 `PrometheusRule` CRD（把 `prometheus-rules.yaml` 的 `spec.groups` 原样搬入）。

## 指标与阈值

| 告警 | 指标 | 默认阈值 | 调整建议 |
|---|---|---|---|
| `BaseonServiceDown` | `up` | 连续 2 分钟为 0 | 不要调大，抓取失败应尽快确认 |
| `BaseonHighErrorRate` | `http_server_requests_seconds_count{status=~"5.."}` | 5% / 10 分钟 | 按 SLO 调到 1%~5% |
| `BaseonHighLatencyP95` | `http_server_requests_seconds_bucket` | P95 > 1s | 按接口分级，核心接口 300ms |
| `BaseonHeapUsageHigh` | `jvm_memory_used_bytes / max_bytes` | 85% / 10 分钟 | 配合 `MaxRAMPercentage` 与 memory limit |
| `BaseonGcPauseHigh` | `jvm_gc_pause_seconds_sum / count` | 平均 200ms | 关注 P99 停顿与 Full GC |
| `BaseonHikariPendingHigh` | `hikaricp_connections_pending` | > 5 / 5 分钟 | 按连接池大小设，不超过 max 的 20% |
| `BaseonHikariTimeouts` | `hikaricp_connections_timeout_total` | 5 分钟内出现 | 任何超时都应排查 |
| `BaseonSlowSqlSpike` | `baseon_mybatis_slowsql_total` | 0.2 次/秒 / 10 分钟 | 与 `slow-sql-threshold-ms` 联动 |

## 注意事项

1. **规则静默空转**：指标未暴露时 `up` 仍为 1（如果抓取成功但某些指标不存在），对应告警永远不触发；上线后先在 Prometheus 查询 `count(http_server_requests_seconds_count)` 确认有数据（P-032）；
2. **`application` 标签来自抓取配置**：本模板在 static_configs 打 `application` 标签，K8s SD 需在 relabel 阶段补齐，否则 `by (application)` 聚合为空；
3. **慢 SQL 指标名**：Micrometer 会把 `baseon.mybatis.slowsql` 转为 `baseon_mybatis_slowsql_total`（Counter 自动加 `_total`）；
4. **AlertManager 占位地址必须替换**：模板中的 `alert-webhook.example.com` 不会收到任何请求，错误接收端不会报错，告警会静默丢弃；
5. **先调阈值再上生产**：默认阈值面向中等规模服务，接入后至少观察一周再收紧。
