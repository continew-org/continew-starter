---
name: ocn-starter-dependency-upgrade
description: >
  依赖升级（真实升级闭环）：在 POM 的 version property 已改（由 ocn-starter-dependency-analyze 技能的「应用升级」
  或用户手动改）之后，执行 mvn compile → 若报错则定位到具体依赖的破坏性变更 → 抓取该依赖官网 changelog /
  release notes / migration guide（优先 gh API compare 差集 + jar 反查兜底）→ 给出源码修复方案并落地 →
  跑 mvn install（spotless 随 compile 自动跑）+ 可选的 -Psonar 验证（与升级质量解耦）。
  本技能也处理"用户直接问怎么升级"的场景：当用户未提供已改好的 POM、只说"帮我升 spring-cloud / hutool 之类"时，
  本技能应**先调用 ocn-starter-dependency-analyze** 做检测与报告，确认要升的目标版本后再执行真实升级。务必在以下
  场景使用：用户说"升级后验证""改完 POM 编译报错""帮我修升级后的编译错误""依赖升级后怎么改""怎么升级这些库"
  "verify the upgrade""upgrade broke the build""compile failed after bump"；或用户直接贴出"升了这些库：
  spring-cloud 2025.0.1→2025.0.3 …"要求去验证修复时。注意：本技能负责"决定升谁"（可自调 analyze）+"改 POM
  版本号"+"改源码"+"编译验证"，是依赖升级的终点。专用于 continew-starter 仓库，依赖坐标目录与 analyze 技能共享。
---

# 依赖升级（Dependency Upgrade）

`ocn-starter-dependency-analyze` 把 POM 的 `<properties>` 版本号改完只是第一步（它只做分析 + 改 POM）。
本技能承接后续：让这次升级**真正通过编译并 install 到本地仓库**，遇到破坏性变更时主动去上游官网查 changelog
并修复本仓库源码。当用户未先跑分析、直接问"怎么升级"时，本技能会先调 analyze 拿到目标版本，再继续。

## 与 analyze 技能的边界

| 技能 | 职责 | 写什么 |
|---|---|---|
| `ocn-starter-dependency-analyze` | 检测落后、生成报告、勾选、**改 POM version property** | 只改 `continew-starter-dependencies/pom.xml` 的 `<properties>` |
| `ocn-starter-dependency-upgrade`（本技能） | 决定升谁（可自调 analyze）、改 POM、编译验证、查 changelog、修源码、install 验证 | 改 POM `<properties>` + 业务 `.java` / `.xml` / `.yml` |

**共享资源**：本技能直接读 analyze 技能目录下的三个 reference，不复制：
- `../ocn-starter-dependency-analyze/references/coordinates.md` —— 各坐标的「迁移要点」是查官网 changelog 的起点
- `../ocn-starter-dependency-analyze/references/special-cases.md` —— 平台级联动、互斥变体、BOM 陷阱
- `../ocn-starter-dependency-analyze/references/tiers.md` —— 分级语义（仅参考，本技能不分级）

**输入来源**（二选一，都解析成「坐标 → 旧版 → 新版」三元组）：
1. 用户直接贴文本列表，如「spring-cloud 2025.0.1 => 2025.0.3, hutool 5.8.44 => 5.8.47 …」
2. `git diff continew-starter-dependencies/pom.xml` 自动提取改动的 `<version property>` 旧值→新值

优先用方案 2（精准、不漏），若用户明确给了列表则用方案 1。两者都拿不到时，提示用户二选一提供。

## 铁律

- **compile 通过 ≠ 升级成功**：compile 看不到运行时行为（auto-config 条件、SPI 加载、反射）。但本仓库无
  `src/test`、无 demo 模块，  故以 `mvn compile` + `mvn install`（spotless 随 compile 自动跑）全过为验收线；若 compile 报错，必须先
  修到全过，再报「建议人工跑 continew-admin 应用做运行期验证」（本技能无权做运行期验证）。`-Psonar` 因
  服务器不可达而失败时与升级质量无关，跳过不阻塞。
- **查官网 changelog 是诊断的一部分，不是可选项**：compile 报「找不到符号 / 方法不存在 / 类找不到」几乎都
  是 API 被删/改名/签名变。必须抓该依赖对应版本的 release notes / migration guide，定位具体 breaking change，
  再改本仓库代码。**禁止凭猜测盲改**（如乱加 `@SuppressWarnings`、强转、注释掉报错行来骗过编译）。
