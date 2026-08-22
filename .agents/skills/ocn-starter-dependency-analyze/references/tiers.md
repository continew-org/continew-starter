# 升级分级规则（Tier Rules）

本文是 `scan.py` 分级逻辑的唯一权威定义。脚本严格按此实现；若改规则，先改本文，再改脚本。

## 三级判定

对每个坐标，先算「同主线最新」与「当前 release」两个目标，再按目标定级：

| 级别 | 判定条件 | 目标版本 | 报告行为 |
|---|---|---|---|
| 🟢 补丁 | 当前 release 与锁定版本**同 major 且同 minor** | 当前 release（= 同主线最新） | 给「应用升级」按钮 |
| 🟡 小版本 | 当前 release 与锁定版本**同 major、跨 minor**（且不跨 major） | 同主线最新（保守目标）；release 作为「可进一步升到」提示 | 给「应用升级」按钮（目标=同主线最新）；展开升级要点 |
| 🔴 大版本 | 当前 release 与锁定版本**跨 major**（且 release 比 locked 新） | 报「同主线最新」（安全）+「当前 release」（需迁移）两个 | **不给应用按钮**，给迁移要点 + 命令供复制 |
| ⚪ 降级 | 当前 release 比**锁定版本还旧**（locked > release） | 无 | 单独提示「疑似项目重新编号/坐标指向不同分支」，不当升级处理 |

> `release` 指经 GA 过滤后的 `max(GA versions)`，不是 `<release>` 字段原始值。

## 三档目标并存（`targets`）

分级只决定**标题风险**，不决定「能看到几个目标」。每个坐标都独立计算三个目标（`build_targets()`）：

| key | 含义 | 算法 |
|---|---|---|
| `patch` | 🟢 同 major.minor 内最高 GA | `same_line_latest()` |
| `minor` | 🟡 同 major 内最高 GA | `same_major_latest()` |
| `major` | 🔴 同 lineage 内跨 major 最高 GA | `absolute_release()` |

任一档若等于锁定版本（无升级空间）或与更低风险档重复，置为 `null`，避免报告重复行。

报告的三个 tab（补丁/小版本/大版本）按 `targets` 切，**不是按 tier 切**：一个坐标只要有 patch 就进「补丁」tab，
有 major 就同时进「大版本」tab，各 tab 数量之和大于 `outdated` 总数，这是预期行为（`summary.available` 单记三计数）。
**同一坐标在不同升级 tab 各展示一份**（不聚合），每份只聚焦该档目标——切到「补丁」tab 时 Spring Boot 3.5.16 就在
那里，无需拼凑。

**🟡 按钮目标取「同主线最新」而非 release**：锁定 3.5.13、release 已到 3.7.2，一次性跳风险叠加，逐 minor 升更可控。
若锁定 minor 已是同主线最高，则 🟡 目标退化为 release。

**🔴 不给按钮**：跨 major 几乎必有破坏性变更，一键改 property + compile 过了也不代表没踩坑（compile 看不到运行时
行为），需用户读迁移要点自行核验，按钮在 🔴 上是反安全的。

## 版本语义解析

版本号形如 `MAJOR.MINOR.PATCH[-QUALIFIER]`：

1. 按开头的数字段切分，**保留 4 段**：`3.5.13` → `(3, 5, 13, 0)`；`1.33` → `(1, 33, 0, 0)`。
2. 忽略非数字后缀：`4.0.0-M1` → `(4, 0, 0, 0)`；`5.0.5-boot3` → `(5, 0, 5, 0)`。
3. 缺位补 0 比较：`1.33` 与 `1.33.0` 同版本。
4. **第 4 段必须保留**：`sonar-maven-plugin` 用四段号（`5.2.0.4988`），截断成三段会让所有 `5.2.0.x` 比较结果相同。

## 预发布版本过滤

Maven metadata 的 `<versions>` 混有 `M1/M2/RC1/Beta/alpha/temp1` 等预发布/临时版，且 `<release>` 字段不保证是 GA
（少数项目如 `jetcache-bom` 会把 `<release>` 指到 RC）。因此 `scan.py` 不用 `<release>` 原始值，而是从 `<versions>`
过滤出 GA 取 `max(GA versions)` 作为「当前 release」；仅当列表无任何 GA 时才回退 `<release>`。

GA 判定用**白名单式**正则 `PRERELEASE_RE`，只把已知预发布 qualifier 判为非 GA：

