---
name: ocn-starter-dependency-analyze
description: >
  依赖升级分析：检测 continew-starter 仓库依赖是否落后、生成排版精美的交互式「依赖升级分析」HTML
  报告（🟢补丁/🟡小版本/🔴大版本 三级分类，每项含变更要点与可一键复制的 POM 修改命令），并可在网页上点
  「应用升级」自动改 POM（即按分析结果改 version property，完成"选定依赖"这步）。交互统一为"先跨 tab
  勾选进「待升级清单」，再统一升级"（每 tab 可全选本档；同一依赖多 tab 出现时只保留一个目标，其余档位禁用）。
  本技能只做分析与改 POM，**不编译、不修源码、不验升级**——改完 POM 后请调用 ocn-starter-dependency-upgrade
  做真实升级（编译、查 changelog、修源码、install）。务必在以下场景使用：用户说"检查依赖版本"
  "依赖升级分析""依赖更新""看看哪些依赖落后了""升级 Spring Boot / Hutool / MyBatis-Plus 等依赖"
  "BOM 版本检查""maven 依赖更新""依赖有没有新版本""Central 上最新版本是多少""第三方库版本"
  "检查 starter 依赖""analyze dependencies""check outdated""bump version"；或用户抱怨某个库有 bug
  想升版本、想定期巡检依赖、想生成依赖升级分析报报告给团队看时。注意：本 skill 专用于 continew-starter
  仓库，读 continew-starter-dependencies/pom.xml 里 ~43 个版本 property 做检测，不适用于其它 Maven 项目。
---

# 依赖升级分析（Dependency Analyze）

检测 `continew-starter-dependencies/pom.xml` 里被 `<properties>` 集中管理的 ~43 个第三方依赖坐标是否落后于
Maven Central，生成交互式依赖升级分析报告，并提供「应用升级」写操作（仅改 version property，把"选定依赖"
这一步落地）。

## 铁律

- **版本数据源只有 Maven Central 的 `maven-metadata.xml`**：不调 search.maven.org 的 solrsearch（目标网络超时），
  不抓 GitHub Releases。当前 release = `max(GA versions)`，不直接信 `<release>`/`<latest>`（常被指向 RC/预发布）。
- **升级是分级决策，不是版本罗列**：每个坐标按版本语义落到 🟢补丁 / 🟡小版本 / 🔴大版本，同时独立报三档目标
  `patch`/`minor`/`major`（各自带变更记录链接与 POM 命令）。只报一个 target 会掩盖同主线安全补丁——典型如
  Spring Boot 锁 `3.5.13` 判 🔴（Central 有 `4.1.0`），但仍必须显示 `patch=3.5.16`，那才是用户最该先升的。
- **区分预发布 qualifier 与发布支线（lineage）**：`-M1/-RC1/-Beta/-SNAPSHOT` 是预发布不能当目标；`-boot3`、
  `-jre` 是并行发布支线、本身是 GA，且只在同 lineage 内比较版本。黑名单式「`-`后跟字母即预发布」会把
  `5.0.5-boot3` 误杀出假降级告警。
- **构建插件（flatten/spotless/sonar）与普通构件走完全相同的检测、分级、应用流程**，property 在同一个
  `<properties>` 块里。
- **写操作在当前分支直接改 POM，默认不跑 mvn**：「应用升级」直接改 property，不建分支、不提交、不推送、不开 PR，
  不要求工作区干净；🟢/🟡 各有一键批量按钮，🔴 故意不给按钮（无论单项还是批量）。改完用户自行跑一次
  `mvn compile` 验证，回退只需 `git checkout` 该 POM。仅当请求带 `{"compile":true}` 才跑 mvn。
- **路径、命令、版本号保留原文不翻译。**

分级算法、版本解析、预发布过滤、tab 切分、排序、陈旧度等详细规则以 [references/tiers.md](references/tiers.md)
为唯一权威；联动/互斥/BOM 等例外见 [references/special-cases.md](references/special-cases.md)。

## 执行流程

### Step 1 扫描（联网，只读）

```bash
python .claude/skills/ocn-starter-dependency-analyze/scripts/scan.py -o dep_scan.json
```

`scan.py` 解析 `<properties>` 锁定值 → 反查 `groupId:artifactId` → 抓各坐标 maven-metadata → 算分级与三档目标 →
merge 坐标目录的升级要点 → 输出 JSON（schema 见脚本顶部注释）。`-o` 以 **UTF-8 直接落盘**，供 Step 3 读取。
约 43 个坐标需十几秒；抓失败的坐标标 `unreachable` 并在报告里列出。**全程只读，不改 POM。**

