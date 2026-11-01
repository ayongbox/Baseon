# CI 模板

## 文件

| 文件 | 作用 | 关键变量 |
|---|---|---|
| `security-scan.gitlab-ci.yml` | 依赖漏洞、镜像漏洞、K8s/Dockerfile 配置风险扫描（Trivy） | `SECURITY_SCAN_IMAGE_REF`、`TRIVY_SEVERITY` |
| `performance-regression.gitlab-ci.yml` | k6 冒烟压测，P95/失败率不达标即失败 | `PERF_BASE_URL`、`PERF_P95_MS`、`PERF_FAIL_RATE` |
| `perf/smoke.js` | 默认压测脚本（恒定 VU 打存活探针） | `PERF_HEALTH_PATH`、`PERF_VUS`、`PERF_DURATION` |

## 接入步骤

1. 把本目录复制到业务工程 `ci/`（保留 `perf/smoke.js` 的相对路径）；
2. 在根 `.gitlab-ci.yml` 引入两个模板：

```yaml
include:
  - local: 'ci/security-scan.gitlab-ci.yml'
  - local: 'ci/performance-regression.gitlab-ci.yml'
```

3. 镜像扫描任务需要先推送候选镜像，再设置变量（在 GitLab CI/CD Variables 中配置，不要写进仓库）：

```yaml
# 上游构建任务示例
push-image:
  script:
    - docker build -f docs/assets/container/Dockerfile --build-arg MODULE=samples/demo-service -t "$CI_REGISTRY_IMAGE:$CI_COMMIT_SHA" .
    - docker push "$CI_REGISTRY_IMAGE:$CI_COMMIT_SHA"
    - echo "SECURITY_SCAN_IMAGE_REF=$CI_REGISTRY_IMAGE:$CI_COMMIT_SHA" >> security-scan.env
  artifacts:
    reports:
      dotenv: security-scan.env
```

4. 性能任务需要被测环境可达：评审环境地址放 `PERF_BASE_URL`，并在部署任务完成后触发。

## 注意事项

1. **`stages` 顺序由根文件决定**：两个模板各自声明 `security` / `performance`，GitLab 会并入根文件；请在根文件显式按 `build → deploy → security → performance` 排序，且不要给扫描/压测任务加 `needs: []`，否则会早于镜像推送或部署完成；
2. **不要扫 `latest`**：镜像扫描只接受不可变标签（`$CI_COMMIT_SHA` 或语义化版本），否则报告无法复现；
3. **Trivy 原生 JSON 不是 GitLab Dependency Scanning 报告格式**：模板只归档产物，不挂 `reports.dependency_scanning`，避免平台解析失败；
4. **压测不要指向生产**：探针也会计入 QPS 与告警规则；测试环境至少与生产同 JVM 参数与资源限制；
5. **探针被认证拦截时压测全红**：`baseon-starter-auth` 默认拦截 `/actuator/**`，需把探针路径加入 `baseon.auth.ignore-paths`（见 P-031）；
6. **依赖扫描依赖漏洞库更新**：`TRIVY_CACHE_DIR` 已挂 cache，流水线仍建议每日一次全量扫描，MR 只扫变更依赖。
