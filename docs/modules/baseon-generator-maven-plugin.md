# baseon-generator-maven-plugin · CRUD 代码生成器

## 定位

按数据库表元数据一键生成符合脚手架规范的 CRUD 代码骨架（9 件套），消灭重复的样板编码。生成产物与 samples/demo-service 手写代码同风格：Controller 返回 `R<T>`/`PageResult<T>`，Entity 遵循审计字段父类惯例。

## 用法

```bash
# 短前缀（需先在 ~/.m2/settings.xml 的 <pluginGroups> 中加 io.github.ayongbox）
mvn baseon:generate -Dtable=t_tenant_user

# 全限定坐标（免配置，任何环境可用）
mvn io.github.ayongbox:baseon-generator-maven-plugin:1.0.0-SNAPSHOT:generate -Dtable=t_tenant_user
```

## 参数

| 参数 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `-Dtable` | 是 | — | 表名，逗号分隔多张表 |
| `-Dmodule` | 否 | 空 | 子模块名；非空时输出到 `<outputDir>/<module>/...` 且包名追加模块段 |
| `-DbasePackage` | 否 | `com.example.demo` | 生成代码的根包 |
| `-DoutputDir` | 否 | `${project.build.directory}/baseon-generated` | 输出根目录 |
| `-DdbUrl` | 否 | `jdbc:mysql://127.0.0.1:3306/baseon_master` | 数据库连接 |
| `-DdbUser` / `-DdbPassword` | 否 | `root` / `root` | 数据库账号 |

## 输出（9 件套）

| 文件 | 要点 |
|---|---|
| `entity/Xxx.java` | 表名去 `t_`/`tb_` 前缀转大驼峰；含 `tenant_id` 列时继承 `TenantEntity`，否则继承 `BaseEntity`；审计列不重复生成 |
| `mapper/XxxMapper.java` | 继承 `BaseMapper` |
| `service/XxxService.java` + `service/impl/XxxServiceImpl.java` | 标准 CRUD 方法签名 |
| `controller/XxxController.java` | REST 风格，返回 `R<T>`/`PageResult<T>` |
| `converter/XxxConverter.java` | MapStruct，`@Mapper(componentModel="spring")` |
| `req/XxxReq.java` / `vo/XxxVO.java` / `resp/XxxResp.java` | 出入参分离 |

类型映射：bigint→Long、int/smallint→Integer、tinyint(1)→Boolean、varchar/text/json→String、decimal→BigDecimal、datetime/timestamp→LocalDateTime、date→LocalDate。**未映射的列类型（blob/time 等）显式抛错**，按提示补 `JdbcTypeMapper`，不静默降级。

## 内部结构（扩展指引）

```
generator.core    纯库：TableMeta/ColumnMeta 模型、NamingConverter、JdbcTypeMapper、
                  CrudRenderer（freemarker）。零 Maven/MP 依赖，单测直接喂假 TableMeta
generator.meta    MybatisPlusTableMetaReader：MP ConfigBuilder 读表元数据 → core 模型
generator.mojo    GenerateMojo 薄壳：参数解析 → 读元数据 → 渲染 → 落盘（同名覆盖）
templates/*.ftl   9 个 freemarker 模板，改样式只动模板不动 Java
```

架构约束由 ArchUnit 固化：core 包不得依赖 `org.apache.maven..` / `com.baomidou..`。

## 注意事项

1. **不要删插件 pom 里的 `com.baomidou:mybatis-plus` 依赖**：MP generator 不传递核心包，删了运行期 `NoClassDefFoundError`（P-020）。
2. **goalPrefix=baseon 是显式锁定的**：artifactId 默认推导的前缀是 `baseon-generator`，改成别的名字前想清楚（pom 中 maven-plugin-plugin 的 goalPrefix 配置）。
3. 生成文件同名直接覆盖——不要手改生成目录里的文件后再跑生成器。
