# baseon-archetype-clean / baseon-archetype-sample · 项目原型模板

## 定位

两个 maven-archetype，对应两类起点：

| 原型 | 场景 | 内容 |
|---|---|---|
| `baseon-archetype-clean` | 老项目迁移/只想要空骨架 | pom（import baseon-bom）+ Application + application.yml + logback-spring.xml |
| `baseon-archetype-sample` | 全新项目，想要可运行参考 | clean 全部 + 示例 Controller/Service/单测（`R<T>`、`PageQuery` 分页、`BizException`） |

出口标准：新项目从生成到起服务 ≤ 5 分钟。

## 用法

```bash
mvn archetype:generate -B \
  -DarchetypeGroupId=io.github.ayongbox \
  -DarchetypeArtifactId=baseon-archetype-sample \
  -DarchetypeVersion=1.0.0-SNAPSHOT \
  -DgroupId=com.example -DartifactId=my-app -Dversion=1.0.0 \
  -Dpackage=com.example.myapp \
  -DbaseonVersion=1.0.0-SNAPSHOT   # 可省略，默认 1.0.0-SNAPSHOT；指定脚手架版本

cd my-app && mvn spring-boot:run
# sample: GET http://localhost:8080/example/hello → {"code":0,"msg":"操作成功","data":"hello baseon"}
```

## 生成工程说明

- **依赖**：spring-boot-starter-web + baseon-starter-web + baseon-starter-trace（sample 另加 test scope 的 spring-boot-starter-test）；版本全部经 baseon-bom import 管控，生成 pom 里不出现任何第三方版本号。
- **插件版本**：BOM 不传递 pluginManagement（P-001），生成 pom 显式锁定 compiler/surefire/spring-boot 插件版本；升级 BOM 基线时需同步模板（archetype-resources/pom.xml）。
- **logback-spring.xml**：复制 starter-trace 内置 pattern（含 `%X{traceId:-}`），仅加 local/dev 下业务包 DEBUG；删掉该文件即回退 starter-trace 内置格式。生产环境不要在本文件配置文件 appender。
- **分页入参**：示例用 `PageQuery`，字段名是 `current`/`size`（MyBatis-Plus 风格），不是 pageNum/pageSize。

## 模板维护

- 模板文件在 `src/main/resources/archetype-resources/`，其中 `${package}`、`${artifactId}`、`${baseonVersion}` 等占位符在生成时替换（archetype-metadata.xml 控制 filtered/packaged）。
- 修改模板后验证：`mvn -pl baseon-archetype-clean,baseon-archetype-sample install`，然后按上文命令实跑一次生成 + 构建。
- archetype-resources 下的模板工程不在 Checkstyle/ArchUnit 扫描范围内（非源目录），风格靠人工对齐。
