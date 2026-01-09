# ContiNew Starter

<a href="https://central.sonatype.com/search?namespace=top.continew.starter" title="Release" target="_blank">
<img src="https://img.shields.io/maven-central/v/top.continew.starter/continew-starter.svg?label=Maven%20Central&logo=sonatype&logoColor=FFF" alt="Release" />
</a>
<a href="https://spring.io/projects/spring-boot" title="Spring Boot" target="_blank">
<img src="https://img.shields.io/badge/Spring Boot-3.3.12-%236CB52D.svg?logo=Spring-Boot" alt="Spring Boot" />
</a>
<a href="https://github.com/continew-org/continew-starter" title="Open JDK" target="_blank">
<img src="https://img.shields.io/badge/Open JDK-17-%236CB52D.svg?logo=OpenJDK&logoColor=FFF" alt="Open JDK" />
</a>
<a href="https://app.codacy.com/gh/continew-org/continew-starter/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade" title="Codacy" target="_blank">
<img src="https://app.codacy.com/project/badge/Grade/90ed633957a9410aa8745f0654827c01" alt="Codacy" />
</a>
<a href="https://sonarcloud.io/summary/new_code?id=Charles7c_continew-starter" title="Sonar" target="_blank">
<img src="https://sonarcloud.io/api/project_badges/measure?project=Charles7c_continew-starter&metric=alert_status" alt="Sonar" />
</a>
<br />
<a href="https://github.com/continew-org/continew-starter/blob/dev/LICENSE" title="License" target="_blank">
<img src="https://img.shields.io/badge/License-LGPL--3.0-blue.svg" alt="License" />
</a>
<a href="https://github.com/continew-org/continew-starter" title="GitHub Stars" target="_blank">
<img src="https://img.shields.io/github/stars/continew-org/continew-starter?style=social" alt="GitHub Stars" />
</a>
<a href="https://github.com/continew-org/continew-starter" title="GitHub Forks" target="_blank">
<img src="https://img.shields.io/github/forks/continew-org/continew-starter?style=social" alt="GitHub Forks" />
</a>
<a href="https://gitee.com/continew/continew-starter" title="Gitee Stars" target="_blank">
<img src="https://gitee.com/continew/continew-starter/badge/star.svg?theme=dark" alt="Gitee Stars" />
</a>
<a href="https://gitee.com/continew/continew-starter" title="Gitee Forks" target="_blank">
<img src="https://gitee.com/continew/continew-starter/badge/fork.svg?theme=dark" alt="Gitee Forks" />
</a>
<a href="https://gitcode.com/continew/continew-starter" title="GitCode Stars" target="_blank">
<img src="https://gitcode.com/continew/continew-starter/star/badge.svg" alt="GitCode Stars" />
</a>

