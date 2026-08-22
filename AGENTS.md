# AGENTS.md

本文件为在本代码库中工作的 AI 智能体提供指引。

> **注意**：`CLAUDE.md` 是指向本文件的符号链接，`.claude/skills` 是指向 `.agents/skills` 的符号链接。
> 请直接编辑本文件和 `.agents/skills/`，不要改动链接本身。

## 项目概述

ContiNew Starter（Continue New Starter）是一个基于 Spring Boot 3.x 的企业级 Starter 库项目（Java 17，LGPL-3.0），封装了 MyBatis-Plus、Sa-Token、Redisson、JetCache 等经过企业实践验证的第三方库，遵循"约定优于配置"理念，为 Spring Boot Web 应用提供完整的自动配置解决方案。

这不是一个应用项目，而是一个发布到 Maven Central 的多模块 Starter 库。groupId 为 `top.continew.starter`，版本通过 `${revision}` 统一管理。

## Repository layout

```
continew-starter/                    聚合 POM：继承 continew-starter-dependencies，无业务代码
├── continew-starter-dependencies/   根父 POM：全部第三方依赖版本（~40 坐标）的唯一数据源
├── continew-starter-bom/            项目 BOM：内部模块坐标与 ${revision} 管理
│
├── 平铺模块（单模块直接提供一种能力）
│   ├── continew-starter-core/       常量、PropertiesConstants、通用工具
│   ├── continew-starter-web/        Web 场景自动配置（CORS 等）
│   ├── continew-starter-ratelimiter/ 限流
│   ├── continew-starter-trace/      链路追踪
│   ├── continew-starter-validation/ 参数校验
│   ├── continew-starter-storage/    对象存储
│   ├── continew-starter-idempotent/ 幂等
│   └── continew-starter-api-doc/    API 文档
│
└── 父子聚合模块（核心 + 实现变体，-core 定义接口与通用逻辑，-mp/-mf 提供特定 ORM 实现）
    ├── continew-starter-cache/     → cache-redisson, cache-jetcache, cache-springcache
    ├── continew-starter-data/      → data-core, data-mp（MyBatis Plus）, data-mf（MyBatis Flex）
    ├── continew-starter-extension/ → extension-crud, extension-datapermission, extension-tenant（各含 -core 与 -mp/-mf）
    ├── continew-starter-auth/      → auth-satoken, auth-justauth
    ├── continew-starter-captcha/   → captcha-behavior, captcha-graphic
    ├── continew-starter-excel/     → excel-core, excel-fastexcel, excel-poi
    ├── continew-starter-json/      → json-jackson
    ├── continew-starter-license/   → license-core, license-generator, license-verifier
    ├── continew-starter-log/       → log-core, log-aop, log-interceptor
    ├── continew-starter-messaging/ → messaging-mail, messaging-websocket, messaging-mqtt
    ├── continew-starter-security/  → security-mask, security-sensitivewords, security-xss
    └── continew-starter-encrypt/   → encrypt-core, encrypt-api, encrypt-field, encrypt-password-encoder
```

## Commands

### 编译与格式化（最重要）

```bash
# 编译整个项目（编译时自动执行 Spotless 代码格式化）
mvn compile

# 编译单个模块（含依赖模块）
mvn -pl :continew-starter-web -am compile

# 跳过 Spotless 格式化（仅在需要快速验证时使用）
mvn compile -Dspotless.apply.skip=true
```

**关键约定**：提交代码前必须执行 `mvn compile`，编译会自动触发 Spotless 插件按照 `.style/p3c-codestyle.xml`（阿里 P3C 黄山版规范）格式化代码并添加 License Header。编译通过后不要再次在 IDE 中打开代码文件，避免不同 IDE 配置导致格式差异。

### 其它命令

```bash
mvn install -DskipTests        # 安装全部模块到本地 Maven 仓库
mvn -pl :continew-starter-web -am install -DskipTests   # 安装单个模块（含依赖模块）
mvn clean                      # 清理所有 target 目录及 flatten 生成文件
mvn deploy -Prelease           # 发布到 Maven Central（仅维护者，需 GPG 签名和 Central 账号）
mvn verify -Psonar             # SonarCloud 代码质量分析
```

### 测试

本项目目前不包含单元测试模块。验证改动正确性的方式是执行 `mvn compile` 确保编译通过。

## Architecture

### 三层 POM 版本管理体系

项目采用 `flatten-maven-plugin` + `${revision}` 的统一版本管理模式，这是理解整个项目的关键：

1. **`continew-starter-dependencies`**（根父 POM）：管理所有第三方依赖版本（Spring Boot、MyBatis-Plus、Sa-Token 等 ~40 个坐标）。它通过 `dependencyManagement` 导入各组件 BOM，并声明版本属性。它是版本锁定的唯一数据源。
2. **`continew-starter-bom`**：管理项目内部各模块的版本，列出所有 `continew-starter-*` 模块的坐标与 `${revision}`。应用方导入此 BOM 即可使用本项目模块。
3. **`continew-starter`**（聚合 POM）：聚合所有业务模块的构建，自身不含业务代码，继承自 `continew-starter-dependencies`。

所有模块版本统一使用 `${revision}` 属性（定义在 `continew-starter-dependencies` 和 `continew-starter-bom` 的 `<properties>` 中），修改版本只需改一处。`flatten-maven-plugin` 在 `process-resources` 阶段将 `${revision}` 解析为实际版本并生成简化的 `.flattened-pom.xml` 用于发布。

### 模块组织模式

约 20 个顶层模块，遵循两种组织模式：

**平铺模块**（如 `continew-starter-core`、`continew-starter-web`、`continew-starter-ratelimiter`）：单一模块直接提供一种能力。