- **平台级联动优先**：若改动含 `spring-boot.version` 或 `spring-cloud.version`，先按
  `special-cases.md §1` 校验二者是否匹配；不匹配时先拦下，要求用户确认 Cloud train 与 Boot 版本区间兼容，
  再继续 compile。
- **spotless 会改写源码**：`mvn compile` 阶段绑定 `spotless:apply`（见 AGENTS.md），修完源码后跑
  `mvn spotless:check` 确认格式化无大面积漂移；若漂移，跑 `mvn spotless:apply` 再 review diff。
- **路径、命令、版本号、类名保留原文不翻译。**
- **不动 version property**：本技能只修升级引发的编译/运行不兼容，绝不回改用户已选定的目标版本（除非用户
  明确要求回退某个库）。

## 执行流程

### Step 0 环境自检（changelog 抓取前提）

Step 4 要用 `gh api`（首选）或 urllib 拉 GitHub compare / release notes。先确认本机状态，缺失时**主动引导用户补齐**，不要闷头用未鉴权 API 直到撞限流才说。

```bash
# 1) gh 是否安装 + 是否已登录（鉴权状态一目了然）
gh auth status --hostname github.com
# 2) 若连 urllib 都不通（极少见），确认网络/代理
python -c "import urllib.request; urllib.request.urlopen('https://api.github.com/rate_limit', timeout=15); print('net ok')"
```

判定与引导：

| 状态 | 表现 | 引导动作 |
|---|---|---|
| gh 未安装 | `gh : 无法将“gh”识别为...` | 提示：`choco install gh` 或 `winget install --id GitHub.cli`；装完再 `gh auth login` |
| gh 已装但**未登录 / token 失效** | `gh auth status` 显示 `X Failed to log in` 或 `token ... invalid` | 若 keyring 里已有账号（如 `Charles7c`），跑 `gh auth refresh -h github.com` 复用账号刷新 token（比重新 login 快）；否则 `gh auth login`（按提示选 GitHub.com、HTTPS、用浏览器/粘贴 token） |
| gh 可用 | `gh auth status` 显示 `✓ Logged in` | 无需动作，Step 4 自动走 `gh api` |
| 仅 urllib 可用 | gh 不可用但 `net ok` | 照常跑，限流 60 次/小时，足够单次升级；若批量升很多库建议先修 gh |

引导话术示例（gh 失效时）：「本机 `gh` 已安装但登录态失效（token 无效）。请先执行 `gh auth refresh -h github.com` 刷新凭证；完成后告诉我，或你刷新好后我再继续 Step 4。期间我可用未鉴权 API 先跑，但限流较低。」

**注意**：不要替用户自动执行 `gh auth login/refresh` 以外的写操作；鉴权涉及用户凭证，须用户自己跑。脚本侧（`changelog_fetch.py`）检测到 `gh` 存在但 401 时也会打印同样的刷新提示，不静默吞错。

### 入口判定：要不要先调 analyze

- **情形 A（已改好 POM）**：用户贴出"升了这些库：…"文本列表，或工作区 `git diff` 已改 `continew-starter-dependencies/pom.xml` 的 version property → 直接进入 Step 1。
- **情形 B（用户只说"帮我升 X / 怎么升级这些库"，未改 POM）**：本技能应**先调用 `ocn-starter-dependency-analyze`** 做检测 + 报告 + 勾选，确认要升的目标版本（必要时等用户确认清单），拿到 analyze 产出的「待升级清单」后，由本技能执行应用升级（改 POM property）+ 后续编译验证。即情形 B = analyze（检测）+ 本技能（改 POM + 修源码 + 验证）。
- 无论 A/B，本技能最终都要走到 Step 1→Step 5 的完整闭环。

### 前置：升级前基线快照（事务性保护）

源码修复会改多个 `.java`/`.xml`，为避免"改一半编译不过又不知改了哪些"，进入 Step 1 前：
1. 记基线：`git diff --stat` 或 `git stash list` 现状，确认起点干净（或明确告知用户当前已有未提交改动）。
2. **每修完一个库**（Step 3 循环里）建议 `git add -A && git commit -m "build(deps): bump X x.y.z→x.y.w"` 做 checkpoint；
   任一步失败可 `git reset --hard <checkpoint>` 单行回退，而不是手工撤销散落的源码改动。
3. 禁止在"全量编译仍报错"的状态下结束会话——要么修到 compile 干净，要么显式告知用户"卡在 X，需人工决策"。

### Step 1 收集「升了哪些库」

