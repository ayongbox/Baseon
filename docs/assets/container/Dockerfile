# syntax=docker/dockerfile:1.7
# Baseon 多阶段镜像模板（JDK 17）。
#
# 构建上下文必须是仓库根目录（不是本文件所在目录）：
#   docker build -f docs/assets/container/Dockerfile \
#     --build-arg MODULE=samples/demo-service \
#     -t baseon/demo-service:local .
#
# 生产建议在 CI 中用 `mvn -DskipTests package` 产出 Jar 后只构建运行阶段，
# 避免把源码与 Maven 缓存带进构建机；本模板保留一体化构建以便本地复现。

ARG JAVA_VERSION=17
ARG MODULE=samples/demo-service

FROM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS build

ARG MODULE
WORKDIR /workspace

# 先复制全量源码：多模块依赖需要 reactor 内的兄弟模块
COPY . .

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp -DskipTests -pl "${MODULE}" -am package \
    && mkdir -p /workspace/dist \
    && JAR=$(find "${MODULE}/target" -maxdepth 1 -name '*.jar' \
        ! -name '*sources.jar' ! -name '*javadoc.jar' | head -n 1) \
    && test -n "${JAR}" \
    && cp "${JAR}" /workspace/dist/app.jar

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS runtime

ARG SERVICE_PORT=8091
ARG APP_USER=baseon

ENV SERVICE_PORT=${SERVICE_PORT} \
    SERVER_PORT=${SERVICE_PORT} \
    TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -Dfile.encoding=UTF-8 -Dspring.output.ansi.enabled=never"

WORKDIR /app

RUN addgroup -S "${APP_USER}" \
    && adduser -S -G "${APP_USER}" "${APP_USER}" \
    && mkdir -p /tmp \
    && chown "${APP_USER}:${APP_USER}" /app /tmp

COPY --from=build /workspace/dist/app.jar /app/app.jar

USER ${APP_USER}

EXPOSE ${SERVICE_PORT}

# 探针走存活检查；接入 baseon-starter-auth 时必须把路径加入 baseon.auth.ignore-paths（P-031）
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${SERVICE_PORT}/actuator/health" || exit 1

# exec 让 java 成为 PID 1，SIGTERM 能触发 Spring 优雅停机
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