**父子聚合模块**（大多数）：按"核心 + 实现变体"组织。例如：
- `continew-starter-cache/` → `cache-redisson`、`cache-jetcache`、`cache-springcache`
- `continew-starter-data/` → `data-core`、`data-mp`（MyBatis Plus）、`data-mf`（MyBatis Flex）
- `continew-starter-extension/` → `extension-crud`、`extension-datapermission`、`extension-tenant`，每个再分 `-core` 和 `-mp`/`-mf`

`-core` 子模块定义接口、模型、通用逻辑；`-mp`/`-mf` 子模块提供特定 ORM 的实现。这种设计允许使用方按需选择 ORM。

### 自动配置机制

每个 Starter 模块通过 Spring Boot 3 的 `AutoConfiguration.imports` 机制注册自动配置类（**不是**旧版 `spring.factories`）：

- 文件路径：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 每行一个全限定类名，使用 `@AutoConfiguration` 注解标注
- 配置类通常带 `@Lazy`、`@ConditionalOnProperty`（按 `enabled` 属性控制开关）、`@ConditionalOnWebApplication` 等条件注解
- 使用 `@EnableConfigurationProperties` 绑定配置属性

### 配置属性前缀约定

所有配置属性统一使用 `continew-starter` 前缀，前缀常量集中在 `continew-starter-core` 的 `PropertiesConstants` 类中（如 `WEB_CORS = "continew-starter.web.cors"`）。新增模块的配置前缀必须在此类中定义，并在 `@ConfigurationProperties` 和 `@ConditionalOnProperty` 中引用，保持一致性。配置前缀格式为 `continew-starter.<module>[.<sub-feature>]`。

### 代码包结构约定

每个模块的 Java 包基础路径为 `top.continew.starter.<module>`，内部按职责分包：
- `autoconfigure/` — 自动配置类
- `autoconfigure/<feature>/` — 特定功能的配置类与 Properties
- `constant/`、`enums/`、`exception/` — 常量、枚举、异常
- `util/` — 工具类
- `annotation/` — 自定义注解
- `aop/` — 切面实现
- `model/` — 数据模型（DTO、VO、实体等）

### License Header 强制要求

所有 Java 文件必须包含 LGPL-3.0 License Header（定义在 `.style/license-header`）。Spotless 插件在编译时会自动检查并补全。新建 Java 文件时请从现有文件复制 header，或直接执行 `mvn compile` 让插件自动添加。

### 代码风格规范

- 遵循阿里《Java开发手册(黄山版)》（`.style/Java开发手册(黄山版).pdf`）
- 代码格式由 `.style/p3c-codestyle.xml`（Eclipse formatter 格式）定义
- 类注释需包含 `@author` 和 `@since` 标签
- 提交信息遵循 [Angular 提交规范](https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-angular)

### 分支策略

- `dev`：开发分支，接受新功能或优化 PR，对应 SNAPSHOT 版本
- `x.x.x`：维护分支，仅接受 bug 修复，不接受新功能
- 提交 PR 前需基于正确分支创建特性分支（如 `feat/newFeature`），不直接修改源分支

## Key Files Reference

| 文件/目录 | 用途 |
|:--|:--|
| `continew-starter-dependencies/pom.xml` | 第三方依赖版本管理（修改依赖版本的唯一入口） |
| `continew-starter-bom/pom.xml` | 项目内部模块版本管理 |
| `continew-starter-core/.../PropertiesConstants.java` | 所有配置属性前缀常量 |
| `.style/p3c-codestyle.xml` | 代码格式化规则（Spotless 使用） |
| `.style/license-header` | License Header 模板 |
| `docs/adr/` | 架构决策记录 |
| `docs/agents/` | Agent 相关的领域文档与 issue tracker 约定 |

## Conventions for Agents

1. **修改依赖版本**：只在 `continew-starter-dependencies/pom.xml` 的 `<properties>` 中修改，不要在各模块的 pom.xml 中硬编码版本号。
2. **新增模块**：需要在 `continew-starter-bom/pom.xml` 注册版本、在 `continew-starter/pom.xml` 的 `<modules>` 中添加聚合、在 `PropertiesConstants` 中定义配置前缀。
3. **新增自动配置类**：必须注册到对应模块的 `AutoConfiguration.imports` 文件中，否则不会被加载。
4. **代码格式化**：不要手动调整代码格式，交给 `mvn compile` 的 Spotless 插件处理。IDE 的格式化设置可能与项目规范冲突。
5. **编译验证**：任何代码改动后，执行 `mvn compile` 或 `mvn -pl :<module> -am compile` 验证编译通过。
6. **`target/` 目录**：构建产物目录，不应提交到 Git，修改源码时忽略其中的 `.class` 文件。

## Agent skills

技能是各 agent 工具（Claude Code / Codex / dsh）共用的**唯一事实源**，统一存放在 `.agents/skills/`，每个技能一个目录、含 `SKILL.md`。

## 编辑这些说明

`CLAUDE.md` 是根目录 `AGENTS.md` 的符号链接，`.claude/skills` 是 `.agents/skills` 的符号链接——请编辑真实文件（本文件与 `.agents/skills/`），不要改动链接本身。保持每条规则自包含，必要时链接到更高层文档。

> **Windows 注意**：克隆后如需符号链接生效，需开启开发者模式（或以管理员运行 git），并执行 `git config core.symlinks true`，否则链接会被检出为普通文本文件。团队若以 Windows 为主且符号链接不可靠，可改用脚本同步（`cp AGENTS.md CLAUDE.md && cp -r .agents/skills/* .claude/skills/`）。