按上面「输入来源」拿三元组列表 `[(property, group:artifact, old, new), ...]`。

用脚本从 git diff 提取（推荐）：
```bash
python .codebuddy/skills/ocn-starter-dependency-upgrade/scripts/diff_versions.py
```
脚本解析 `git diff continew-starter-dependencies/pom.xml`，只抓 `<xxx.version>` property 的 `-`/`+` 行，
输出 JSON 三元组；无 git 或 diff 为空则提示改用文本列表。

若用户给了文本列表，调用：
```bash
python .codebuddy/skills/ocn-starter-dependency-upgrade/scripts/parse_list.py "spring-cloud 2025.0.1 => 2025.0.3, hutool 5.8.44 => 5.8.47"
```
解析常见分隔符（`=>`/`->`/`→`/`to`/空格）与「库名↔coordinates.md 映射」生成三元组。

拿到列表后，先对其中每个坐标读 `coordinates.md` 对应小节的「迁移要点」与 `constraints`，标记哪些库**已知有
破坏性变更风险**（如 Hutool 6.x、Spring Boot 4.x、POI 5→6）。这一步是"预判"，compile 前先心里有数。

### Step 1.5 升级前影响面预检（强制，先于任何 POM 改动）

> 生产级闭环的第一道闸门。**这一步解决"编译通过了也不知道踩没踩完坑"的问题**：
> 坐标改名 / BOM 拆包 / 上游删类，compile 全都不报错，必须升级前主动预检。

```bash
python .codebuddy/skills/ocn-starter-dependency-upgrade/scripts/diff_versions.py -o .tmp_upgrade_plan.json
python .codebuddy/skills/ocn-starter-dependency-upgrade/scripts/precheck.py --plan .tmp_upgrade_plan.json -o .tmp_upgrade_precheck.json
```

`diff_versions.py` 现在**同时输出两类变更**，二者都要看：
- `version_bumps`：版本号变更（原行为）。
- `coordinate_renames`：**groupId / artifactId 改名**（原脚本完全看不见的盲区）。
  例如 nextdoc4j 1.2.0→1.4.1 把 `nextdoc4j-bom-springboot3` 改名 `nextdoc4j-bom`、
  `nextdoc4j-springboot3-starter` 改名 `nextdoc4j-spring-boot-starter`，还把 springdoc-openapi
  拆成新传递依赖——这类改动不会让 compile 报错，但运行期 classpath 会缺东西。

`precheck.py` 做**主动 jar 反查（粗筛）**：扫描本仓库 `src` 里 import 的上游全限定类名，
解压 old/new 两个 jar 比对，命中"new 缺失（类被整个删除）"即为破坏性变更，**在改 POM 之前**就预警。
> ⚠ 局限：jar 反查只能抓"类被整个删除"，**抓不了"类被改名/移动/方法签名变更"**
>（类还在 jar 里但包路径或方法没了）。这类破坏性变更是 `changelog_fetch.py` compare API 的强项——
> 对 🔴 大版本、或任何"compile 报错后又修好的库"，Step 4 必须再用 compare API 的 removed/renamed
> 文件列表对照本仓库 import，补足 jar 反查的盲区。两者互补，不可互相替代。

**依赖冲突 / 重复快照（建议 2，针对坐标改名与拆包场景）**：坐标改名（如 nextdoc4j）常把依赖拆出来，
可能造成新坐标与既有依赖**版本冲突**或**多版本共存**。升级前先抓基线树，升级后（Step 3 每升一个库）对比：

```bash
# 升级前基线（对每个将升级的 group 各抓一份，或一次性全量）
mvn -B -q dependency:tree 2>&1 | Out-File -Encoding utf8 .tmp_deptree_before.txt
# 升级后（Step 3 每升完一个库）再抓，diff 比对
mvn -B -q dependency:tree 2>&1 | Out-File -Encoding utf8 .tmp_deptree_after.txt
```

重点看升级库 group 下是否出现 `(omitted for conflict)` 或多版本并列（如 `springdoc-openapi 2.7.0` 与
某 starter 自带的 `2.8.0` 共存）。出现则需在 dependencies POM 的 `<dependencyManagement>` 里**强制统一版本**，
或在引用方加 `<exclusions>`。这一步能抓到 jar 反查和 compile 都抓不到的"运行期 classpath 冲突"。

