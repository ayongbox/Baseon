# 容器化模板

## 文件

| 文件 | 作用 |
|---|---|
| `Dockerfile` | JDK 17 多阶段构建：Maven 构建 → JRE Alpine 运行，非 root、容器感知堆参数 |
| `dockerignore` | 复制到业务工程根并重命名为 `.dockerignore` |
| `docker-compose.yml` | MySQL 8 + Redis 7 + Nacos 2.3 + demo-service；`monitoring` profile 追加 Prometheus/AlertManager |
| `mysql/init/01-create-schema.sql` | 创建 `baseon_slave`，与 `baseon_master` 模拟主从 |
| `k8s/configmap.yaml` | 非敏感环境变量（端口、注册中心地址、探针、actuator） |
| `k8s/secret.example.yaml` | 密码与 JWT 密钥示例（不提交真实值） |
| `k8s/deployment.yaml` | 滚动发布、三类探针、资源限制、优雅停机、安全上下文 |
| `k8s/service.yaml` | ClusterIP 服务 |
| `k8s/hpa.yaml` | CPU 70% 目标，2~10 副本 |

## 构建镜像

```bash
# 在仓库根执行，构建上下文必须是根目录
docker build -f docs/assets/container/Dockerfile \
  --build-arg MODULE=samples/demo-service \
  --build-arg SERVICE_PORT=8091 \
  -t baseon/demo-service:local .

docker run --rm -p 8091:8091 \
  -e SPRING_PROFILES_ACTIVE=local \
  baseon/demo-service:local
```

- `MODULE`：Maven 子模块路径，默认 `samples/demo-service`；
- `SERVICE_PORT`：同时写入 `SERVER_PORT` 与 `EXPOSE`/`HEALTHCHECK`；
- 构建阶段使用 BuildKit 缓存挂载 `.m2`，首次构建仍需下载全部依赖；
- 生产流水线可改为只复制已构建 Jar，运行阶段与本模板一致。

## 本地编排

```bash
docker compose -f docs/assets/container/docker-compose.yml up -d --build
docker compose -f docs/assets/container/docker-compose.yml ps

# 追加监控栈（Prometheus :9090 / AlertManager :9093）
docker compose -f docs/assets/container/docker-compose.yml --profile monitoring up -d
```

服务地址：demo-service `http://localhost:8091`，Nacos `http://localhost:8848/nacos`，MySQL `localhost:3306`，Redis `localhost:6379`。

## 部署到 K8s

```bash
kubectl create namespace baseon
# 先创建真实 Secret（不要 apply secret.example.yaml 的占位值）
kubectl -n baseon create secret generic baseon-demo-service-secret \
  --from-literal=BASEON_AUTH_SECRET="$(openssl rand -base64 48)" \
  --from-literal=BASEON_DATASOURCE_MASTER_PASSWORD='***' \
  --from-literal=BASEON_DATASOURCE_SLAVES_0_PASSWORD='***' \
  --from-literal=SPRING_CLOUD_NACOS_PASSWORD='***'

kubectl apply -f docs/assets/container/k8s/configmap.yaml
kubectl apply -f docs/assets/container/k8s/deployment.yaml
kubectl apply -f docs/assets/container/k8s/service.yaml
kubectl apply -f docs/assets/container/k8s/hpa.yaml
```

`deployment.yaml` 中的镜像地址、`imagePullSecrets`、`namespace`、数据源域名均为占位，按环境替换。

## 注意事项

1. **`/actuator/health` 被认证拦截会导致探针失败**：接入 `baseon-starter-auth` 时必须在 `BASEON_AUTH_IGNORE_PATHS` 中放行 health 端点（P-031）；
2. **`/actuator/prometheus` 需要额外依赖**：本仓库 demo 未引入 `micrometer-registry-prometheus`，直接抓取会 404；监控接入前先给业务工程加该依赖（P-032）；
3. **堆参数不要写死**：`JAVA_OPTS` 使用 `MaxRAMPercentage`，K8s `limits.memory` 变更后堆自动跟随（P-030）；
4. **Nacos 2.x 必须暴露 9848**：只映射 8848 时客户端 gRPC 连接失败，服务注册不上；
5. **Compose 中的凭据仅限本地**：Nacos Token、MySQL root、JWT secret 都是演示值，生产走 Secret/KMS；
6. **探针路径与 profile 相关**：`prod` 下 Swagger 与调试端点由 starter 硬关闭，探针只依赖 actuator；
7. **HPA 需要 metrics-server**：未部署时 CPU 指标为 unknown，副本数保持不变；
8. **MySQL init 脚本只执行一次**：仅在 `mysql-data` 数据卷首次初始化时运行，改脚本后需删除数据卷重建；
9. **Dockerfile 面向可执行服务模块**：`MODULE` 指向带 `spring-boot-maven-plugin:repackage` 的服务模块（如 `samples/demo-service`），不要指向纯库 Starter。
