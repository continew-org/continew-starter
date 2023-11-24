# ContiNew Starter

<a href="https://github.com/Charles7c/continew-starter/blob/dev/LICENSE" target="_blank">
<img src="https://img.shields.io/badge/License-LGPL--3.0-blue.svg" alt="License" />
</a>
<a href="https://github.com/Charles7c/continew-starter" target="_blank">
<img src="https://img.shields.io/badge/SNAPSHOT-v1.0.0-%23ff3f59.svg" alt="Release" />
</a>
<a href="https://github.com/Charles7c/continew-starter" target="_blank">
<img src="https://img.shields.io/badge/SpringBoot-3.1.5-%236CB52D.svg" alt="Release" />
</a>
<a href="https://github.com/Charles7c/continew-starter" target="_blank">
<img src="https://img.shields.io/badge/Java-17-%236CB52D.svg" alt="Release" />
</a>
<a href="https://github.com/Charles7c/continew-starter" target="_blank">
<img src="https://img.shields.io/github/stars/Charles7c/continew-starter?style=social" alt="GitHub stars" />
</a>
<a href="https://github.com/Charles7c/continew-starter" target="_blank">
<img src="https://img.shields.io/github/forks/Charles7c/continew-starter?style=social" alt="GitHub forks" />
</a>
<a href="https://gitee.com/Charles7c/continew-starter" target="_blank">
<img src="https://gitee.com/Charles7c/continew-starter/badge/star.svg?theme=white" alt="Gitee stars" />
</a>
<a href="https://gitee.com/Charles7c/continew-starter" target="_blank">
<img src="https://gitee.com/Charles7c/continew-starter/badge/fork.svg?theme=white" alt="Gitee forks" />
</a>

