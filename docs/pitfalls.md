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

---

## P-012 · Filter 抛出的异常到不了 @RestControllerAdvice

**现象**：starter-auth 的 `AuthTokenFilter` 校验失败时抛 `BizException(CommonErrorCode.UNAUTHORIZED)`，客户端收到的不是统一 JSON，而是 Tomcat 默认 500 HTML 错误页。

**根因**：`@RestControllerAdvice` 基于 `HandlerExceptionResolver`，只能处理 DispatcherServlet **分发之后**（拦截器、Controller）抛出的异常；Filter 处于 Servlet 容器层，异常不会进入 Spring MVC 的异常解析链。

**解决**：Filter 内认证失败时自行写响应：`response.setStatus(401)` + `Content-Type: application/json;charset=UTF-8` + 用 ObjectMapper 序列化 `R.fail(...)` 写入 body。

**规避**：凡是 Filter 层做的校验（鉴权、签名、防重放），失败响应必须自写 JSON；拦截器层（`HandlerInterceptor`）抛出的异常可以走 Advice，不用特殊处理。

---

## P-013 · Spring Boot 绑定 Map 配置时剥离 key 中的连字符

**现象**：Sentinel 热点参数规则配在 `spring.cloud.sentinel.datasource.param-flow.nacos.*` 下，启动后规则不加载，普通 flow 规则却正常。

**根因**：Spring Boot 的 relaxed binding 在把 yml 绑定到 `Map<String, ...>` 时会把 key 里的连字符剥掉（`param-flow` 实际绑定为 `paramflow`），而 SCA 按 `rule-type` 匹配 datasource key 用的是精确字符串，剥离后匹配失败、数据源静默不生效。普通类型绑定（绑定到 POJO 字段）不受影响，因为字段名与属性名之间有规范化映射。

**解决**：datasource 的 Map key 必须用纯字母数字（如 `paramflow`），`rule-type: param-flow` 的值不受影响。

**规避**：配置到 `Map<String, X>` 的 yml key 一律避免连字符；排查「配置了但没生效」类问题时，先怀疑 key 被规范化。

---

## P-014 · Nacos 3.x 移除了 v1 OpenAPI，脚本发布配置/登录要换 v3 路径

**现象**：沿用 Nacos 2.x 的脚本 `POST /nacos/v1/cs/configs` 与 `POST /nacos/v3/auth/login` 发布规则均返回 404。

**根因**：Nacos 3.x 服务端移除了 v1 OpenAPI；且登录接口的正确路径是 `POST /nacos/v3/auth/user/login`（不是 `/v3/auth/login`）。

**解决**：登录用 `POST /nacos/v3/auth/user/login` 拿 `accessToken`，发布配置用 `POST /nacos/v3/admin/cs/config?accessToken=...`，group 参数名为 `groupName`。

**规避**：对接 Nacos 3.x 的 OpenAPI 前先查官方 v3 文档，不要沿用 2.x 的路径记忆。

---

## P-015 · @Degrade 与 @CircuitBreaker 组合时，切面顺序决定熔断器能否看到失败

**现象**：下游停服后 `@CircuitBreaker` 熔断器永远不打开，日志里全是真实异常（Connection refused）穿透，无任何 `CallNotPermittedException`，也没有熔断告警。

**根因**：`DegradeAspect` 未声明 `@Order` 时默认 `LOWEST_PRECEDENCE`（最内层，最贴近方法），而 resilience4j-spring6 2.2.x 的 `CircuitBreakerAspect` 默认 order 是 `LOWEST_PRECEDENCE - 4`（字节码确认，**不是** 一些资料写的 -1）。Spring AOP 中 order 值小者为外层，于是熔断器在外、降级在内——CACHE 策略把异常吞掉返回缓存值，熔断器看到的是「成功」，永远不计数。

**解决**：`DegradeAspect` 声明 `@Order(Ordered.LOWEST_PRECEDENCE - 5)`，比熔断器小 1、稳处其外层：真实异常先经熔断器计数再被降级兜底；OPEN 后熔断器抛 `CallNotPermittedException` 同样由降级切面兜底返回缓存。

**规避**：自研切面与第三方库切面组合时，必须查目标库 aspect 的**字节码/源码级**真实 order（库文档经常过时），并在 javadoc 写明顺序约定；若业务通过 `circuitBreakerAspectOrder` 自定义了熔断器顺序，需同步审视本值。

---

## P-016 · Maven jar 插件不感知依赖 jar 更新，子模块改动后 demo 包需 clean package

**现象**：`baseon-starter-resilience` 修复后执行了 `mvn -pl baseon-starter-resilience install`（本地仓库 jar 已是新版），随后 `mvn -pl samples/demo-service package` 显示 BUILD SUCCESS，但打出的 fat jar 里仍然是**旧版** starter——类上没有新加的注解。

**根因**：maven-jar-plugin 的增量检查只看 `target/classes` 时间戳，不检查依赖 jar 是否更新；`BOOT-INF/lib` 里收编的是构建会话解析时的快照。只 package 不 clean 时旧 jar 原样保留。

**解决**：改了 starter 类代码后要重打 demo 包，用 `mvn -pl samples/demo-service clean package`；或先 `mvn install` 全量再 package。

**规避**：「install 了 starter 但 demo 行为没变」是本类问题的典型信号，验证手段：解压 fat jar 后 `javap -v` 目标类确认注解/常量值，或比对 `BOOT-INF/lib` 中 jar 的 MD5 与本地仓库是否一致。

