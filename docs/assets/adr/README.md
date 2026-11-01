# 架构决策记录（ADR）

## 文件

| 文件 | 状态 | 内容 |
|---|---|---|
| `template.md` | — | 新增 ADR 的固定结构 |
| `0001-revision-and-bom.md` | 已采纳 | `${revision}` 统一版本 + BOM 对外 + 插件版本锁定 |
| `0002-core-zero-spring.md` | 已采纳 | core 零 Spring，`core.boot` 外置为 `baseon-core-boot` |
| `0003-mechanism-policy-spi.md` | 已采纳 | 框架承载机制、业务通过 SPI 提供策略 |
| `0004-autoconfig-and-hard-disable.md` | 已采纳 | 条件装配 + 生产危险能力双层硬失效 |
| `0005-test-dual-track.md` | 已采纳 | 单测零 Spring；集成测试 H2 / Testcontainers 双轨 |
| `roadmap.md` | 活文档 | 已交付期次、按需挂起项、GA 出口条件 |
| `anti-patterns.md` | 活文档 | 使用脚手架时的反模式清单 |

## 新增步骤

1. 复制 `template.md`，命名 `NNNN-短横线标题.md`，编号四位递增且不复用；
2. 只记录**已发生或已拍板**的决策，提案写「状态：提议」；
3. 决策被推翻时新开 ADR，旧 ADR 状态改为「被 ADR-NNNN 取代」，不修改历史结论；
4. 同一提交内更新本索引表；
5. 涉及代码结构的决策必须给出验证方式（ArchUnit 规则、构建命令或测试类）。

## 状态定义

- **提议**：已讨论未落地，不作为事实引用；
- **已采纳**：代码与构建已按此执行；
- **已废弃**：曾生效但不再推荐，历史代码可能仍在；
- **被取代**：由编号更大的 ADR 替代，旧文件保留。