预检结论处理：
- 若 `coordinate_renames` 非空：必须同步改**所有引用方 POM**（dependencies + 各 starter 的 `<dependency>`），
  并核对新坐标是否拆分/新增了传递依赖（如 nextdoc4j 新增 springdoc-openapi）。**改完引用方再进 Step 3**。
- 若 `precheck.json` 的 `removed_symbols` 非空：列出具体类，进 Step 3 前先准备源码修复方案。
- 若 jar 本地不可用（`jar_unavailable`）：先 `mvn install` 旧版或 `mvn dependency:get` 新版，再跑预检；
  拿不到就显式标注「未做符号反查，风险未知」，不能假装安全。

### Step 2 平台级联动校验

若列表含 `spring-boot.version` 或 `spring-cloud.version`：
- 读 `special-cases.md §1` 的 `SPRING_CLOUD_COMPAT` 矩阵（Boot 代 → Cloud train）。
- 若二者不匹配，**暂停**并明确告知用户：「升级了 X 但 Y 未同步，运行期会炸，请先确认兼容矩阵再继续」。
  不擅自替用户决定升哪个。

### Step 3 逐个库升级 + 逐个 compile（隔离归因）

> 不要一次性改完所有 version property 再统一 compile——那样 50 个错归并到"哪个库"
> 只能靠 import 前缀猜，不可靠（Hutool 与 Boot 都可能改通用类）。**逐个库升级、逐个编译**，
> 锁定每个破坏性变更的真正来源。

1. 取 Step 1.5 的 `version_bumps` 列表，**一次只升一个库**：改它对应的 `<xxx.version>` property。
2. 只编译**直接受该库影响的模块**（用 `-pl :<artifact> -am compile`），不要全量 compile，加速反馈。
3. 解析报错（同下），修完源码后再升下一个库。
4. **每升完一个库，抓一次 after 树并和 Step 1.5 的 before 树 diff**（尤其坐标改名/拆包库）：
   `python -c "import difflib;...` 不直观，直接用 `git diff --no-index .tmp_deptree_before.txt .tmp_deptree_after.txt`
   看该 group 下是否新增 `(omitted for conflict)` 或多版本共存，有则先在 `<dependencyManagement>` 统一版本。
5. 全部库升完后，跑一次全量 `mvn -B compile` 兜底。

```bash
mvn -B compile 2>&1 | tee /tmp/verify-compile.log
```
（Windows 用 `mvn -B compile` 直接看输出，无需 tee；日志可存到 `.tmp_verify_compile.log` 避开水印文件）

解析失败：
- 提取 `ERROR` / `cannot find symbol` / `package X does not exist` / `incompatible types` / `method X in Y
  cannot be applied` 等关键行，连同文件名:行号。
- 把每个报错归并到「它属于哪个升级库」——靠 import 语句的 package 前缀（`org.springframework.cloud.*` →
  spring-cloud；`cn.hutool.*` → hutool）与报错类所在 starter 模块反查。

### Step 4 查官网 changelog 并修复

对 Step 3 归并到的每个「报错库」：

1. **先读本地 `coordinates.md` 该坐标「迁移要点」**——若已写破坏性 minor 核验清单，直接照做。
2. **抓官网 changelog**：用 `changelog_fetch.py` 按坐标预置的官方 release-notes / migration 页面拉取该
   版本区间的变更：
   ```bash
   python .codebuddy/skills/ocn-starter-dependency-upgrade/scripts/changelog_fetch.py \
     --group org.springframework.cloud --artifact spring-cloud-dependencies \
     --from 2025.0.1 --to 2025.0.3
   ```
   脚本优先用 `gh api`（本机有 `gh` 时自动鉴权、速率更高，否则退 urllib）拉 GitHub **compare API**，
   自动筛出该区间里 `removed`/`renamed` 的 `.java` 文件（最强破坏性信号，如某类被删），再附 release
   notes 正文。这样 compile 报错前就能预判"哪个类没了"。若 compare/release 都拿不到，或要核对真实签名，
   加 `--class 全限定类名` 让脚本吐出 `mvn dependency:get` + 解压 + `javap` 的反查命令（jar 不会漏写
   破坏性变更）。抓不到任何结构化数据时回退 WebSearch。
3. **定位到具体报错**：把 compile 报错的方法/类签名与 changelog 的 breaking change 条目对上。
4. **给修复方案并落地**：改对应 `.java`（换 API、补 import、调签名）或 `.xml`/`.yml`（配置项改名）。
   改完**不擅自大面积重构**，只做让编译通过的最小改动，并在对话里说明「为什么这么改、依据哪条 changelog」。