**补充（6 期）**：reactor 全量 `clean install` 打出的 demo fat jar 偶发连 `BOOT-INF/classes/application.yml` 都缺失（plain jar 只有 class 没有资源），启动报 `No spring.config.import property has been defined`——同样是打包阶段不确定性，对 demo 模块单独再跑一次 `clean package` 即可恢复。

---

## P-017 · 测试篡改 JWT 签名时勿改末字符：Base64URL 填充位会让篡改失效

**现象**：`tamperedSignatureThrowsUnauthorized` 测试偶发失败（改过的 token 仍验签通过），同一份代码前一次构建全绿、后一次报 "Expecting code to raise a throwable"。

**根因**：HS256 签名 32 字节，Base64URL 编码后**末字符只含 4bit 有效数据、低 2bit 是填充位**。测试把末字符 `A`(000000) 改成 `B`(000001) 时，变化的 bit 恰好落在填充位上，解码出的 32 字节签名分毫未变，验签自然通过。只有签名末字符恰好是 A/B 时才触发，表现为概率性失败。

**解决**：篡改点改为**倒数第二个字符**（6bit 全是有效数据，改动必然破坏签名）。

**规避**：手写 Base64/Base64URL 篡改测试时，永远改非末尾字符；或干脆改 payload 段（payload 变了签名必然不匹配）。凡是「改一个字符预期失败」的测试，都要想一层编码填充位。

---

## P-018 · fastexcel 与 feign-form-spring 传递的 commons-io 版本分歧触发 dependencyConvergence

**现象**：引入 `baseon-starter-excel` 后 demo-service 构建被 enforcer 拦下：`Dependency convergence error for commons-io:commons-io`——feign-form-spring 传 2.11.0，fastexcel 传 2.16.1。

**根因**：父 POM 开启了 `dependencyConvergence` 规则，同构件全工程只允许一个版本；两个第三方各自传递了不同版本。

**解决**：父 POM dependencyManagement 显式锁定 commons-io 2.16.1（高版本，向后兼容性好，对 feign-form-spring 无影响）。

**规避**：新增第三方依赖后第一时间跑一次全量构建让 enforcer 暴露收敛问题；锁版本时优先锁高版本并注释说明来源分歧， commons-io/commons-lang3 这类工具库高版本覆盖低版本通常安全，但锁完要跑相关模块测试确认。

---

## P-019 · springdoc webflux 线 `api-docs.enabled=false` 会把 swagger-ui 整体拖死

**现象**：网关（springdoc webflux 2.5.0）配置 `springdoc.api-docs.enabled=false` 后启动直接失败：`No qualifying bean of type 'org.springdoc.core.properties.SwaggerUiConfigProperties'`。

**根因**：webflux 线的 UI 自动配置 Bean（`SwaggerUiConfigProperties` / `SwaggerUiConfigParameters` 等）整体受 api-docs 开关门控——api-docs 一关，这些配置 Bean 根本不注册，swagger-ui 随之报废。api-docs 与 swagger-ui 在 webflux 线不是两个独立开关。

**解决**：网关聚合场景保持 api-docs 默认开启（网关自身无业务接口，其 /v3/api-docs 只返回空文档，无暴露面），swagger-ui 承载各服务文档聚合即可。反向操作（业务服务只留 api-docs、关 swagger-ui）在 webmvc 线是安全的，已验证互不影响。

**规避**：springdoc 2.x 的 swagger-config 端点路径是 `/v3/api-docs/swagger-config`（挂在 api-docs 路径下），排查聚合 404 时先确认路径前缀，不要按 1.x 的 `/v3/swagger-config` 猜。

---

## P-020 · mybatis-plus-generator 不传递 MP 核心包，插件运行期 NoClassDefFoundError

**现象**：`baseon-generator-maven-plugin` 单测全绿，但 `mvn baseon:generate` 连真实库时报 `NoClassDefFoundError: com.baomidou.mybatisplus.core.toolkit.StringUtils`。

**根因**：`mybatis-plus-generator` 的 pom 不把 `mybatis-plus` 核心包作为传递依赖；而 `ConfigBuilder` 处理表元数据时内部用到 `toolkit.StringUtils`。单测走的是 core 纯库路径（假 TableMeta），不经插件 realm 类加载，所以暴露不出来。

**解决**：插件 pom 显式引 `com.baomidou:mybatis-plus`（父 POM dependencyManagement 与 MP 全家桶锁同版本线）。

**规避**：Maven 插件「单测绿 ≠ 运行绿」——单测不经过插件 realm 隔离类加载器。插件代码一旦触碰第三方库的内部 API，必须补一次真实 goal 级冒烟（连真实环境跑 `-Dtable=`）才算验完。

---

## P-021 · @SpringBootApplication 启动类不能为 HideUtilityClassConstructor 加 private 构造器

**现象**：接入 Checkstyle 后给启动类补 private 构造器消除 `HideUtilityClassConstructor` 违规，应用启动失败——CGLIB 无法创建配置类代理。

**根因**：启动类同时是 `@Configuration`，Spring 需要 CGLIB 子类化代理；private 构造器破坏了代理创建。而启动类只有 static main 方法，在 Checkstyle 眼里长得像工具类。

**解决**：启动类用 **protected** 构造器——`HideUtilityClassConstructor` 只拦 public/默认构造器，protected 既过规则又不破坏代理；纯静态工具类才用 private。

**规避**：修「工具类构造器」类违规时先辨认类身份：被 Spring 代理的类（`@Configuration`/`@SpringBootApplication`）一律 protected，别图省事用 private。
