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

---

## P-022 · Feign 链路断裂：缺 feign-micrometer 时下游另起 trace

**现象**：接入 OTel 分布式追踪后 A 经 Feign 调 B，A 的 traceId（响应头、业务日志）与 B 的 traceId 不一致——自定义头 `X-Trace-Id` 透传值是对的，但 B 的 MDC/span 用的是另一个全新 ID，追踪后端里链路断成两截。

**根因**：spring-cloud-openfeign 的观测能力（client span 创建 + W3C `traceparent` 头注入）由独立工件 `io.github.openfeign:feign-micrometer` 承载，`spring-cloud-starter-openfeign` 本身不含。缺失时 Feign 调用不注入 `traceparent`，下游的 observation 过滤器解析不到传播头，只能新建 trace。脚手架自定义头透传（`X-Trace-Id`）与 W3C 传播是两条独立通道，前者正常不代表后者正常。

**解决**：`baseon-starter-tracing-otel` 聚合 `feign-micrometer`（版本由 spring-cloud BOM 经 feign-bom 管理，无需手写）；它是纯能力包不含 web 栈，不用 Feign 的服务引入后自动不生效。

**规避**：验证分布式追踪链路时，必须对比「下游 span 的 traceId 是否等于上游」，仅看上游自身日志对齐不够。`traceparent` 格式为 `00-{traceId}-{spanId}-{flags}`，排查时可直接抓包看这个头有没有注入。

---

## P-023 · 继承 BaseMultiTableInnerInterceptor 必须自行实现四个 process 方法，基类默认抛 UOE

**现象**：`DataScopeLineInnerInterceptor extends BaseMultiTableInnerInterceptor` 只重写了 `buildTableExpression`，一跑 `parserSingle` 就抛 `UnsupportedOperationException`。

**根因**：`BaseMultiTableInnerInterceptor` 只提供多表处理骨架（抽象 `buildTableExpression`），`processSelect/processInsert/processUpdate/processDelete` 四个入口在基类里默认抛 UOE——它假设子类照搬 `TenantLineInnerInterceptor` 的完整实现。

**解决**：解压本地 `mybatis-plus-jsqlparser-*-sources.jar` 参考 `TenantLineInnerInterceptor`，补齐四个 process 方法（行级权限只保护读，insert/update/delete 空实现；processSelect 里处理 Select body + WithItem 列表），`beforeQuery` 里调 `parserSingle` 改写 SQL。

**规避**：继承 MP 多表拦截体系前先读官方 `TenantLineInnerInterceptor` 源码；单测入口用公共方法 `parserSingle(sql, null)`，别直接调 protected 的 processSelect（编译不过且绕过真实链路）。

---

## P-024 · jsqlparser 5.1 构造 IN 表达式必须用 ParenthesedExpressionList，否则渲染无括号

**现象**：`new InExpression(column, expressionList)` 生成的 SQL 是 `DEPT_ID IN 20, 10`（无括号），数据库直接语法错误。

**根因**：jsqlparser 5.x 中普通 `ExpressionList` 渲染时不自带括号；带括号的 IN 列表由 `ParenthesedExpressionList` 承载。

**解决**：`new ParenthesedExpressionList<>(items)` 包装后再传给 `InExpression`；列名构造对齐 MP 官方 `getAliasColumn`——表无别名时返回裸列名，别强行拼表名前缀。

**规避**：jsqlparser 版本升级后 AST 类形态会变（4.x 的 ExpressionList 泛型 → 5.x 的 ParenthesedExpressionList），升级后先跑一遍 SQL 改写单测再动其他逻辑；断言 IN 内容时别依赖 `Set.of` 的元素顺序（跨 JVM 不稳定），分别断言关键片段包含。

---

## P-025 · AOP 双切点 @annotation || @within 下注解参数会为 null

**现象**：`@Around("@annotation(dataScope) || @within(dataScope)")` 写法下，只标类级注解的方法被切中时 `dataScope` 参数为 null，环绕通知里 NPE。