5. **循环**：改完再跑 `mvn compile`，直到无 ERROR。

### Step 5 全链路验证

compile 干净后，跑完整验收：

```bash
mvn -B install -DskipTests        # 装到本地仓库，验证 flattened POM / 发布链路
                                 # 注意：spotless:apply 已绑定 install 的 compile 阶段，
                                 # 会随 install 自动跑，无需（也不应）单独调 spotless:check
```

- `install` 失败（如 flatten 生成 POM 内容异常、GPG/发布配置问题）：按报错修；若纯属本机无发布凭证，
  明确告知用户「install 因发布配置失败，非升级导致的编译问题」，不要假装通过。
- **格式化校验**：spotless 已绑定 `compile` 阶段，上一步 `install` 已自动执行 `spotless:apply`。若想单独
  确认「无新增漂移」，可跑 `mvn spotless:check`（独立 goal 需本机有 spotless 插件前缀，CI 环境才有；
  本地直接用 `install` 结果即可）。发现漂移就 `mvn spotless:apply` 再 `git diff` 确认只动了格式。
- **Sonar 与升级质量解耦**：`-Psonar` 能否跑通取决于本机/CI 是否可达 SonarQube 服务器，**与本次升级的
  编译正确性无关**。若 `-Psonar` 因服务器不可达失败，明确说明「Sonar 跳过，非升级问题」，不阻塞验收；
  以「不带 sonar 的 `install` 通过 + 源码已按 changelog 修复」为有效闭环。

### Step 5.5 运行期最小检查清单（compile 通过 ≠ 升级成功）

本仓库无 `src/test`、无 demo 模块，所以 auto-config 条件、`@ConditionalOnMissingBean`
fallback、SPI 加载、反射调用**全部不在编译验证范围内**。compile 通过只能证明"类型对得上"，
证明不了"运行不炸"。**必须**针对本次升级坐标生成一份可勾选的运行期检查清单，写到
`.tmp_upgrade_runtime_checklist.md`，每条带：受影响模块、要核对的 auto-config 类、对应 changelog 链接。
尤其对以下信号重点列项：
- `coordinate_renames` 涉及的坐标：新 BOM/新 starter 是否真的把 bean 注册进 classpath（如 nextdoc4j
  改名后 springdoc-openapi 是否到位）。
- `precheck.json` 的 `removed_symbols`：被删类的调用方是否全部改完，运行期有无 `NoClassDefFoundError`。
- 大版本（🔴）/ Spring Boot 代际升级：auto-config property 改名、废弃 endpoint 移除。

若环境可达消费方（continew-admin）的测试，应**强制**在其上跑一次上下文启动测试；否则在结论里明确标注
「未做运行期验证，存在编译期不可见风险」。

### Step 6 对话里给结论

一段总结：本次升了哪几个库、几个触发了源码修复、各改了什么文件、compile/install/spotless/sonar 各自结果、
**以及「建议人工运行期验证」清单**（auto-config 条件、SPI、运行期行为本技能无法覆盖的点）。
若 Step 1.5 预检发现坐标改名或符号删除，必须在结论里显式说明「已处理 / 未处理」，**不得隐含"编译过=安全"**。

## 运行前提

- **Python 3 标准库**，零第三方依赖（urllib/xml.etree/re/json/subprocess 调 git）。
- **联网**：Step 4 需访问各库官网（GitHub Releases / 官网 migration 页）；离线时 `changelog_fetch.py` 标注
  「离线，回退 WebSearch」并提示手动查。
- **git + mvn + JDK 17**：本机需 `mvn`（无 wrapper），JDK 17 与 CI 一致。
- **Windows**：命令用 `python`（不是 `python3`）。

## 维护提示

- **新增依赖坐标**：在 analyze 技能的 `coordinates.md` 加小节即可，本技能自动复用；若某库有稳定的官方
  changelog 入口，在 `changelog_fetch.py` 的 `CHANGELOG_SOURCES` 补映射，让 Step 4 自动抓。
- **三目录镜像**：按 `CLAUDE.md` / `AGENTS.md` 的「Skill mirroring」约定，改完 `.codebuddy/skills/`（源）后
  把本技能文件同步到 `.agents/skills/`、`.claude/skills/` 并保持字节一致。
- **ADR**：架构决策在仓库根 `docs/adr/`（analyze 技能已立 0001 数据源、0002/0003 写模式、0004 verify 拆分），
  本技能对应 0004 的"upgrade"侧，仅引用不另立。