✨ [ContiNew Admin 脚手架](https://cnadmin.charles7c.top/)

## 简介

ContiNew Starter（Continue New Starter）是一种特殊类型的 Spring Boot Starter，其作用与常规的 Starter 类似，它可以帮助开发人员快速集成常用的第三方库或工具到 Spring 应用程序中。ContiNew Starter 包含了一系列经过优化和配置的依赖包（如 MyBatis-Plus、SaToken），可轻松集成到应用中，从而避免开发人员手动引入依赖的麻烦，为 Spring Boot 项目的灵活快速构建提供支持。

## 项目源码

| 开源平台      | 源码地址                                    |
| ------------- | ------------------------------------------- |
| GitHub        | https://github.com/Charles7c/continew-starter |
| Gitee（码云） | https://gitee.com/Charles7c/continew-starter  |

## 反馈交流

💬 欢迎各位小伙伴儿扫描下方二维码加好友，备注 `cnadmin`，拉你进群，探讨技术、提提需求~   

加入交流群后，你将会：

- 第一时间收到框架动态
- 第一时间收到框架更新通知
- 第一时间收到框架 Bug 通知
- 和众多大佬互相 (huá shuǐ) 交流 (mō yú)

<div align="left">
  <img src="https://doc.charles7c.top/qrcode.jpg" alt="二维码" width="230px" />
</div>

<details>
<summary>无加群意愿</summary>
💬 如无加群意愿，欢迎在 <a href="https://github.com/Charles7c/continew-starter/discussions" target="_blank">Discussions</a> 中进行交流探讨~ 🍻
</details>

## 快速使用

> **Note**
> 目前 ContiNew Starter 尚处于开发中，若要提前体验 Snapshot（快照）版本，请按以下方法进行。如遇问题，诚邀反馈。

1.在项目 pom.xml 中配置 Snapshot（快照）仓库地址

```xml
<repositories>
    <repository>
        <id>sonatype-nexus-snapshots</id>
        <name>Sonatype Nexus Snapshots</name>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
        <snapshots>
            <updatePolicy>always</updatePolicy>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

2.在项目 pom.xml 中锁定版本（**下方两种方式请任选其一**）

第一种方式：如您使用的是 Spring Boot Parent 的方式，则替换 Spring Boot Parent 为 ContiNew Starter

```xml
<parent>
    <groupId>top.charles7c.continew</groupId>
    <artifactId>continew-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

第二种方式：如您使用的是引入 Spring Boot Dependencies 的方式，则替换 Spring Boot Dependencies 为 ContiNew Starter Dependencies

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
            <groupId>top.charles7c.continew</groupId>
            <artifactId>continew-starter-dependencies</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

3.在项目 pom.xml 中引入所需模块依赖

```xml
<dependencies>
    <!-- 核心模块 -->
    <dependency>
        <groupId>top.charles7c.continew</groupId>
        <artifactId>continew-starter-core</artifactId>
    </dependency>
</dependencies>
```

4.在  application.yml  中根据引入模块，添加所需配置

e.g. 跨域配置

```yaml
--- ### 跨域配置
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

| 模块名称                 | 模块作用                                   |
| ------------------------ | ------------------------------------------ |
| continew-starter-core    | 核心模块（包含跨域、线程池自动配置）       |
| continew-starter-api-doc | API 文档模块（包含 Knife4j 自动配置）      |
| continew-starter-json    | JSON 模块（包含 Jackson 自动配置）         |
| continew-starter-cache   | 缓存模块（包含 Redisson 自动配置）         |
| continew-starter-data    | 数据访问模块（包含 MyBatis Plus 自动配置） |

## 贡献代码

### 分支说明

ContiNew Starter 的分支目前分为下个大版本的开发分支和上个大版本的维护分支，PR 前请注意对应分支是否处于维护状态。

| 分支  | 说明                                                         |
| ----- | ------------------------------------------------------------ |
| dev   | 开发分支，默认为下个大版本的 SNAPSHOT 版本，接受新功能或新功能优化 PR |
| x.x.x | 维护分支，在 vx.x.x 版本维护期终止前（一般为下个大版本发布前），用于修复上个版本的 Bug，只接受已有功能修复，不接受新功能 PR |

### 流程步骤

如果您想提交新功能或优化现有代码，可以按照以下步骤操作：

1. 首先，在 Gitee 或 Github 上将项目 fork 到您自己的仓库
2. 然后，将 fork 过来的项目（即您的项目）克隆到本地
3. 切换到当前仍在维护的分支（请务必充分了解分支使用说明，可进群联系维护者确认）
4. 开始修改代码，修改完成后，将代码 commit 并 push 到您的远程仓库
5. 在 Gitee 或 Github 上新建 pull request（pr），选择好源和目标，按模板要求填写说明信息后提交即可（多多参考 [已批准合并的 pr 记录](https://github.com/Charles7c/continew-starter/pulls?q=is%3Apr+is%3Amerged)，会大大增加批准合并率）
6. 最后，耐心等待维护者合并您的请求即可

请记住，如果您有任何疑问或需要帮助，我们将随时提供支持。

> **IMPORTANT**
> 欢迎大家为 ContiNew Starter 贡献代码，我们非常感谢您的支持！为了更好地管理项目，维护者有一些要求：
>
> 1. 请确保代码、配置文件的结构和命名规范良好，完善的代码注释，并遵循阿里巴巴的 <a href="https://github.com/Charles7c/continew-starter/blob/dev/code-style/Java%E5%BC%80%E5%8F%91%E6%89%8B%E5%86%8C(%E9%BB%84%E5%B1%B1%E7%89%88).pdf" target="_blank">《Java开发手册(黄山版)》</a> 中的代码规范，保证代码质量和可维护性
> 2. 在提交代码前，请按照 [Angular 提交规范](https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-angular) 编写 commit 的 message（建议在 IntelliJ IDEA 中下载并安装 Git Commit Template 插件，以便按照规范进行 commit）
> 3. 提交代码之前，请关闭所有代码窗口，执行 mvn compile 命令，编译通过后，不要再打开查看任何代码窗口，直接提交即可

## License

- 遵循 <a href="https://github.com/Charles7c/continew-starter/blob/dev/LICENSE" target="_blank">LGPL-3.0</a> 开源许可协议
- Copyright © 2022-present <a href="https://blog.charles7c.top" target="_blank">Charles7c</a>

## GitHub Star 趋势

![GitHub Star 趋势](https://starchart.cc/charles7c/continew-starter.svg)