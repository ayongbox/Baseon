# Skills

本目录存放可被多个工程复用的 skill，随 Baseon 仓库做版本控制。

## baseon-consumer

- 用途：所有消费 Baseon 的业务工程共用，涵盖 BOM 引入、starter 选型、配置前缀、core 契约、运行期顺序与陷阱。
- 全局入口：`~/.trae-cn/skills/baseon-consumer` 软链指向本目录下的 `baseon-consumer/`。
- 维护规则：涉及 starter 增删、配置前缀变更或新增陷阱编号时，必须同步更新本目录下的 `baseon-consumer/`，与代码、`docs/modules/`、`docs/pitfalls.md`、`docs/CHANGELOG.md` 在同一次提交内完成。
- 全局侧无需额外操作，软链自动指向最新内容。
- 修改 Baseon 仓库自身时参考 `.trae/skills/baseon-architecture/`（本地规约，不随仓库分发）。
