# 特殊情况（Special Cases）

分级算法（见 [tiers.md](tiers.md)）处理不了的联动与陷阱，全在这。`scan.py` 对平台级坐标会查本文规则做覆盖。

## 1. Spring Boot ↔ Spring Cloud 平台级联动

最强的耦合。Spring Cloud 每个 release train（如 `2025.0.x`）只兼容特定 Spring Boot 版本区间；错配在运行期
炸（auto-config 条件不匹配），compile 却可能过，不能靠 compile 兜底。

**报告行为**：
- Boot 有可用升级时，执行建议必须先列「配套 Cloud release train」。
- 用户只升 Boot 不升 Cloud 时，`server.py` 的 apply 要拦下并提示「升 Spring Boot 需同时确认
  spring-cloud.version 兼容」。**这是 🔴 级强制联动，即便 Boot 是 🟢 补丁也拦截**——Cloud train 与 Boot patch
  的兼容边界由 Cloud 项目声明，跨 patch 偶有不兼容。
- **自动校验**：`scan.py` 内嵌 `SPRING_CLOUD_COMPAT` 矩阵（Boot 代 → Cloud train，如 3.5.x→2025.0.x Northfields、
  4.x→2025.1.x Oakwood），扫描时自动做 Boot→Cloud 映射与交叉校验：Boot 目标对应 train 不匹配当前
  `spring-cloud.version` 时，把 Cloud 提档为 🟡、加 `⚠ Boot 不匹配` 徽章。矩阵权威来源为官网
  https://spring.io/projects/spring-cloud#overview ，遇到未覆盖的 Boot 代**不要臆测**，查官网补一行。

**例外**：Cloud 已能兼容目标 Boot 且仅升 Boot patch，可放行，但执行建议要写明「已确认 Cloud train X 兼容 Boot Y.Z」。

## 2. 互斥变体（mutually exclusive implementations）

三组坐标发布相同 FQCN/相同 auto-config role，不能同 classpath：

| 互斥组 | 涉及 property |
|---|---|
| `data-mp` vs `data-mf` | `mybatis-plus.version` / `mybatis-flex.version`（CRUD 实现也分两套） |
| `excel-fastexcel` vs `excel-poi` | `fastexcel.version` / `poi.version`（都发布 `ExcelUtils`，CRUD core 显式依赖 FastExcel） |
| `log-aop` vs `log-interceptor` | （无独立 property，依赖 log 实现，共享 auto-config/bean role） |

**报告行为**：执行建议里若两组都有可用升级，提示「二者择一升级，勿同时」，不强制。

## 3. BOM vs 实际构件

很多 property 在 `<dependencyManagement>` 里指向 **BOM 构件**（`xxx-bom`/`xxx-dependencies`/字面量 `bom`）而非
运行时构件。检测时抓 BOM 的 metadata 是对的（BOM 版本即系列版本号）。要点：

- **sa-token**：property 指 `sa-token-bom`，运行时构件是 `sa-token-spring-boot3-starter`。勿抓父 `sa-token`
  聚合 POM（latest 停在 1.6.0 是历史遗留）。
- **aws-sdk**：artifactId 字面量是 `bom`，路径 `software/amazon/awssdk/bom/`，不是 `aws-sdk-bom`。
- **spring-boot**：指 `spring-boot-dependencies`（BOM），不是 `spring-boot-starter-parent`（parent POM，
  version 列表含 M/RC 易混）。
- **hutool**：`hutool-bom` 与 `hutool-all` 同 property，抓 BOM 即可。

## 4. artifactId 大小写与点号

metadata 路径里 artifactId **大小写敏感、点号原样**：`me.zhyd.oauth:JustAuth` → `me/zhyd/oauth/JustAuth/`
（`J` 大写）；`org.eclipse.paho:org.eclipse.paho.client.mqttv3` 路径里点号保留。`scan.py` 直接用 artifactId 拼
路径，不做归一——COORDINATES 里的 artifactId 必须与 Central 完全一致。

## 5. 带后缀的版本号（lineage，发布支线）

部分坐标版本号带语义后缀，须区分是「预发布 qualifier」还是「发布支线（lineage）」：

- **lineage（本身是 GA）**：`graceful-response` `5.0.5-boot3` 的 `-boot3` 是 Boot 3 支线标记；Guava 的 `-jre`/
  `-android` 同理。
- **预发布（不能当目标）**：`-M1`/`-RC1`/`-Beta`/`-SNAPSHOT`。

`scan.py` 用 `PRERELEASE_RE` 白名单识别预发布，其余 `-<word>` 后缀按 lineage 处理，**只在同 lineage 内比较版本**
（`lineage_of()`）。若锁定版本所在 lineage 无任何 GA，才回退「全体 GA 最高版」→ `<release>`。四段版本号按四段比
（`sonar-maven-plugin` `5.2.0.4988`，截断成三段会让所有 `5.2.0.x` 相等而检测不出升级）。

## 6. 构建插件（flatten / spotless / sonar）

插件与普通构件**一样检测、一样分级、一样可用「应用升级」按钮**，property 在同一个 `<properties>` 块里。升级
副作用已写进各自 `constraints`：

- **spotless** 绑定 `compile` 会改写源码 → 升级后跑 `mvn spotless:check` 确认格式没漂移。
- **flatten** 生成发布用 POM → 升级后确认 flattened POM 内容不变。
- **sonar** 仅 `-Psonar` 使用，不影响主构建。

## 7. 由 spring-boot-dependencies 传递管理的坐标

`snakeyaml`、`swagger`（部分）等既被本 POM 显式锁，又被 `spring-boot-dependencies` BOM 管。升级时与 Boot 期望
冲突会按最近声明原则取本 POM 值，但可能引发 Boot 行为异常。报告执行建议提示：「snakeyaml/swagger 显式锁版本与
Boot 期望可能冲突，升 Boot 时一并核对」。
