# AGENTS.md

本文件为在本代码库中工作的 AI 编程智能体（DeepSeek Harness、Claude Code、Codex、Cursor 等）提供指引。面向人类贡献者的说明请查阅 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## AI 贡献准则

- **不得以 AI 身份在 Issue 或 PR 上发表评论**。讨论区只属于人类。
- **先讨论再实现**：非平凡改动（如新功能、重构）开工前，先在 Issue 评论中与维护者就实现方向达成一致。
- **依赖版本只改根父 POM**：修改依赖版本只能在 `continew-starter-dependencies/pom.xml` 的版本属性中进行，禁止在各模块 pom.xml 中硬编码版本号。
- **内部模块依赖统一写 `${project.groupId}`**：模块 POM 与 BOM 中的内部依赖 groupId 一律使用 `${project.groupId}`；版本由 BOM（`${revision}`）统一供给，模块 POM 免写 version；`<parent>` 与项目自身坐标保持字面量。
- **新增模块需在三处注册**：项目 BOM（`continew-starter-bom/pom.xml`）、聚合 POM 的 `<modules>`（`pom.xml`）；若模块引入配置属性，还须在 `PropertiesConstants` 中定义前缀常量。
- **自动配置类必须注册到 `AutoConfiguration.imports`**：未注册的配置类不会被加载（参见[配置与自动配置规范](#配置与自动配置规范)）。
- **披露 AI 使用**：当提交中较大部分由 AI 生成时，请在 commit message 末尾追加 trailer，注明实际使用的智能体，例如：
  
  ```
  Assisted-by: DeepSeek Harness
  ```
- 贡献流程一律**遵循 [CONTRIBUTING.md](./CONTRIBUTING.md)**。

## 项目概述

ContiNew Starter（Continue New Starter）是基于 Spring Boot 3.x 的企业级 Starter 库。它遵循"约定优于配置"理念，将一系列经过企业实践验证的第三方库（MyBatis-Plus、Sa-Token、Redisson、JetCache 等）封装为开箱即用的 Starter，供 Spring Boot Web 应用集成。**这不是一个应用项目**，而是发布到 Maven Central 的多模块 Starter 库，groupId 为 `top.continew.starter`，全部模块版本通过 `${revision}` 属性统一管理。

**当前版本**：2.17.0-SNAPSHOT | **主分支**：`dev` | **Java**：JDK 17 | **构建**：Maven 3.9.16（`./mvnw`，Windows 为 `mvnw.cmd`）

## 核心架构

关键模块及其职责：

- **continew-starter-dependencies**：根父 POM——全部第三方依赖版本（约 40 个坐标）的唯一数据源
- **continew-starter-bom**：项目 BOM——管理全部内部模块的版本
- **continew-starter**：聚合 POM——继承 `continew-starter-dependencies`，聚合所有业务模块的构建，自身不含业务代码
- **core**：核心模块——常量、`PropertiesConstants`、通用工具（线程池等自动配置）
- **json**：JSON——jackson
- **api-doc**：接口文档——Spring Doc + NextDoc4j
- **validation**：参数校验——Hibernate Validator
- **web**：Web 开发——跨域、全局异常 + 响应、链路追踪等自动配置
- **cache**：缓存——redisson / jetcache（多级缓存）/ springcache 三种实现
- **auth**：认证——satoken（国产轻量认证鉴权）/ justauth（第三方登录）
- **data**：数据访问——core + mp（MyBatis Plus）/ mf（MyBatis Flex）双 ORM 实现
- **encrypt**：加密——core / field（字段加密）/ api（API 加密）/ password-encoder（密码编码器）
- **security**：安全——mask（JSON 数据脱敏）/ xss（XSS 过滤）/ sensitivewords（敏感词）
- **ratelimiter**：限流
- **idempotent**：幂等
- **trace**：链路追踪
- **captcha**：验证码——graphic（静态）/ behavior（动态）
- **messaging**：消息——mail（邮件）/ websocket / mqtt
- **log**：日志——core / aop（基于 AOP）/ interceptor（拦截器实现）
- **excel**：Excel 处理——core / fastexcel / poi
- **storage**：存储——本地存储 & 对象存储（S3 协议，兼容主流云厂商）
- **license**：License——core / generator（生成器）/ verifier（校验器）
- **extension**：扩展——crud / datapermission（数据权限）/ tenant（租户），各含 `-core` 与 `-mp`/`-mf`

聚合模块的组织约定：`-core` 子模块定义接口与通用逻辑，`-mp`/`-mf` 提供特定 ORM 实现，使用方按需选择。

自动配置：每个 Starter 模块通过 Spring Boot 3 的 `AutoConfiguration.imports` 机制注册（**不是**旧版 `spring.factories`）；详见[配置与自动配置规范](#配置与自动配置规范)。

## 构建与测试命令

```bash
# 完整构建（全部门禁：validate 阶段 Enforcer -> Spotless -> Checkstyle，编译，verify 阶段 SpotBugs）
./mvnw verify

# 仅编译（含 validate 阶段三道门禁，不含 SpotBugs）——仅用于快速迭代，不可作为提交前自检
./mvnw compile

# 编译单个模块（含依赖模块）
./mvnw -pl :continew-starter-web -am compile

# 自动修复格式（被 Spotless 门禁拦截时使用：格式化 + 清理无用 import + 补 License Header）
./mvnw compile -Pformat

# 安装全部模块到本地 Maven 仓库
./mvnw install -DskipTests

# 清理所有 target 目录及 flatten 生成的 .flattened-pom.xml
./mvnw clean
```

本项目目前不包含单元测试模块。代码改动的验证方式是执行 `./mvnw verify` 确保四道门禁全部通过。

版本管理：`${revision}` 属性定义在 `continew-starter-dependencies` 与 `continew-starter-bom` 中，`flatten-maven-plugin` 在 `process-resources` 阶段将其解析为实际版本并生成用于发布的简化 `.flattened-pom.xml`——修改版本只需改一处。

### 提交前门禁（必须通过）

提交 Java 代码前，AI 智能体**必须**让门禁通过：

1. 执行 `./mvnw verify`。四道门禁依次为：validate 阶段的 **Enforcer**（构建环境与依赖合规）、**Spotless check**（代码格式）、**Checkstyle**（代码规范），以及编译后 verify 阶段的 **SpotBugs**（字节码缺陷），任一不通过都会直接构建失败。
2. 若被 Spotless 拦截，执行 `./mvnw compile -Pformat` 自动修复，然后再执行一次 `./mvnw verify` 确认通过。
3. 四道门禁全部通过后才能提交。

构建过程**不会修改任何源码文件**；`-Pformat` 是唯一会修改源码的 profile。不要用 IDE 格式化或 `git diff --check` 替代 Spotless 门禁——IDE 格式化引擎是另一套实现，可能放行项目格式化器拒绝的代码。

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
| 大括号 | `if/else/for/while/do-while` 必须加大括号（`NeedBraces`） |
| 空行 | 连续空行最多保留 1 行（`EmptyLineSeparator`） |
| 类注释 | 必须包含 `@author` 与 `@since` 标签 |
| 格式化豁免 | `// @formatter:off` 与 `// @formatter:on` 之间的代码不参与格式化 |
| 命名 | `style/` 配置文件与 agent 技能统一使用 `ocn-` 前缀（OCN = OpenContiNew） |

### License Header

每个新增 Java 源文件**必须**包含 LGPL-3.0 License Header。Spotless 在 validate 阶段校验，`./mvnw compile -Pformat` 可自动补全。模板位于 [`style/license-header`](style/license-header)：

```java
/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## 配置与自动配置规范

ContiNew Starter 各模块遵循严格约定。AI 智能体生成 Starter 代码时**必须**遵守以下规范。

### 配置属性前缀

所有配置属性统一使用 `continew-starter` 前缀，格式为 `continew-starter.<module>[.<sub-feature>]`。前缀常量**必须**定义在 [`PropertiesConstants`](continew-starter-core/src/main/java/top/continew/starter/core/constant/PropertiesConstants.java) 中——基于 `CONTINEW_STARTER` 基础常量拼接——并在 `@ConfigurationProperties` 与 `@ConditionalOnProperty` 中引用，禁止内联字符串字面量。

```java
public static final String CONTINEW_STARTER = "continew-starter";
public static final String WEB = CONTINEW_STARTER + StringConstants.DOT + "web";
public static final String WEB_CORS = WEB + StringConstants.DOT + "cors";               // continew-starter.web.cors
public static final String ENCRYPT_FIELD = ENCRYPT + StringConstants.DOT + "field";     // continew-starter.encrypt.field
```

### 自动配置注册

每个自动配置类**必须**注册到所属模块的 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中——每行一个全限定类名——否则不会被加载。

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

## Java 版本

- **全部模块**：Java 17（全项目统一，无向后兼容目标）

## PR 约定

所有 PR 必须提交到 `dev` 分支（新功能与功能优化）；维护分支 `x.x.x` 仅接受 bug 修复。请基于目标分支创建特性分支（如 `feat/new-feature`），不要直接修改源分支。遵循 [PR 模板](.github/PULL_REQUEST_TEMPLATE.md)。

**提交格式**：[Conventional Commits（约定式提交）1.0.0](https://www.conventionalcommits.org/zh-hans/v1.0.0/)规范，`<类型>[可选作用域]: <描述>`，破坏性变更在类型或作用域后追加 `!`，如 `feat(cache): 新增 xxx`、`feat!:`。

**提交前检查**：

```bash
./mvnw verify     # 四道门禁必须全部通过（被 Spotless 拦截时使用 -Pformat）
```

## 安全漏洞

不得通过 GitHub Issue 报告安全漏洞。请使用 GitHub 私有漏洞报告——详见 [SECURITY.md](./SECURITY.md)。

## Agent Skills

各 agent 工具（DeepSeek Harness / Claude Code / Codex）共用的技能统一存放在 `.agents/skills/` 作为唯一事实源——每个技能一个目录、含 `SKILL.md`。新增技能沿用 `ocn-` 命名前缀。
