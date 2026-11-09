# AGENTS.md

本文件为在本代码库中工作的 AI 编程智能体（DeepSeek Harness、Claude Code、Codex、Cursor 等）提供指引。面向人类贡献者的说明请查阅 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## AI 贡献准则

- **不得以 AI 身份在 Issue 或 PR 上发表评论**。讨论区只属于人类。
- **先讨论再实现**：非平凡改动（如新功能、重构）开工前，先在 Issue 评论中与维护者就实现方向达成一致。
- **依赖版本只改根父 POM**：修改依赖版本只能在 `continew-starter-dependencies/pom.xml` 的版本属性中进行，禁止在各模块 pom.xml 中硬编码版本号。
- **内部模块依赖统一写 `${project.groupId}`**：模块 POM 与 BOM 中的内部依赖 groupId 一律使用 `${project.groupId}`；版本由 BOM（`${revision}`）统一供给，模块 POM 免写 version；`<parent>` 与项目自身坐标保持字面量。
- **新增模块需在三处注册**：项目 BOM（`continew-starter-bom/pom.xml`）、聚合 POM 的 `<modules>`（`pom.xml`）；若模块引入配置属性，还须在 `PropertiesConstants` 中定义前缀常量。
- **仅测试用途模块的注册豁免**：artifactId 以 `-test-support` 或 `-tests` 结尾的模块，只在聚合 POM 注册、不进 BOM，必须设置 `maven.deploy.skip=true` 并加入 release profile `central-publishing-maven-plugin` 的 `excludeArtifacts`，发布前比对构件清单确认未误发布。
- **自动配置类必须注册到 `AutoConfiguration.imports`**：未注册的配置类不会被加载（参见[配置与自动配置规范](#配置与自动配置规范)）。
- **交付前自动自检**：AI 生成或修改 Java 源码或影响装配行为的资源（`AutoConfiguration.imports`、`default-*.yml`、模块 pom）后，必须调用 `open-code-review-delegate` 自检（delegate 模式无网关依赖），发现 HIGH 及以上问题先修复再交付，未标注 severity 时按 category 为 bug/security 视同 HIGH。
- **披露 AI 使用**：当提交中较大部分由 AI 生成时，请在 commit message 末尾追加 trailer，注明实际使用的智能体，例如：
  
  ```
  Assisted-by: DeepSeek Harness
  ```
- 贡献流程一律**遵循 [CONTRIBUTING.md](./CONTRIBUTING.md)**。

## 项目概述

ContiNew Starter（Continue New Starter）是基于 Spring Boot 3.x 的企业级 Starter 库。它遵循"约定优于配置"理念，将一系列经过企业实践验证的第三方库（MyBatis-Plus、Sa-Token、Redisson、Caffeine 等）封装为开箱即用的 Starter，供 Spring Boot Web 应用集成。**这不是一个应用项目**，而是发布到 Maven Central 的多模块 Starter 库，groupId 为 `top.continew.starter`，全部模块版本通过 `${revision}` 属性统一管理。

**当前版本**：3.1.0-SNAPSHOT | **主分支**：`dev` | **构建 JDK**：21+（Enforcer `requireJavaVersion [21,)`，推荐 25） | **产物字节码**：Java 21（`java.version=21` 既是编译目标也是使用方运行时门槛，不表示用 JDK 21 构建；依赖字节码不得高于 21，由 `enforceBytecodeVersion` 拦截） | **构建**：Maven 3.9.16（`./mvnw`，Windows 为 `mvnw.cmd`）

## 核心架构

关键模块及其职责：

- **continew-starter-dependencies**：第三方依赖版本（约 40 个坐标）的唯一数据源
- **continew-starter-bom**：管理全部内部模块版本
- **continew-starter**：聚合 POM，继承 `continew-starter-dependencies`，自身不含业务代码
- **core**：常量、`PropertiesConstants`、通用工具（线程池等自动配置）
- **json**：Jackson 封装
- **api-doc**：Spring Doc + NextDoc4j
- **validation**：Hibernate Validator
- **web**：跨域、全局异常与响应、链路追踪等自动配置
- **cache**：redisson / multicache（Caffeine L1 + Redis L2）/ springcache
- **auth**：satoken / justauth / refresh-token（轮换、重放保护、统一撤销、WebSocket 联动）
- **data**：core + mp（MyBatis-Plus）/ mf（MyBatis-Flex）
- **encrypt**：core / field（字段加密）/ api（API 加密）/ password-encoder
- **security**：mask（脱敏）/ xss / sensitivewords
- **ratelimiter**：限流
- **idempotent**：幂等
- **trace**：链路追踪
- **captcha**：graphic（静态）/ behavior（动态）
- **messaging**：mail / websocket / mqtt
- **log**：core / aop / interceptor
- **excel**：core / fastexcel / poi
- **storage**：本地存储与 S3 对象存储（兼容主流云厂商）
- **license**：core / generator / verifier
- **extension**：crud / datapermission / tenant，各含 `-core` 与 `-mp`/`-mf`

聚合模块的组织约定：`-core` 子模块定义接口与通用逻辑，`-mp`/`-mf` 提供特定 ORM 实现，使用方按需选择。

自动配置：每个 Starter 模块通过 Spring Boot 3 的 `AutoConfiguration.imports` 机制注册（**不是**旧版 `spring.factories`）；详见[配置与自动配置规范](#配置与自动配置规范)。

## 构建与测试命令

```bash
# 完整构建（validate 阶段 Enforcer -> Spotless -> Checkstyle，编译 + 单元测试，verify 阶段 SpotBugs）
./mvnw verify

# 仅编译（含 validate 阶段三道门禁，不含 SpotBugs），仅用于快速迭代，不可作为提交前自检
./mvnw compile

# 编译单个模块（含依赖模块）
./mvnw -pl :continew-starter-web -am compile

# 自动修复格式（被 Spotless 门禁拦截时使用：格式化 + 清理无用 import + 补 License Header）
./mvnw compile -Pformat

# 安装全部模块到本地 Maven 仓库
./mvnw install -DskipTests

# 清理所有 target 目录及 flatten 生成的 .flattened-pom.xml
./mvnw clean

# 运行基础设施集成测试（*IT 用例，Testcontainers 真实 Redis/MySQL；本机无 Docker 时由 @EnabledIfDockerAvailable 自动跳过）
./mvnw -B verify -Pintegration

# 生成本地测试覆盖率报告（target/site/jacoco），与 sonar profile 并存
./mvnw -B verify -Pcoverage
```

单元测试随模块维护（如 `continew-starter-auth-refresh-token` 内置 78 个用例，无外部依赖，本地与 CI 自动执行）。代码改动的验证方式是执行 `./mvnw verify` 确保四道静态门禁与全部单元测试通过。

版本管理：`${revision}` 属性定义在 `continew-starter-dependencies` 与 `continew-starter-bom` 中，`flatten-maven-plugin` 在 `process-resources` 阶段将其解析为实际版本并生成用于发布的简化 `.flattened-pom.xml`，修改版本只需改一处。

### 提交前门禁（必须通过）

提交 Java 代码或影响装配行为的资源前，AI 智能体**必须**让门禁通过：

1. 执行 `./mvnw verify`。门禁依次为：validate 阶段的 **Enforcer**（构建环境与依赖合规）、**Spotless check**（代码格式）、**Checkstyle**（代码规范），编译后 test 阶段的**单元测试**，以及 verify 阶段的 **SpotBugs**（字节码缺陷），任一不通过都会直接构建失败。
2. 若被 Spotless 拦截，执行 `./mvnw compile -Pformat` 自动修复，然后再执行一次 `./mvnw verify` 确认通过。
3. AI 生成/修改 Java 或装配资源后先跑 `open-code-review-delegate` 自检（见 AI 贡献准则）。
4. 全部门禁与单元测试通过后才能提交。

> **非 Java 改动**：
> - 仅改文档或运行时配置，无需执行 `./mvnw verify`。
> - 改 `docs/reference/properties.mdx` 或 `@ConfigurationProperties` 类，先执行 `./mvnw compile -DskipTests` 生成 metadata，再执行 `python scripts/check-doc-properties.py`。
> - CI 对文档改动同样执行完整构建与漂移检查。

构建过程**不会修改任何源码文件**；`-Pformat` 是唯一会修改源码的 profile。不要用 IDE 格式化或 `git diff --check` 替代 Spotless 门禁。IDE 格式化引擎是另一套实现，可能放行项目格式化器拒绝的代码。

Sonar 分析使用 `./mvnw verify -Psonar`（激活 JaCoCo），日常 `./mvnw verify` 不激活。

## 代码风格

遵循**阿里巴巴《Java 开发手册(黄山版)》**（P3C）。

- Eclipse Formatter 配置（唯一事实源，Spotless 使用）：[`style/ocn-eclipse-formatter.xml`](style/ocn-eclipse-formatter.xml)
- IDEA 代码风格（近似映射）：[`style/ocn-idea-code-style.xml`](style/ocn-idea-code-style.xml)
- Checkstyle 规则：[`style/ocn-checkstyle.xml`](style/ocn-checkstyle.xml)
- 风格说明与 IDE 配置指引：[`style/STYLE.md`](style/STYLE.md)

### AI 智能体关键规则

| 规则 | 值 |
|------|-----|
| 缩进 | **4 空格**（禁用 Tab），续行缩进 4 空格 |
| 行宽 | 最多 **100 字符**（由 Spotless 的 Eclipse 格式化器 `lineSplit=100` 强制；Checkstyle `LineLength` 设为 150 仅作兜底） |
| 星号导入 | **禁止**（`AvoidStarImport`） |
| 无用 import | **禁止**（`-Pformat` 自动清理） |
| import 顺序 | 静态导入置顶，第三方组居中，JDK 平台组（java/javax/jakarta）收尾；组内按字典序（不含尾部分隔符比较，短前缀在前）、组间空行分隔（`CustomImportOrder` 强制，Spotless 不做排序） |
| 大括号 | `if/else/for/while/do-while` 必须加大括号（`NeedBraces`） |
| 空行 | 连续空行最多保留 1 行（`EmptyLineSeparator`） |
| 类注释 | 必须包含 `@author` 与 `@since`；嵌套类型不强制。`@author` 由 `JavadocType(authorFormat=\S)` 强制，`@since` 为约定 |
| @since 取值 | 取当前形态首次发布的版本号（如 `3.0.0`），禁止日期；发布后新增的 public/protected 方法补方法级 `@since`，存量不补、private 不标；迁移或重加按"宁新勿旧"取最新版本 |
| @author 署名 | 类级必填，创建者在前、实质贡献者按顺序追加；方法级禁止（第三方来源署名除外）；AI 辅助的类署使用者本人，AI 信息只进 commit trailer |
| Javadoc | 禁止单行（`RegexpSingleline` 强制）；方法注释写全 `@param`（含类型参数）与 `@return`，接口方法必须写全 |
| 内联全限定名 | **禁止**（`InlineFullyQualifiedName`：与依赖库同名时应调整类名，而非内联全限定名绕过） |
| 格式化豁免 | `// @formatter:off` 与 `// @formatter:on` 之间的代码不参与格式化 |
| 命名 | `style/` 配置文件与 agent 技能统一使用 `ocn-` 前缀（OCN = OpenContiNew），第三方技能除外 |

### 注释风格

注释只写当前代码的非显然约束。归属映射：修改历史归 commit message，修复原因与旧实现缺陷归 commit 正文或 PR 正文，两者都不进代码注释。

以下违规注释出自同生态 ContiNew Admin 的整改记录（PR #251、#255 台账，可核实原文）：

| 违规注释 | 违规点 | 处置 |
|:--|:--|:--|
| `// 先记录仍持有超套餐菜单的受影响角色，删除后再查恒为空集，在线刷新会失效` | 时态拼贴，讲的是已修掉的旧缺陷，读者误以为当前仍有问题 | 改为面向当前约束的一句（如「受影响集合须在关联删除前查询」），或直接删除（顺序已由单测锁定） |
| ` * 回归：菜单权限置空撤销时没有新关联可插入，……` | 「回归：」前缀是变更叙事，正文其余是有效契约说明 | 删前缀，保留契约说明 |
| `（用例设计稿的 3 秒会被入口校验挡掉，属测试侧设计修正，非产品缺陷）` | 设计稿修订叙事，读者看不到设计稿 | 整句删除；阈值与静置窗口的约束说明保留 |
| 通用型：`// 输出调试日志` 置于 `log.debug(...)` 上方 | 复述自明代码 | 删除 |

判定手法：写注释时不看 diff，只看当前文件；把注释单独抽出对着最新代码读，读起来像一段修复说明或旁白，即不合格。

- 块注释平实概括一段动作，单行注释解释非显然约束；用代码里已有的词，不另造需要反推的概括。
- 词表扫描注意假阳性：业务动词「新增与清理共用 compute 串行化」一类（`ConcurrentWebSocketSessionDao`）中的「新增」是操作名而非变更痕迹，属合法注释。
- 不为自解释代码逐行加注释，密度与周围代码一致；公共 API 用 Javadoc 写契约，实现内部只解释非显然决策。

### License Header

每个新增 Java 源文件**必须**包含 LGPL-3.0 License Header，模板见 [`style/license-header`](style/license-header)。Spotless 在 validate 阶段校验，`./mvnw compile -Pformat` 自动补全，无需手工复制。

## 配置与自动配置规范

ContiNew Starter 各模块遵循严格约定。AI 智能体生成 Starter 代码时**必须**遵守以下规范。

### 配置属性前缀

所有配置属性统一使用 `continew-starter` 前缀，格式为 `continew-starter.<module>[.<sub-feature>]`。前缀常量**必须**定义在 [`PropertiesConstants`](continew-starter-core/src/main/java/top/continew/starter/core/constant/PropertiesConstants.java) 中（基于 `CONTINEW_STARTER` 基础常量拼接），并在 `@ConfigurationProperties` 与 `@ConditionalOnProperty` 中引用，禁止内联字符串字面量。

```java
public static final String CONTINEW_STARTER = "continew-starter";
public static final String WEB = CONTINEW_STARTER + StringConstants.DOT + "web";
public static final String WEB_CORS = WEB + StringConstants.DOT + "cors";               // continew-starter.web.cors
public static final String ENCRYPT_FIELD = ENCRYPT + StringConstants.DOT + "field";     // continew-starter.encrypt.field
```

### 自动配置注册

每个自动配置类**必须**注册到所属模块的 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中（每行一个全限定类名），否则不会被加载。

### 包结构

每个模块的 Java 包基础路径为 `top.continew.starter.<module>`，内部按职责分包：

| 包 | 用途 |
|---------|---------|
| `autoconfigure/` | 自动配置类 |
| `autoconfigure/<feature>/` | 特定功能的配置类与 Properties |
| `constant/`、`enums/`、`exception/` | 常量、枚举、异常 |
| `util/` | 工具类 |
| `annotation/` | 自定义注解 |
| `aop/` | 切面实现 |
| `model/` | 数据模型（DTO、VO、实体等） |

### 示例

```java
@AutoConfiguration
@ConditionalOnProperty(prefix = PropertiesConstants.XXX, name = PropertiesConstants.ENABLED, havingValue = "true")
@EnableConfigurationProperties(XxxProperties.class)
public class XxxAutoConfiguration {

    @Bean
    public XxxService xxxService(XxxProperties properties) {
        // business logic ...
    }
}
```

## PR 约定

所有 PR 必须提交到 `dev` 分支（新功能与功能优化）；维护分支 `x.x.x` 仅接受 bug 修复。请基于目标分支创建特性分支（如 `feat/new-feature`），不要直接修改源分支。遵循 [PR 模板](.github/PULL_REQUEST_TEMPLATE.md)。

**提交格式**：[Conventional Commits（约定式提交）1.0.0](https://www.conventionalcommits.org/zh-hans/v1.0.0/)规范，`<类型>[可选作用域]: <描述>`，破坏性变更在类型或作用域后追加 `!`，如 `feat(cache): 新增 xxx`、`feat!:`。

**PR 正文口吻**：以变更者第一人称视角直接陈述改了什么、为什么改，读者是几个月后另一台机器前的人。不写成对当前审查者的会话回复（「如您所问」「现已修复完成」「请查看」），不带汇报腔（「本 PR 完成了以下工作」）。修复原因与修复前后行为对比写在这里（「解决方案」段），不写进代码注释。

**提交前检查**：

```bash
./mvnw verify     # 四道静态门禁与单元测试必须全部通过（被 Spotless 拦截时使用 -Pformat）
```

## 安全漏洞

不得通过 GitHub Issue 报告安全漏洞。请使用 GitHub 私有漏洞报告，详见 [SECURITY.md](./SECURITY.md)。

## 文档（docs/）

本仓库 `docs/` 目录是 ContiNew Starter 官方文档的**唯一权威源**，经 [continew.top](https://github.com/continew-org/continew.top) 官网仓库构建时汇聚渲染为 `/docs/starter/*`。AI 智能体被指派编写或更新文档时在此目录工作，**开工前必须先读取 [`ocn-doc-writing/SKILL.md`](https://github.com/continew-org/continew.top/blob/main/.agents/skills/ocn-doc-writing/SKILL.md)**（目录结构、frontmatter、链接策略、写作风格禁令的事实源全本），本文件只保留三条硬约束：

- 修改模块代码（尤其配置项、行为）必须在**同一 PR** 中同步更新对应文档，并执行 `python scripts/check-doc-properties.py` 与 `@ConfigurationProperties` 对账（见提交前门禁）；纯文档改动无需跑 `./mvnw verify`。
- 类名、配置键、默认值、异常串、版本号必须以 **starter 源码的具体值**为准，禁止凭印象；示例场景去业务化，不贴 admin 业务数据；配置项归 starter，配置值归 admin。
- 多实现模块（cache、auth、data、encrypt 等）按「目录 + 子实现各一篇 + 模块 index 入口」组织；各代码模块的 `README.md` 只放「是什么 + 坐标 + 一句话用法 + 完整文档链接」，不复述完整文档。

## Agent Skills

各 agent 工具（DeepSeek Harness / Claude Code / Codex）共用的技能统一存放在 `.agents/skills/` 作为唯一事实源，每个技能一个目录、含 `SKILL.md`。新增技能沿用 `ocn-` 命名前缀，第三方技能保留官方名、不加该前缀。

评审相关技能按场景选用，`open-code-review` 用于 CI 自动评审（与 `ocr-review.yml` 同源）与维护者显式深度评审（需网关 key）；`open-code-review-delegate` 用于 AI 交付前自动自检与贡献者可选自检（无网关依赖，[CONTRIBUTING.md](./CONTRIBUTING.md) 与 [PR 模板](.github/PULL_REQUEST_TEMPLATE.md) 中「本地 AI 自检」所指）。