> **Windows 编码坑**：不要用 shell 的 `>` 重定向捕获 scan 输出——PowerShell 默认写成 **UTF-16**，`server.py`/
> `build_report.py` 会读不了（虽然它们已兼容 UTF-8/UTF-16，仍建议用 `-o` 一步到位，避免产生两个同名文件）。
> 同理，调试时避免用 `python -c "多行 JSON 处理"`，PowerShell 对内嵌引号转义易出错。

### Step 1.5 官方工具链交叉验证（可选但推荐）

`scan.py` 只靠 maven-metadata 比版本，存在盲区：插件版本完全没审计、个别坐标 metadata 抓取失败会漏报、
且无法发现「已声明未使用 / 未声明已使用」依赖与依赖冲突。用标准 Maven 插件做交叉验证，结果并入 Step 2 分析与报告：

```bash
# 1) 官方过时依赖 + 插件审计（覆盖 scan.py 的插件盲区）
mvn -B -q versions:display-dependency-updates versions:display-plugin-updates 2>&1 | Out-File -Encoding utf8 .tmp_versions.txt

# 2) 依赖分析：找「已声明未使用 / 未声明已使用」依赖（升级后 spotless 漂移、缺传递依赖的先兆）
mvn -B -q dependency:analyze 2>&1 | Out-File -Encoding utf8 .tmp_depanalyze.txt

# 3) 依赖树冲突快照（供升级技能 Step 1.5 比对升级前后差异）
mvn -B -q dependency:tree 2>&1 | Out-File -Encoding utf8 .tmp_deptree.txt
```

- 读 `.tmp_versions.txt`：与 `dep_scan.json` 的目标版本**交叉核对**——官方报有更新但 scan 没列的，补进报告；
  插件更新单独成节（scan.py 不覆盖插件）。
- 读 `.tmp_depanalyze.txt`：列出 `Used undeclared dependencies`（运行时隐患）与 `Unused declared dependencies`
  （可清理、升级后易产生未用 import 漂移）。升级前清理 unused 能减少 spotless 噪音。
- `.tmp_deptree.txt` 留作基线，交给 upgrade 技能对比升级后是否引入冲突 / 多版本共存。

### Step 1.6 安全维度（CVE）标注

`scan.py` 不含安全信息。对下列库，在报告里**显式标注安全维度**并给可选验证命令：
- 跨 major（🔴）或已知历史 CVE 的库（如 Log4j、commons-text、SnakeYAML、Spring 系列），注明
  「升级到目标版本可修复已知 CVE，建议优先」。
- 需要精确 CVE 列表时，可选跑 OWASP Dependency-Check（本仓库无 test，属重操作，默认不跑，仅给出命令）：

```bash
mvn -B org.owasp:dependency-check-maven:check -DfailOnError=false 2>&1 | Out-File -Encoding utf8 .tmp_depcheck.txt
```

报告「安全建议」栏放置上述结论；CVE 详情以 `dependency-check` 输出为准，不臆测。

### Step 2 分析与分级

读 [references/tiers.md](references/tiers.md) 确认分级判定，读 [references/coordinates.md](references/coordinates.md)
拿各坐标的升级约束与迁移要点，然后看扫描 JSON 做这几件事：

1. **挑 Top 5 落后大户**：按「版本缺口跨度 + 是否跨 major」综合排序，判类型。
2. **识别平台级联动**：Spring Boot 与 Spring Cloud 强绑定，只升 Boot 不升 Cloud 必须在执行建议里拦下并给配套
   Cloud 版本（规则见 [references/special-cases.md](references/special-cases.md)）。
3. **三级分类**：🟢 同 major.minor 安全给按钮；🟡 同 major 跨 minor 展开升级要点后给按钮；🔴 跨 major 给迁移
   要点、**不给按钮**。注意 🔴 项整项判红，但它的 patch 档仍给按钮——风险与目标是分开的。
4. **并入官方交叉验证（Step 1.5）**：凡 `versions:display-dependency-updates` 报有更新但 scan 未列的，补进对应档；
   插件更新单列「插件审计」小节。`.tmp_depanalyze.txt` 里的 `Unused declared dependencies` 在报告里标「升级前建议清理」，
   `Used undeclared dependencies` 标「运行期隐患」。
5. **并入安全维度（Step 1.6）**：跨 major / 已知 CVE 历史库在卡片加「🔒 升级可修复已知漏洞，建议优先」标记；
   精确 CVE 需 `dependency-check` 输出，不臆测。

### Step 3 生成交互报告

直接吃 Step 1 的 `dep_scan.json`（`build_analysis` 内部完成转换），默认用一键应用模式：

```bash
python .claude/skills/ocn-starter-dependency-analyze/scripts/server.py dep_scan.json   # 自动开浏览器，Ctrl+C 停
```