**根因**：`@annotation` 绑方法级注解、`@within` 绑类级注解；两个切点或连接时，Spring 无法把注解实例绑定到统一参数，未命中的来源注入 null。

**解决**：环绕方法不接收注解参数，改为手动解析——`MethodSignature.getMethod().getAnnotation()` 优先取方法级，miss 时回退 `signature.getDeclaringType().getAnnotation()` 取类级。注意 `getDeclaringType()` 返回原生 Class，getAnnotation 结果是原始 Annotation，需显式强转。

**规避**：同一注解要同时支持方法级和类级时，固定用「无参 around + 手动 resolveAnnotation」模式；单测 mock ProceedingJoinPoint 时给 MethodSignature 配真实方法对象（FixtureClass 挂真实注解），别 mock getMethod 返回 null 埋雷。

---

## P-026 · Jackson 字段裁剪不能走 changeProperties：BeanSerializer 被缓存，裁剪结果固化到首个请求

**现象**（设计期规避）：字段级数据权限若用 `BeanSerializerModifier.changeProperties` 按当前用户裁字段，该回调只在序列化器首次构建时执行一次——Jackson 全局缓存 BeanSerializer，后续请求复用，裁剪结果会固化成第一个请求用户的可见字段。

**根因**：BeanSerializer 的构建（changeProperties）与使用（serialize）是两个阶段，构建结果按 Class 全局缓存；任何「随请求上下文变化」的裁剪都必须落在 serialize 阶段。

**解决**：改用 `modifySerializer` 包装返回的 BeanSerializer——外层自定义 serializer 在每次 serialize 时读线程上下文（DataScopeContext + visibleFields），经委托对象的 `properties()` 迭代器逐字段判断后输出；无上下文时直接透传委托。

**规避**：Jackson 定制凡依赖请求态（登录人/租户/权限）的逻辑，一律放 serialize 运行期求值；changeProperties 等构建期回调只能做与请求无关的静态裁剪。无 spring-web 依赖的模块注册定制走 Jackson 原生 `Module`（SimpleModule.setSerializerModifier），别引 Jackson2ObjectMapperBuilder。

---

## P-027 · 集成测试 schema.sql 被 spring.sql.init 与 @Sql 双重执行，数据翻倍断言全挂

**现象**：SpringBootTest 集成测试查全表返回 8 行而预期 4 行——建表脚本里的种子 INSERT 跑了两遍。

**根因**：`spring.sql.init.schema-locations` 已在应用上下文启动时执行过 schema.sql；测试类又加 `@Sql(scripts = "schema.sql", executionPhase = BEFORE_TEST_METHOD)`，每个用例前再执行一次，种子数据重复插入。

**解决**：二选一——启动级初始化交给 `spring.sql.init.*`（删 `@Sql`），或只留 `@Sql` 关掉 spring.sql.init。

**规避**：Boot 2.5+ 的 sql.init 与 `@Sql` 是两套独立机制、互不感知；集成测试看到「数据莫名翻倍 / 主键冲突」先查这两处是不是配重了。

---

## P-028 · testcontainers 1.19.x 不提供独立 redis 模块，RedisContainer 无法解析

**现象**：集成测试基类引 `org.testcontainers:redis` + `RedisContainer`，Maven 报 `'dependencies.dependency.version' for org.testcontainers:redis:jar is missing`，编译期 `RedisContainer` 找不到符号。

**根因**：Spring Boot 3.2.x 托管的 testcontainers 1.19.8 BOM 只列了 `testcontainers`、`mysql`、`junit-jupiter` 等，未包含 `redis` 子构件（1.19 起 redis 容器不再作为独立模块发布）。

**解决**：Redis 改用核心包的 `GenericContainer` 启动：`new GenericContainer<>("redis:7").withExposedPorts(6379)`，端口取 `getMappedPort(6379)`，无需额外依赖。

**规避**：用 testcontainers 某中间件前先确认它在当前 BOM 版本里是否有独立模块；不在就用 `GenericContainer` + 官方镜像，核心包 `org.testcontainers:testcontainers` 始终由 BOM 托管。

---