📚 [在线文档](https://continew.top) | 💬 [吐槽广场（你就是 Talk King!）](https://continew.top/docs/starter/issue-hub.html) | [![问 DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/continew-org/continew-starter)

## 简介

ContiNew Starter（Continue New Starter）基于“约定优于配置”理念，进一步精简常规配置，提供完整的配置解决方案，帮助开发人员更快速地将常用第三方库或工具集成到 Spring Boot Web 应用程序中。

ContiNew Starter 封装了一系列经过企业实践验证的依赖包（如 MyBatis-Plus、SaToken），可轻松集成到应用中，减少开发人员手动引入依赖及配置的工作量，为 Spring Boot Web 项目的灵活快速构建提供支持。

> ContiNew Starter 源自 [ContiNew Admin](https://github.com/continew-org/continew-admin) 后台管理框架项目。随着 ContiNew Admin 的发展，作者发现初学者需要关注过多的脚手架通用基础能力，且在新项目中复用这些能力时存在迁移困难的问题。于是，ContiNew Starter 应运而生，作者在 ContiNew Admin 2.x 版本时，将项目中的通用基础能力进行了抽离并深度优化。这样，无论是在 ContiNew Admin 中使用，还是单独使用这些基础能力，都可以更加轻松。

## 解决痛点

在开发一个 Java Web 项目之前，我们可能需要做如下准备工作：

1. 引入 Spring Boot 父项目进行版本锁定（无 Spring 不 Java）。
2. 引入 Spring Boot Web 依赖。
3. 根据需求引入不同组件的 Starter。
4. 针对引入的 Starter 进行配置（查阅文档或通过搜索引擎查找常用配置）。 
   - 编写 Java 配置。 
   - 编写 application.yml 配置。
5. 编写各类全局处理器。
6. 开始业务开发。

在 Spring Boot “约定优于配置” 理念的帮助下，我们开发一个 Spring Java Web 程序已经简化到了不可思议的程度，而且很多设计良好的组件 Starter 提供了极大的扩展性，提供了非常多的配置，给使用者最大的可行性，当你需要处理一些自定义场景时，这些配置简直是太过方便。

然而，高度扩展性也带来了配置复杂性，新手用户在初次使用组件时往往需要花费大量精力在配置上。因此，各种脚手架项目应运而生，你可能会想，这基础配置关脚手架项目什么事？**脚手架项目的作用不仅仅是提供一系列通用基础功能，更多的是提供了一种通用的解决方案，无论是针对所使用组件的配置，还是实现的某个功能的设计，亦或是开发规范** 。即使是初学者，把脚手架项目拿过来，只需要删减不需要的功能，修改品牌元素，就可以继续在其基础上进行开发一个成熟的项目。

ContiNew Starter 将脚手架项目中的通用基础配置进行封装与深度优化，从企业实践角度精简配置，使新项目或已有项目在使用这些组件时更加便捷。

## 项目源码

| 开源平台    | 源码地址                                             |
|:--------|:-------------------------------------------------|
| GitCode | https://gitcode.com/continew/continew-starter    |
| Gitee   | https://gitee.com/continew/continew-starter      |
| GitHub  | https://github.com/continew-org/continew-starter |

## 像数1，2，3一样容易

1.在项目 pom.xml 中锁定版本（**以下两种方式任选其一**）

方式一：如您使用的是 Spring Boot Parent 的方式，则替换 Spring Boot Parent 为 ContiNew Starter。

```xml
<parent>
    <groupId>top.continew.starter</groupId>
    <artifactId>continew-starter</artifactId>
    <version>{latest-version}</version>
</parent>
```

方式二：如您使用的是引入 Spring Boot Dependencies 的方式，则替换 Spring Boot Dependencies 为 ContiNew Starter Dependencies

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- ContiNew Starter Dependencies -->
        <dependency>
            <groupId>top.continew.starter</groupId>
            <artifactId>continew-starter-dependencies</artifactId>
            <version>{latest-version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

2.在项目 pom.xml 中引入所需模块依赖

```xml
<dependencies>
    <!-- Web 模块 -->
    <dependency>
        <groupId>top.continew.starter</groupId>
        <artifactId>continew-starter-web</artifactId>
    </dependency>
</dependencies>
```

3.在 application.yml 中根据引入模块，添加所需配置

示例：跨域配置

```yaml
--- ### 跨域配置
continew-starter.web:
  cors:
    enabled: true
    # 配置允许跨域的域名
    allowed-origins: '*'
    # 配置允许跨域的请求方式
    allowed-methods: '*'
    # 配置允许跨域的请求头
    allowed-headers: '*'
    # 配置允许跨域的响应头
    exposed-headers: '*'
```

## 模块结构

```
continew-starter
├─ continew-starter-core（核心模块：包含线程池等自动配置）
├─ continew-starter-json（JSON 模块）
│  └─ continew-starter-json-jackson
├─ continew-starter-api-doc（接口文档模块：Spring Doc + NextDoc4j）
├─ continew-starter-validation（校验模块：Hibernate Validator）
├─ continew-starter-web（Web 开发模块：包含跨域、全局异常+响应、链路追踪等自动配置）
├─ continew-starter-cache（缓存模块）
│  ├─ continew-starter-cache-redisson（Redisson）
│  ├─ continew-starter-cache-jetcache（JetCache 多级缓存）
│  └─ continew-starter-cache-springcache（Spring 缓存）
├─ continew-starter-auth（认证模块）
│  ├─ continew-starter-auth-satoken（国产轻量认证鉴权）
│  └─ continew-starter-auth-justauth（第三方登录）
├─ continew-starter-data（数据访问模块）
│  ├─ continew-starter-data-core（核心模块）
│  ├─ continew-starter-data-mp（MyBatis Plus）
│  └─ continew-starter-data-mf（MyBatis Flex）
├─ continew-starter-encrypt（加密模块）
│  ├─ continew-starter-encrypt-core（核心模块）
│  ├─ continew-starter-encrypt-field（字段加密）
│  └─ continew-starter-encrypt-api（API 加密）
│  └─ continew-starter-encrypt-password-encoder（密码编码器）
├─ continew-starter-security（安全模块）
│  ├─ continew-starter-security-mask（脱敏：JSON 数据脱敏）
│  ├─ continew-starter-security-xss（XSS 过滤）
│  └─ continew-starter-security-sensitivewords（敏感词）
├─ continew-starter-ratelimiter（限流模块）
├─ continew-starter-idempotent（幂等模块）
├─ continew-starter-trace（链路追踪模块）
├─ continew-starter-captcha（验证码模块）
│  ├─ continew-starter-captcha-graphic（静态验证码）
│  └─ continew-starter-captcha-behavior（动态验证码）
├─ continew-starter-messaging（消息模块）
│  ├─ continew-starter-messaging-mail（邮件）
│  └─ continew-starter-messaging-websocket（WebSocket）
├─ continew-starter-log（日志模块）
│  ├─ continew-starter-log-core（核心模块）
│  ├─ continew-starter-log-aop（基于 AOP 实现）
│  └─ continew-starter-log-interceptor（基于拦截器实现（Spring Boot Actuator HttpTrace 增强版））
├─ continew-starter-excel（Excel 文件处理模块）
│  ├─ continew-starter-excel-core（核心模块）
│  ├─ continew-starter-excel-fastexcel（FastExcel）
│  └─ continew-starter-excel-poi（POI）
├─ continew-starter-storage（存储模块）
│  └─ continew-starter-storage-local（本地存储）
├─ continew-starter-license（License 模块）
│  ├─ continew-starter-license-core（核心模块）
│  ├─ continew-starter-license-generator（License 生成器）
│  └─ continew-starter-license-verifier（License 校验器）
└─ continew-starter-extension（扩展模块）
   ├─ continew-starter-extension-datapermission（数据权限模块）
   │  ├─ continew-starter-extension-datapermission-core（核心模块）
   │  └─ continew-starter-extension-datapermission-mp（MyBatis Plus）
   ├─ continew-starter-extension-tenant（租户模块）
   │  ├─ continew-starter-extension-tenant-core（核心模块）
   │  └─ continew-starter-extension-tenant-mp（MyBatis Plus）
   └─ continew-starter-extension-crud（CRUD 模块）
      ├─ continew-starter-extension-crud-core（核心模块）
      ├─ continew-starter-extension-crud-mp（MyBatis Plus）
      └─ continew-starter-extension-crud-mf（MyBatis Flex）
```

## 参与贡献

ContiNew（Continue New）系列项目致力于通过持续迭代，为开发者提供舒适的开发体验。作为开源社区，我们的初衷是希望通过开源协作模式，提升技术透明度、放大集体智慧、共创优秀实践，源源不断地为企业级项目开发提供助力。

我们诚挚邀请广大社区用户为 ContiNew 项目贡献力量，包括但不限于 Issue 排查、测试验证、代码开发与重构等。每一份贡献，都是推动项目进步的重要力量（请查阅 [贡献指南](https://continew.top/about/contributing.html)）。欢迎各位感兴趣的小伙伴儿，[添加微信](https://continew.top/discussion.html) 讨论或认领任务。

### 分支说明

ContiNew 系列项目采用清晰的分支策略，确保开发与维护有序进行。提交 PR 前，请确认目标分支是否处于活跃维护状态。

| 分支  | 说明                                                         |
| ----- | ------------------------------------------------------------ |
| dev   | 开发分支，用于下个大版本的 SNAPSHOT 开发，接受新功能或功能优化 PR |
| x.x.x | 维护分支，用于特定版本（如 vx.x.x）的 bug 修复，仅接受已有功能的修复 PR，不接受新功能 |

### 流程步骤

若您希望提交新功能或优化现有代码，请遵循以下步骤：

1. 在开源平台上将项目 fork 到您的个人仓库
2. 将 fork 的项目克隆到本地开发环境
3. 基于当前维护的分支（如 dev）创建新分支（如 feat/newFeature），请勿直接修改源分支（源分支仅做同步 ContiNew 最新代码用）
4. 在新分支上进行代码修改，完成后提交并 push 到您的远程仓库
5. 在开源平台上创建 pull request (PR)，选择正确的源分支和目标分支，按模板填写说明信息（参考 [已合并的 PR](https://github.com/continew-org/continew-starter/pulls?q=is%3Apr+is%3Amerged) 可提高合并率）
6. 提交 PR 后，系统会提示签署 CLA（贡献者协议）。请确保 commit 使用的邮箱与平台绑定邮箱一致（如果不一致，可以在本地通过 `git reset --soft HEAD~1` 回退，然后使用正确邮箱重新提交，最后 `git push -f` 即可，不需要重新创建 PR），然后使用该邮箱签署即可
7. 耐心等待维护者审核并合并您的 PR（建议通过交流群进行快捷沟通）
8. PR 合并后，下次贡献前请先同步最新代码，再重复步骤 3 开始

> [!IMPORTANT]
> 为了确保项目质量和协作效率，请注意以下事项：
>
> 1. 代码和配置文件请参考已有风格，遵循清晰的结构与命名规范，提供完善的注释（符合阿里巴巴 <a href="https://github.com/continew-org/continew-starter/blob/dev/.style/Java%E5%BC%80%E5%8F%91%E6%89%8B%E5%86%8C(%E9%BB%84%E5%B1%B1%E7%89%88).pdf" target="_blank">《Java开发手册(黄山版)》</a> 中的代码规范）
> 2. 提交代码前请关闭所有代码窗口，执行 `mvn compile` 命令进行代码格式化（ContiNew 项目后端编译时会自动执行插件进行代码格式修正）。编译通过后请勿再次打开代码窗口，避免不同 IDE 配置导致的格式差异
> 3. 提交时，请按照 [Angular 提交规范](https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-angular) 编写 commit message（参考已有风格）

## 反馈交流

感谢您对 ContiNew 开源项目的关注与支持！我们非常重视每一位用户的反馈和建议，这是推动项目不断进步的动力。 欢迎扫描下方二维码加入我们的官方交流群，与项目维护团队及其他大佬用户实时交流探讨。

- 与项目核心团队直接沟通，获取第一手项目动态
- 解决使用过程中遇到的问题，分享经验心得
- 参与功能讨论和需求收集，影响项目未来发展
- 结识志同道合的技术爱好者，扩展人脉圈

<div align="left">
  <img src=".image/qrcode.jpg" alt="二维码" height="230px" />
</div>

## 鸣谢

### 鸣谢

感谢参与贡献的每一位小伙伴🥰

<a href="https://github.com/continew-org/continew-starter/graphs/contributors">
	<img src="https://contrib.rocks/image?repo=continew-org/continew-starter" />
</a>

### 特别鸣谢

- 感谢 <a href="https://www.jetbrains.com/" target="_blank">JetBrains</a> 提供的 <a href="https://jb.gg/OpenSourceSupport" target="_blank">非商业开源软件开发授权</a> 
- 感谢 <a href="https://github.com/baomidou/mybatis-plus" target="_blank">MyBatis Plus</a>、<a href="https://github.com/dromara/sa-token" target="_blank">Sa-Token</a> 、<a href="https://github.com/alibaba/jetcache" target="_blank">JetCache</a>、<a href="https://github.com/opengoofy/crane4j" target="_blank">Crane4j</a>、<a href="https://nextdoc4j.top/" target="_blank">NextDoc4j</a>、<a href="https://github.com/dromara/hutool" target="_blank">Hutool</a> 等开源组件作者为国内开源世界作出的贡献
- 感谢 <a href="https://github.com/elunez/eladmin" target="_blank">ELADMIN</a>、<a href="https://github.com/dromara/RuoYi-Vue-Plus" target="_blank">RuoYi-Vue-Plus</a>、<a href="https://gitee.com/herodotus/dante-engine" target="_blank">Dante-Engine</a>，致敬各位作者为开源脚手架领域作出的贡献
  - e.g. 起源于 ELADMIN 项目开源的 QueryHelper 组件
  - e.g. 扩展于 Dante-Engine 项目封装的 Redisson 相关配置
- 感谢项目使用或未使用到的每一款开源组件，致敬各位开源先驱 :fire:

## License

- 遵循 <a href="https://github.com/continew-org/continew-starter/blob/dev/LICENSE" target="_blank">LGPL-3.0</a> 开源许可协议
- Copyright © 2022-present <a href="https://charles7c.top" target="_blank">Charles7c</a>

## GitHub Star 趋势

![GitHub Star 趋势](https://starchart.cc/charles7c/continew-starter.svg)