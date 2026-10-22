# Baseon 脚手架文档

> 本目录是脚手架的**活文档**，与代码同一仓库、同一迭代节奏演进。
> 架构总纲见仓库根目录《通用SpringCloud企业微服务脚手架.md》，本目录只记录「当前已实现什么、怎么用、有什么坑」。

## 目录结构

```
docs/
├── README.md             本文档：导航 + 当前能力矩阵
├── CHANGELOG.md          版本能力台账：每次迭代新增能力与注意事项
├── modules/              模块手册：一个模块一篇，记录用法、配置、边界
│   └── baseon-core.md
├── pitfalls.md           工程踩坑记录：现象 / 根因 / 解决 / 规避
└── assets/               CI、容器、告警等运维资产（后续阶段补充）
```

## 当前进度

| 期 | 内容 | 状态 |
|---|---|---|
| 0 期 | 根 POM + BOM + demo 验证工程 | ✅ 完成 |
| 1 期 | baseon-core（model / tool / context）+ ArchUnit | 🟡 进行中（boot/cloud/db/secure/launch/spi 未建） |
| 2 期 | starter-web（异常、Jackson、上下文、日志、CORS）+ starter-trace（TraceId、MDC、异步透传） | ✅ 完成 |

## 能力矩阵

| 构件 | 能力 | 状态 |
|---|---|---|
| `baseon-parent` | revision 统一版本、flatten 展开、enforcer 构建约束 | ✅ 可用 |
| `baseon-bom` | 依赖版本单一入口（供业务工程 import） | ✅ 可用 |
| `baseon-core` · model | `R<T>`、`PageResult<T>`、`PageQuery`、错误码与 `ErrorType`、`BizException`、实体父类、`LoginUser` | ✅ 可用 |
| `baseon-core` · tool | `StrUtil`、`MoneyUtil`、`BeanCopyUtil` | ✅ 可用 |
| `baseon-core` · context | `SecurityUtil`、`ContextWrapper` | ✅ 可用 |
| `baseon-core` · boot | `ThreadPoolFactory`、`TransactionUtil`、`SpringContextHolder` | ⬜ 未实现 |
| `baseon-core` · cloud/db/secure/launch/spi | 对应抽象契约 | ⬜ 未实现 |
| `baseon-starter-web` | 全局异常、Jackson 统一、用户上下文、请求日志脱敏、CORS | ✅ 可用 |
| `baseon-starter-trace` | TraceId、MDC、线程池/异步/定时任务透传 | ✅ 可用 |

## 文档维护约定

1. **同一次提交内必须同步**：新增/修改能力 → 更新 `CHANGELOG.md` + 对应 `modules/*.md`；
2. **踩坑必记录**：调试中发现的非显而易见问题，沉淀到 `pitfalls.md`，编号递增；
3. **文档只写当前事实**：不写「计划要做的能力」，计划类内容归入 `CHANGELOG` 的「待办」区；
4. **示例可运行**：模块手册中的代码片段应能在 `samples/demo-service` 下直接编译通过。
