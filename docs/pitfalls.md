# 工程踩坑记录

> 只记录**非显而易见、且会重复发生**的问题。每条含：现象 / 根因 / 解决 / 规避。
> 新增条目编号递增，同时追加到 `CHANGELOG.md` 对应迭代的「注意事项」。

---

## P-001 · 导入 BOM 后插件版本仍然失控

**现象**：项目已 import `spring-boot-dependencies`，但构建日志里出现 `spring-beans-7.1.0-M2.jar`、`jackson 3.1.6` 等里程碑版本；同时提示 `'build.plugins.plugin.version' for org.springframework.boot:spring-boot-maven-plugin is missing`。

**根因**：`<scope>import</scope>` 导入 BOM 时，**只传递 `dependencyManagement`，不传递 `pluginManagement`**。插件未声明版本时，Maven 通过仓库元数据解析最新版（含里程碑版）。

**解决**：在父 POM 的 `pluginManagement` 中显式锁定所有插件版本。

**规避**：新增任何构建插件（ArchUnit、Checkstyle、SpotBugs、CycloneDX、japicmp、surefire）时，同步补 `pluginManagement` 条目与 `${xxx.version}` 属性。

---

## P-002 · 内核 POJO 的 `isSuccess()` 变成响应字段

**现象**：接口返回 `{"code":0,"msg":"操作成功","data":"...","success":true}`，多出 `success` 字段，与接口契约不符。

**根因**：Jackson 把 `isXxx()` 视为布尔属性 `xxx` 并序列化。`baseon-core` 零第三方依赖，无法使用 `@JsonIgnore`。

**解决**：方法重命名为 `success()`（非 getter 命名），响应体恢复为 `code/msg/data`。

**规避**：内核 POJO 中所有非属性方法都不使用 `is` / `get` / `set` 前缀；需要判断语义时用 `success()`、`hasXxx()` 之外的命名。

---

## P-003 · 本地 curl 请求被代理劫持

**现象**：`curl http://localhost:8080/demo/hello` 报 `Failed to connect to 127.0.0.1 port 7897`，但服务实际监听 8080。

**根因**：shell 中设置了 `http_proxy` / `all_proxy=127.0.0.1:7897`（Clash 等代理工具），curl 把 localhost 请求也转发给代理。

**解决**：单次请求加 `--noproxy '*'`；或临时清空 `http_proxy= https_proxy= all_proxy= curl ...`。

**规避**：在 `~/.zshrc` 中设置 `export NO_PROXY="localhost,127.0.0.1,::1"` 与 `no_proxy` 同名变量。Maven 下载依赖遇到超时也先排查代理。

---

## P-004 · 父 POM 不能 import 自家 BOM

**现象**：为消除版本重复维护，尝试让 `baseon-parent` 通过 `<scope>import</scope>` 引入 `baseon-bom`，干净环境首次构建失败（无法解析 BOM）。

**根因**：`dependencyManagement` 的 import 在工程模型构建阶段解析，此时同一 reactor 中的 `baseon-bom` 尚未安装到本地仓库。

**解决**：父 POM 自己维护内部构件版本（`${revision}`），`baseon-bom` 面向外部业务工程单独维护，两处同步更新。

**规避**：新增内部构件时，同时在 `baseon-parent` 与 `baseon-bom` 登记版本；CI 增加一致性检查（后续阶段）。

---

## P-005 · 函数式接口重载导致 lambda 歧义

**现象**：`ContextWrapper.wrap(SecurityUtil::getTenantId)` 编译失败，提示 `The method wrap(Runnable) is ambiguous for the type ContextWrapper`。

**根因**：`wrap(Callable<T>)` 与 `wrap(Supplier<T>)` 都是「无参、有返回值」的函数式接口，方法引用无法推导出唯一目标类型，编译器判定歧义。

**解决**：Supplier 重载改名为 `wrapSupplier(Supplier<T>)`，调用方显式区分。

**规避**：公共 API 不要同时暴露多个「仅函数式接口不同」的重载；需要支持多种函数形态时使用不同方法名（`wrap` / `wrapSupplier`），或在同一个方法内用适配器收敛。

---

## P-006 · 请求日志里的中文变成乱码

**现象**：接口响应 `{"msg":"操作成功"}` 正常，但 `RequestLogFilter` 打印的响应体是 `{"msg":"æä½æå"}`。

**根因**：用 `response.getCharacterEncoding()` 解码缓存字节，Tomcat 在未显式设置 charset 时返回 `ISO-8859-1`，而实际响应字节是 UTF-8。

**解决**：响应体固定按 UTF-8 解码；请求体仍按 `request.getCharacterEncoding()`（浏览器/客户端会正确声明）。