## P-029 · DataSourceTransactionManager 默认不支持 NESTED：保存点事务抛 NestedTransactionNotSupportedException

**现象**：调用 `TransactionUtil.executeInNestedTransaction(...)` 抛 `NestedTransactionNotSupportedException: Savepoint support not enabled`，而 REQUIRED / REQUIRES_NEW 正常。

**根因**：Spring `DataSourceTransactionManager` 的 `nestedTransactionAllowed` 默认为 `false`，Boot 自动装配创建的事务管理器同样保持该默认值；NESTED 传播行为依赖 JDBC Savepoint，未显式放行时直接拒绝。

**解决**：注册 `DataSourceTransactionManagerCustomizer<DataSourceTransactionManager>` 或自定义 `PlatformTransactionManager` Bean，调用 `setNestedTransactionAllowed(true)`；同时确认数据库与连接池支持 Savepoint（H2、MySQL + HikariCP 均支持）。

**规避**：优先用 REQUIRES_NEW（独立事务）或业务补偿实现「局部失败不影响整体」；确实需要 Savepoint 语义时才开启 NESTED，并在使用该能力前验证底层驱动。

---

## P-030 · 容器里写死 -Xmx，调整 limits 后堆不跟随导致 OOMKilled 或利用率虚低

**现象**：K8s `limits.memory` 从 1Gi 调到 2Gi 后，Pod 仍按 512m 堆运行，容器内存利用率长期低于 50%；或反过来，镜像里固定 `-Xmx2g` 但 limit 只有 1Gi，Pod 被 OOMKilled 且 JVM 无堆 dump。

**根因**：JVM 的容器感知只决定**默认**堆大小（`MaxRAMPercentage`，默认 25%），显式 `-Xmx` 会覆盖该计算；镜像、部署清单、启动脚本三处任一写死都会让堆与 limit 脱钩。

**解决**：镜像与 K8s 统一使用 `-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0`，不写 `-Xmx`；`-XX:+ExitOnOutOfMemoryError` + `-XX:HeapDumpPath` 让 OOM 可观测。

**规避**：见 `docs/assets/container/Dockerfile` 与 `k8s/deployment.yaml`；更换运行环境后先用 `java -XX:+PrintFlagsFinal -version | grep MaxHeapSize` 确认实际堆。

---

## P-031 · 接入 starter-auth 后 K8s 探针 401，Pod 反复重启

**现象**：`/actuator/health` 在本地直接访问返回 200，部署到 K8s 后 readiness/liveness 持续失败，Pod 进入 `CrashLoopBackOff` 或不断被摘流；日志里没有业务异常，只有 401。

**根因**：`baseon-starter-auth` 默认对除 `ignore-paths` 外的所有路径要求登录，`/actuator/**` 不在默认放行列表；探针不带 Token，因此每次请求都被拒绝。Docker `HEALTHCHECK`、k6 压测脚本同样受影响。

**解决**：把探针路径加入 `baseon.auth.ignore-paths`（health、liveness、readiness 三个都要），或使用独立的未认证管理端口（`management.server.port`）并只在内网暴露；k6 的 `PERF_HEALTH_PATH` 指向放行后的路径。

**规避**：新增任何探针/压测入口时，先确认它是否在 `ignore-paths` 里；见 `docs/assets/container/k8s/configmap.yaml`。

---

## P-032 · Prometheus 抓取成功但业务指标缺失，告警规则静默空转

**现象**：告警规则上线后从未触发，`up{job="baseon-services"}` 一直为 1；人工制造 5xx 也没有告警，直到故障复盘才发现规则从未满足。

**根因**：`up=1` 只代表抓取请求成功；若 `management.endpoints.web.exposure.include` 未包含 `prometheus`、未引入 `micrometer-registry-prometheus`，或指标名与实际不符，规则引用的序列根本不存在，PromQL 结果为空，告警永远不会进入 pending。

**解决**：接入前在 Prometheus 查询 `count(http_server_requests_seconds_count)`、`count(jvm_memory_used_bytes)`、`count(baseon_mybatis_slowsql_total)`（慢 SQL 需 micrometer 注册表）确认有数据；再用 `promtool check rules` 与测试规则验证。