`server.py` 起在 127.0.0.1 + 随机端口 + 随机 token；🟢/🟡 目标给「应用升级」按钮，🔴 不给。
**交互统一为「先选择、再统一升级」**（已移除逐档「一键批量」按钮）：每个升级卡片头部有勾选框，可在
补丁/小版本/大版本任一 tab 勾选加入右下角「待升级清单」，每 tab 顶部有「☑ 全选本档」；在清单里可增删，
最后点「⚡ 统一升级」一次性 POST 批量写入。同一依赖会出现在多个 tab（如 Spring Boot 在补丁与小版本/大版本），
一旦在某 tab 选中，它在其它 tab 的同名勾选框即禁用提示；清单以 `property` 为唯一键，只保留一个目标版本。
🔴 大版本项可加入清单仅作统一浏览，但「统一升级」按安全规则跳过、不自动应用（需自行迁移）。
安全模型（白名单、批量先整批校验、token/Host 校验）见脚本顶部注释，此处不重复。改完不跑 mvn。

**复用/留存**：报告页右上角有「**⬇ 导出报告**」按钮，一键把当前页面导出为自包含的静态 HTML
（`APPLY=null`，无应用按钮，可单独打开/分享）。也可用静态模式生成文件：

```bash
python .claude/skills/ocn-starter-dependency-analyze/scripts/build_report.py dep_scan.json ~/Desktop/dep-report.html \
  && start ~/Desktop/dep-report.html   # Windows；macOS 用 open
```

**排障：网页没有「应用升级」按钮** = 要么开了静态报告（改用 `server.py`），要么该项/该档是 🔴（红灯故意不给）。

报告布局、tab 切分、排序、陈旧度展示等均为前端渲染行为，以 [references/tiers.md](references/tiers.md)
和 `assets/report_template.html` 为准，执行时无需在对话里复述。

### Step 4 对话里给摘要

报告生成后，在对话里用一段结论先行的摘要：可安全升级的项数、最该先升的 2-3 项、风险最高的一项（如
Spring Boot 跨大版本）。若跑了 Step 1.5/1.6，额外点出：①官方 `versions` 插件审计出的**插件更新**与 scan 漏报项；
②`dependency:analyze` 的 unused/undeclared 依赖；③**安全维度**（哪些 🔴 库升级可修复已知 CVE，建议优先）。细节让用户看网页。

### Step 5 后续真实升级闭环（移交 upgrade 技能）

应用升级、POM 改完后，**编译验证与破坏性变更修复不属于本技能**（见 ADR 0003 compile 改为 opt-in）。
明确告知用户：改完 POM 后请调用 **`ocn-starter-dependency-upgrade`** 技能承接后续——
它会 `mvn compile`，若报错则抓对应库官网 changelog 并修源码，再跑 `mvn install` + `spotless:check`
+ `-Psonar` 全链路验证。两技能共享 `references/coordinates.md` 的迁移要点与 `special-cases.md` 的
Spring Boot↔Cloud 兼容矩阵，无需重复提供上下文。

## 运行前提

- **Python 3 标准库**，零第三方依赖（仅 `urllib`/`xml.etree`/`re`/`json`）。
- **联网**：Step 1 需访问 `repo1.maven.org:443`；离线时 `scan.py` 用内置坐标目录生成「无最新版」快照并标注离线。
- **git + mvn**：默认写操作只改文件，不调 git/mvn；仅 `{"compile":true}` 路径需本机 `mvn`。纯读报告不需要。
- **Windows**：命令用 `python`（不是 `python3`），macOS/Linux 自行改 `python3`。

## 何时读哪个 reference

| 想知道 | 读 |
|---|---|
| 某坐标的 groupId/artifactId/升级约束/迁移要点 | [references/coordinates.md](references/coordinates.md) |
| 🟢🟡🔴 判定、同主线最新、预发布过滤、tab 与陈旧度 | [references/tiers.md](references/tiers.md) |
| Spring Boot↔Cloud 联动、互斥变体、BOM vs artifact | [references/special-cases.md](references/special-cases.md) |

## 维护提示

- **新增依赖坐标**：在 `continew-starter-dependencies/pom.xml` 加 property + dependencyManagement 条目后，同步在
  [references/coordinates.md](references/coordinates.md) 加一小节，并在 `scan.py` 的 `COORDINATES` 字典补映射。
  漏了仍能分级，只是报告里该坐标「升级要点」为空。
- **上游发大版本**：在 coordinates.md 对应小节补「迁移到 X.Y 的要点」，这是本 skill 唯一需要人工跟进的维护面。
- **ADR**：架构决策在仓库根 `docs/adr/`（0001 数据源、0002/0003 写模式），改动铁律前先看 ADR。
- **三目录镜像**：按 `CLAUDE.md` / `AGENTS.md` 的「Skill mirroring」约定，改完 `.claude/skills/`（源）后把
  SKILL.md 与相关文件同步到 `.agents/skills/`、`.codebuddy/skills/` 并保持字节一致。