```
[-.](?:M|RC|CR|A|B|ALPHA|BETA|EA|PRE|SNAPSHOT|DEV|INCUBATING|TEMP)\d*$
```

命中的过滤掉：`4.0.0-M1`、`1.35.0.RC`、`1.34.0.temp1`、`2.0.0-Beta`、`3.0.0-SNAPSHOT`。

**不能用「`-` 后跟字母就是预发布」这种黑名单式判定**——那会把 `5.0.5-boot3`、`32.0.0-jre` 这类**发布支线
（lineage）**误杀成预发布。它们是正式 GA，只是并行的另一条线（见 [special-cases.md](special-cases.md) §5）。

## lineage（发布支线）隔离

后缀形如 `-<word>` 且不在预发布白名单里的，视为 lineage 标记（`lineage_of()`）。**版本只在同一 lineage 内比较**：
`5.0.5-boot3` 只与其它 `-boot3` 比，不与 `-boot2` 或纯数字版混比。若锁定版本所在 lineage 无任何 GA，才依次回退到
「全体 GA 最高版」→ `<release>` 字段。

## 同主线最新算法

```
given locked = (Lmajor, Lminor, Lpatch)
candidates = [v in metadata.versions if is_ga(v) and v.major == Lmajor and v.minor == Lminor]
same_line_latest = max(candidates)   # 按 (major, minor, patch) 字典序
```

仅匹配 `major == Lmajor and minor == Lminor`——锁定版本所在补丁线的最高版。🟡 的「保守目标」是「同 major 的最高
GA」（`max(同 major 的所有 GA)`），不是「同主线最新」。

## 平台级联动覆盖

即便版本语义算出 🟢/🟡，**平台级坐标**（Spring Boot、Spring Cloud）受联动约束覆盖，规则见
[special-cases.md](special-cases.md)。典型：Boot 升级必须配对应 Cloud release train，否则报告在执行建议拦下，不给
单独的 Boot 升级按钮。

## 互斥变体不重复检测

`data-mp`/`data-mf`、`excel-fastexcel`/`excel-poi`、`log-aop`/`log-interceptor` 是互斥实现，各 property 独立算，
报告在执行建议提示「二者择一」即可，不强制（见 [special-cases.md](special-cases.md) §2）。

## 已最新也展示 + 发布时间/陈旧度

**已最新坐标不隐藏**：一个坐标若 `best == locked`（无任何 GA 比当前版本新），仍进「✅已最新」tab，不参与任何升级
tab（其 `targets` 全空）。目的：让人一眼看到「哪些其实不用动」，并借陈旧度判断是否该换组件。报告里统一称 `locked`
为「**当前版本**」，不出现「锁定版本」字样。

**发布时间来源**：扫描时对每个坐标额外 HEAD 请求当前版本与各升级目标的 `.pom`，取 HTTP `Last-Modified` 响应头，
写入 `locked_released` 与 `released{tier}`；带 `_LM_CACHE` 按 `group:artifact:version` 去重（只各抓一次）。网络失败则
为 `null`，前端显示「未获取到」，不阻塞报告。

**展示形态**：卡片头部 = `当前 X.Y.Z（绝对日期 · 相对时间） → 升级 A.B.C（绝对日期 · 相对时间）`，相对时间形如
`今天 / 昨天 / 3天前 / 约5个月前 / 约2年前`，绝对+相对并存。当前版本发布时间算陈旧度；升级目标每行右侧标「发布于
日期（相对时间）」。

**陈旧度（staleness）徽章**——基于 `locked_released` 与今天之差：

| 区间 | 颜色 | 文案 |
|---|---|---|
| < 3 个月（<90 天） | 绿 `fresh` | 已发布 X 天 |
| 3–12 个月 | 黄 `mid` | 已发布约 X 个月 |
| ≥ 18 个月 | 红 `old` | 已发布约 X 个月 |

≥18 个月额外提示：「上游长期无新发布，可评估是否替换为更活跃组件」。

**排序规则**（纯前端渲染行为，不改扫描/分级）：
- 升级 tab（补丁/小版本/大版本）：按「落后程度」降序——先按版本缺口跨度（`gap_versions.length`）越大越前，再按
  当前版本发布时间越旧越前，让 drift 最远的浮到顶部。
- 已最新 tab：按当前版本发布时间从旧到新排，最该评估替换（陈旧度红）的排最前。