**规避**：告警规则与指标暴露在同一次变更中评审；`docs/assets/alerting/README.md` 的阈值表与 Prometheus `/api/v1/targets` 页面应作为上线检查项。

---

## P-033 · 新模块只依赖 spring-boot 构件，编译报「程序包 org.slf4j 不存在」

**现象**：新增 `baseon-core-launch` 只声明了 `spring-boot` / `spring-boot-autoconfigure`（optional），编译期 `LoggerFactory` / `Logger` 全部找不到符号。

**根因**：`spring-boot` 构件本身**不传递** `slf4j-api`——Boot 的日志门面由 `spring-boot-starter-logging` 带入，而 starter 又由业务工程的 `spring-boot-starter-*` 传递。只依赖 `spring-boot` 的「轻量外置构件」类路径里没有 slf4j。

**解决**：凡代码里使用 slf4j 的非 starter 模块，显式声明 `org.slf4j:slf4j-api`（optional，版本由 Boot BOM 管理），见 `baseon-core-launch/pom.xml`。

**规避**：新建外置构件（core.* 物理外置、工具库）时按「编译期最小闭包」自查：spring-boot 不带 slf4j、spring-context 不带 spring-tx；缺的依赖一律 optional 显式声明，不依赖下游传递。

---

## P-034 · rocketmq-client 不传递 rocketmq-acl，ACL 编译报「程序包 org.apache.rocketmq.acl 不存在」

**现象**：只声明了 `rocketmq-client`，使用 `AclClientRPCHook` / `SessionCredentials` 时编译找不到 `org.apache.rocketmq.acl.common` 包；按旧记忆从 `org.apache.rocketmq.remoting.common` import `SessionCredentials` 同样失败。

**根因**：`rocketmq-acl` 是**独立构件**，`rocketmq-client`（4.9.7）的 pom 只传递 rocketmq-common / commons-lang3 / opentracing，ACL 支持必须单独声明；且 4.x 起 `SessionCredentials` 在 `org.apache.rocketmq.acl.common` 包，不在 remoting 包。

**解决**：显式声明 `org.apache.rocketmq:rocketmq-acl`（本仓库版本由父 POM `rocketmq.version` 统一管控），import 使用 `org.apache.rocketmq.acl.common.AclClientRPCHook` / `SessionCredentials`，见 `baseon-starter-mq-rocketmq/pom.xml`。

**规避**：接入第三方客户端的「可选增强能力」（ACL、tracing 插件等）时，先 `mvn dependency:tree` 确认该能力构件是否真在传递闭包内，不要凭主构件存在推断。

---

## P-035 · DefaultMQPushConsumer 开 ACL 报「找不到构造器」，AllocateMessageQueueAveragely 导包错误

**现象**：按直觉写 `new DefaultMQPushConsumer(group, new AclClientRPCHook(...))` 编译失败，javap 确认 4.9.7 无 `(String, RPCHook)` 两参构造器；补负载策略时从 `org.apache.rocketmq.client.consumer` 包 import `AllocateMessageQueueAveragely` 也找不到。

**根因**：RocketMQ 4.9.7 的 `DefaultMQPushConsumer` 开启 ACL 必须用三参构造器 `(String group, RPCHook, AllocateMessageQueueStrategy)`；且 `AllocateMessageQueueAveragely` **实现类**在 `org.apache.rocketmq.client.consumer.rebalance` 包，只有 `AllocateMessageQueueStrategy` 接口在 `consumer` 包。

**解决**：

```java
new DefaultMQPushConsumer(group,
        new AclClientRPCHook(new SessionCredentials(ak, sk)),
        new AllocateMessageQueueAveragely())   // import 自 consumer.rebalance 包
```

**规避**：第三方客户端的构造器与实现类包路径以 `javap` / `unzip -l` 实际产物为准，官方文档与博客常滞后于版本；见 `MqConsumerRegistrar#createConsumer`。