**规避**：日志、消息队列、文件写入等场景不要信任容器的默认编码，显式指定 UTF-8；只有确实来自外部声明的编码才动态解析。

---

## P-007 · Spring Cloud 4.x 中 LoadBalancerClientFactory 包名迁移

**现象**：编译报错 `找不到符号: 类 LoadBalancerClientFactory，位置: 程序包 org.springframework.cloud.client.loadbalancer`。

**根因**：`org.springframework.cloud.client.loadbalancer.LoadBalancerClientFactory` 在 Spring Cloud Commons 3.x 中已被标记废弃，4.x（Cloud 2022+）正式移除，新位置是 `org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory`。

**解决**：两处 import 改为 `org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory`。

**规避**：使用在旧版本中被 `@Deprecated` 的框架类时，先确认当前版本是否仍存在；升级 Spring Cloud 大版本后优先全局搜索 `org.springframework.cloud.client.loadbalancer` 包下的引用。

---

## P-008 · 引入 nacos config 后启动报 No spring.config.import property

**现象**：引入 `spring-cloud-starter-alibaba-nacos-config` 后启动失败：`No spring.config.import property has been defined`。

**根因**：SCA 配置中心默认开启 import-check（`spring.cloud.nacos.config.import-check.enabled=true`），强制要求显式声明 `spring.config.import`，防止用户误以为配置中心已生效。

**解决**：声明 `spring.config.import: optional:nacos:<dataId>.yaml`，`optional:` 前缀允许配置不存在也能启动；确实不用配置中心时显式关闭检查。

**规避**：只要依赖里带了 nacos config，就必须在 application.yml 写 `spring.config.import`，即使是纯注册中心使用场景。

---

## P-009 · Nacos 3.x 控制台端口与客户端端口分离

**现象**：`server-addr` 配置为控制台地址 `localhost:8181`，服务启动注册时报 `NacosException: Client not connected, current status:STARTING`。

**根因**：Nacos 3.x 拆分了端口：控制台 UI 独立端口（本机 8181），客户端 API 与 gRPC 在主端口（默认 8848，gRPC 为 9848/9849）。客户端连控制台端口时 HTTP 可达（返回 404/500 误导判断），但 gRPC 握手失败。

**解决**：`spring.cloud.nacos.server-addr` 指向主端口 `localhost:8848`；浏览器访问控制台仍用 8181。

**规避**：区分「给人用的控制台地址」与「给客户端用的 server-addr」；排查注册问题时先 `lsof` 确认服务端真实监听端口，客户端 gRPC 端口 = 主端口 + 1000。

---

## P-010 · MyBatis-Plus 3.5.9 拦截器被拆到独立构件

**现象**：引入 `mybatis-plus-spring-boot3-starter:3.5.9` 后，`PaginationInnerInterceptor` / `TenantLineInnerInterceptor` / `TenantLineHandler` 编译报「程序包不存在」。

**根因**：MP 自 3.5.9 起把依赖 JSqlParser 的拦截器拆分到独立构件 `mybatis-plus-jsqlparser`，starter 不再传递引入。

**解决**：父 POM `dependencyManagement` 追加 `com.baomidou:mybatis-plus-jsqlparser:${mybatis-plus.version}`，使用方模块显式声明依赖（不写版本）。

**规避**：升级 MP 版本时先核对官方「版本差异」说明；`IgnoreStrategy` 在 `com.baomidou.mybatisplus.core.plugins` 包（不在旧版的 `core.toolkit`）。

---

## P-011 · AbstractRoutingDataSource 读不到 @Transactional(readOnly=true)

**现象**：读写分离场景中，`@Transactional(readOnly = true)` 方法依然路由到主库；`TransactionSynchronizationManager.isCurrentTransactionReadOnly()` 在路由时永远返回 false。

**根因**：`DataSourceTransactionManager.doBegin()` 获取连接（触发路由决策）时，readOnly 标记还没注册——`prepareSynchronization` 在 `doBegin` **之后**才把 readOnly 写入同步上下文。时序上路由决策必然早于标记可见。

**解决**：`ReadOnlyRouteAspect` 以 `@Order(HIGHEST_PRECEDENCE)` 在事务拦截器之前执行，读 `@Transactional` 注解元数据，把只读标记写入 `DataSourceContextHolder`，路由数据源凭标记轮询从库。嵌套事务（synchronization 已激活）不切换——连接已由外层事务绑定。

**规避**：任何「依赖事务属性做数据源路由」的方案都不能在 `determineCurrentLookupKey` 里读事务同步状态；同类库（dynamic-datasource）的处理口径是「事务内强制走主库」，与本期方案的差异在于读多写少场景的成本取舍。
