## [v1.5.1](https://github.com/continew-org/continew-starter/compare/v1.5.0...v1.5.1) (2024-03-23)

### ✨ 新特性

- 【web】FileUploadUtils 新增文件下载方法 ([171040b](https://github.com/continew-org/continew-starter/commit/171040b674ae18dd6560f1501f7c0d47a1f4b8ba))

### 🐛 问题修复

- 【api-doc】修复接口级鉴权配置不生效的问题 ([ab4a72e](https://github.com/continew-org/continew-starter/commit/ab4a72e0fffe20b492c2250cc997ba3e94794118))

### 📦 依赖升级

- 【dependencies】Redisson 3.27.1 => 3.27.2 ([0f43e1c](https://github.com/continew-org/continew-starter/commit/0f43e1cff7f2fc58273de300acf4432f3a300af4))
- 【dependencies】Crane4j 2.6.0 => 2.6.1 ([0f43e1c](https://github.com/continew-org/continew-starter/commit/0f43e1cff7f2fc58273de300acf4432f3a300af4))

## [v1.5.0](https://github.com/continew-org/continew-starter/compare/v1.4.1...v1.5.0) (2024-03-08)

### 💎 功能优化

- 【data/mybatis-plus】重构 IService 及 BaseService 体系结构 ([36ce07b](https://github.com/continew-org/continew-starter/commit/36ce07b600fc54eeca3682d09aa5992cb2b35c17))
- 【json/jackson】优化 Jackson 默认配置 ([a54294d](https://github.com/continew-org/continew-starter/commit/a54294df6e24dc36d36cf6a94559af2ed4c32d44))
- 【extension/crud】调整 BaseController checkPermission 方法的访问修饰符 private => protected ([12bdf3e](https://github.com/continew-org/continew-starter/commit/12bdf3e44a10e9683b8605642eac9c463c590af4))

### 📦 依赖升级

- 【dependencies】Spring Boot 3.1.8 => 3.1.9 ([98261b9](https://github.com/continew-org/continew-starter/commit/98261b96dacdba9d210790112248d798e292ae0b))
- 【dependencies】Redisson 3.26.0 => 3.27.1 ([98261b9](https://github.com/continew-org/continew-starter/commit/98261b96dacdba9d210790112248d798e292ae0b))
- 【dependencies】Crane4j 2.5.0 => 2.6.0 ([98261b9](https://github.com/continew-org/continew-starter/commit/98261b96dacdba9d210790112248d798e292ae0b))
- 【dependencies】Hutool 5.8.25 => 5.8.26 ([98261b9](https://github.com/continew-org/continew-starter/commit/98261b96dacdba9d210790112248d798e292ae0b))
- 【dependencies】CosId 2.6.5 => 2.6.6 ([98261b9](https://github.com/continew-org/continew-starter/commit/98261b96dacdba9d210790112248d798e292ae0b))
- 【dependencies】Amazon S3 1.12.651 => 1.12.675 ([98261b9](https://github.com/continew-org/continew-starter/commit/98261b96dacdba9d210790112248d798e292ae0b))
- 【dependencies】Ip2region 3.1.7 => 3.1.9 ([98261b9](https://github.com/continew-org/continew-starter/commit/98261b96dacdba9d210790112248d798e292ae0b))
- 【dependencies】TTL 2.14.4 => 2.14.5 ([98261b9](https://github.com/continew-org/continew-starter/commit/98261b96dacdba9d210790112248d798e292ae0b))

## [v1.4.1](https://github.com/continew-org/continew-starter/compare/v1.4.0...v1.4.1) (2024-02-21)

### ✨ 新特性

- 【data/core】新增 continew-starter-data-core 模块 ([4ffc5dc](https://github.com/continew-org/continew-starter/commit/4ffc5dc1d4cdbb572070312eef172a197e216318))
- 【data/core】新增获取数据库类型带默认类型方法 ([31ca1fd](https://github.com/continew-org/continew-starter/commit/31ca1fda528629906958d8422897ac8ae179daab))

### 💎 功能优化

- 【security/crypto】调整部分 StrUtil => CharSequenceUtil ([2e5788f](https://github.com/continew-org/continew-starter/commit/2e5788f007f61f0ec531aa69834229d128311398))
- 【data/mybatis-plus】移除 ` 符号的使用，保持数据库无关性 ([557ea13](https://github.com/continew-org/continew-starter/commit/557ea1375728695c027004e64e2fa8d2d66215e6))
- 【core】完善自定义异常类构造方法 ([d42585c](https://github.com/continew-org/continew-starter/commit/d42585cb4d6660724db004893f57a6c67b4690e1))
- 【cache/redisson】完善 Redisson 工具类 ([9ed2dac](https://github.com/continew-org/continew-starter/commit/9ed2dac00c58621cbc133e3e072c100306cf170a))
- 优化字符串模板方法 API 使用 ([c986784](https://github.com/continew-org/continew-starter/commit/c9867844b650a69f6b3b3ea4f9af67807091eb1b))

### 🐛 问题修复

- 【data/mybatis-plus】修复 Query In、Not In 查询数据类型转换错误 ([9bd4583](https://github.com/continew-org/continew-starter/commit/9bd458322339f3197f1925347ff16f53fe0f3856))

### 💥 破坏性变更

- 【log/core】continew-starter-log-common => continew-starter-log-core ([56a22c4](https://github.com/continew-org/continew-starter/commit/56a22c4bce2445fb135d1fce7b6155fd5b48051e))
- 【data/mybatis-plus】调整 Query 相关类到 data-core ([3f2a306](https://github.com/continew-org/continew-starter/commit/3f2a306cad1d15436ae36c1b2eb54f28b50e84b9))
- 【extension/crud】调整 IService 到 data-core ([ab7e987](https://github.com/continew-org/continew-starter/commit/ab7e987672202f3e80c0e4f64ea0c576ff7cc89f))
- 更新 Gitee 项目链接 ([89108ad](https://github.com/continew-org/continew-starter/commit/89108ad55addeaf47f224f4ed90ecb42244dbfd8))

## [v1.4.0](https://github.com/continew-org/continew-starter/compare/v1.3.0...v1.4.0) (2024-02-14)

### ✨ 新特性

- 【captcha/graphic】新增图形验证码服务接口，并调整验证码默认启用 ([3184faa](https://github.com/continew-org/continew-starter/commit/3184faaa27111845867d2210f0db16381d53d800))
- 【log/httptrace-pro】Log 注解新增 include、exclude 属性，用于扩展或减少日志包含信息 ([669ea85](https://github.com/continew-org/continew-starter/commit/669ea85658c89c631518def8f84d4f5d60059ad7)) ([2afb0b6](https://github.com/continew-org/continew-starter/commit/2afb0b625fc936364c6dacacc735ce421c5cb37c))
- 【security/mask】新增安全模块-脱敏，支持 JSON 数据脱敏 ([7b79519](https://github.com/continew-org/continew-starter/commit/7b795194d3db979c239ab30d78fdb61d95f06896)) ([111e732](https://github.com/continew-org/continew-starter/commit/111e7329673778c475c4ff4aa5ba6eef9f43f506))
- 【security/crypto】新增安全模块-加密，支持 MyBatis ORM 框架字段加密 ([5ccdd9e](https://github.com/continew-org/continew-starter/commit/5ccdd9e5da2a81d6a1f69bdf3f0e4eb1475b68a0)) ([88f82d1](https://github.com/continew-org/continew-starter/commit/88f82d1c0aa5abf8f094564f4b84ae84efd80946)) ([b604f2f](https://github.com/continew-org/continew-starter/commit/b604f2fc7eb938a52338ee41cf1823af374a14da)) ([74a1166](https://github.com/continew-org/continew-starter/commit/74a1166b5f250c2ba8aab027d98bc11e59860c01)) ([9ebcd14](https://github.com/continew-org/continew-starter/commit/9ebcd14878b499039a70380b0773b00b9f8dc111))
- 【security/all】新增 continew-starter-security-all 模块，统一引入加密、脱敏、密码编码器能力 ([12c3d64](https://github.com/continew-org/continew-starter/commit/12c3d640668298439ef0b610f5b36848e1f91b1a))

### 💎 功能优化

- 【log/httptrace-pro】默认启用日志 ([2aea8ba](https://github.com/continew-org/continew-starter/commit/2aea8ba8318dded142a274221af7de2b62d4ced9))
- 【log/httptrace-pro】兼容小写 user-agent 情况 ([18b9d1b](https://github.com/continew-org/continew-starter/commit/18b9d1ba799ce96d8831b7243508b2517ff5c5c7))
- 【auth/satoken】JWT 配置支持启用/关闭 ([c33a670](https://github.com/continew-org/continew-starter/commit/c33a6709f50c2240cc9826c4ee2e83d88db5fb07))
- 【cache/redisson】优化协议前缀变量命名 ([00798bd](https://github.com/continew-org/continew-starter/commit/00798bdb4c82c8ec8b3cf1110a0afaaa94ad2b27))
- 【auth】调整 Redisson 模块为可选依赖 ([00bba33](https://github.com/continew-org/continew-starter/commit/00bba33517c15936ec2f40a8a7f3213d25a223aa))
- 【data/mybatis-plus】重构 ID 生成器配置，支持默认、CosId、自定义 ([c9311df](https://github.com/continew-org/continew-starter/commit/c9311df093d4524b272535640333c413a2eda86f)) ([58dc51f](https://github.com/continew-org/continew-starter/commit/58dc51f66c3a77f7f1621557cdd065243b6ae5a9))
- 【message/sms】精简部分依赖 ([f67f278](https://github.com/continew-org/continew-starter/commit/f67f278784002de553c923f399d585e35f0a6356))
- 根据 Sonar 建议调整，StrUtil => CharSequenceUtil ([ea71cf5](https://github.com/continew-org/continew-starter/commit/ea71cf573b7b6452b9315c67967f29e25468a04a))

### 🐛 问题修复

- 【extension/crud】修复删除后置处理方法访问修饰符使用错误 ([24f9975](https://github.com/continew-org/continew-starter/commit/24f99754d041e113f07eb43570d6a49c4ff24008))
- 【message/mail】修复发送邮件收件人不为空判断错误 ([Gitee PR#12](https://gitee.com/continew/continew-starter/pulls/12))
- 【auth/satoken】修复 SaInterceptor Bean 获取方式错误 ([1ba1596](https://github.com/continew-org/continew-starter/commit/1ba1596f4e4b31d82e174e981711e45a1df67ee7))

### 📦 依赖升级

- 【dependencies】Spring Boot 3.1.7 => 3.1.8 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】Dynamic Datasource 4.2.0 => 4.3.0 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】JetCache 2.7.4 => 2.7.5 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】Redisson 3.25.2 => 3.26.0 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】SMS4J 3.0.4 => 3.1.1 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】X File Storage 2.0.0 => 2.1.0 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】Amazon S3 1.12.626 => 1.12.651 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】Crane4j 2.4.0 => 2.5.0 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8)) ([c963978](https://github.com/continew-org/continew-starter/commit/c96397898027140c243b034dc3d23bd3d60695e7))
- 【dependencies】Knife4j 4.4.0 => 4.5.0 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】Hutool 5.8.24 => 5.8.25 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】ip2region 3.1.6 => 3.1.7 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】flatten-maven-plugin 1.5.0 => 1.6.0 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))
- 【dependencies】spotless-maven-plugin 2.40.0 => 2.43.0 ([ab76665](https://github.com/continew-org/continew-starter/commit/ab76665aab8cf06e508f039dbce13959e171d0c8))

## [v1.3.0](https://github.com/continew-org/continew-starter/compare/v1.2.0...v1.3.0) (2024-02-03)

### ✨ 新特性

* 【data/mybatis-plus】新增 QueryIgnore 忽略查询解析注解 ([91651b0](https://github.com/continew-org/continew-starter/commit/91651b0b59cf642cd59aca068d8bca4554dc9895))
* 【security/password】新增安全模块-密码编码器自动配置 ([47a4d57](https://github.com/continew-org/continew-starter/commit/47a4d57dee3739de12ccbe9e15e25aef5b9ae558)) ([Gitee PR#9](https://gitee.com/continew/continew-starter/pulls/9))
* 【web】新增链路跟踪自动配置 ([8fc19ab](https://github.com/continew-org/continew-starter/commit/8fc19ab9b87b1a1b6d290ee9a40d0157de267324))

### 💎 功能优化

- 【extension/crud】排序字段增加是否存在校验 ([Gitee PR#7](https://gitee.com/continew/continew-starter/pulls/7))
- 【data/mybatis-plus】优化数据权限处理器代码结构 ([aecefa1](https://github.com/continew-org/continew-starter/commit/aecefa15ecbb9660f2ffa2f3bef3ad9eeb810916))
- 【auth/satoken】支持更灵活的动态化路由拦截鉴权 ([31f29db](https://github.com/continew-org/continew-starter/commit/31f29db19dede2cbf6988946b0dd8c8f153d1bd9))
- 【auth/satoken】优化 SaToken 持久层配置 ([e6f8ac8](https://github.com/continew-org/continew-starter/commit/e6f8ac8afa1b6c487343dc88d8ac7fdfde40e58b))
- 【captcha/behavior】优化行为验证码缓存配置 ([8598e6d](https://github.com/continew-org/continew-starter/commit/8598e6d109c1ca6be3e973ceb41c6dd7bd93c333))
- 【storage/local】优化存储模块依赖 ([dcb6568](https://github.com/continew-org/continew-starter/commit/dcb6568916cd549f1c403ece1c4f4d29ecc320b9))
- 移除 Lombok 私有构造注解使用 ([11d0798](https://github.com/continew-org/continew-starter/commit/11d0798f92a5fe4eda6300a7e6065f2d3afef0df))
- 移除 Lombok 依赖，再度精简依赖 ([0eb6afa](https://github.com/continew-org/continew-starter/commit/0eb6afabb6ccaa9d421981280c896e381f68b9a6)) ([Gitee PR#9](https://gitee.com/continew/continew-starter/pulls/9))
- 新增 Qodana、Sonar 扫描 ([ab1e999](https://github.com/continew-org/continew-starter/commit/ab1e999094d9349a24eff51382a940f0ec682801)) ([1a8c589](https://github.com/continew-org/continew-starter/commit/1a8c589083f80eddd2fe7e4c99751c699dd4d357))
- 优化大量代码，解决 [Sonar](https://sonarcloud.io/organizations/charles7c/projects)、[Codacy](https://app.codacy.com/gh/Charles7c/continew-admin/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)、[Qodana](https://qodana.cloud/organizations/pQDPD/teams/p5jqd/) 扫描问题，点击各链接查看对应实时质量分析报告（Codacy 已达到 A）

### 🐛 问题修复

- 【web】配置 Validator 失败立即返回模式 ([1223f60](https://github.com/continew-org/continew-starter/commit/1223f6052d459087419b7373b8a2d7cfa36ea45c))

### 💥 破坏性变更

- 【data/mybatis-plus】重构 QueryHelper => QueryWrapperHelper，支持多列查询，并删除 blurry 属性 ([6dc20e8](https://github.com/continew-org/continew-starter/commit/6dc20e8909073f771c33736262290fe14095b2e7)) ([f16b968](https://github.com/continew-org/continew-starter/commit/f16b968b3f161c58144e59c67629b5787ba2d60d)) ([13a6809](https://github.com/continew-org/continew-starter/commit/13a6809e2aa9744b3c5ca3558d5709af7cde4698))
- 【extension/crud】优化包结构 ([eabedd8](https://github.com/continew-org/continew-starter/commit/eabedd861b533068d4fed31c412401fdba50aa63))
- 【captcha/graphic】优化图形验证码自动配置，提供 Captcha Bean ([30d7631](https://github.com/continew-org/continew-starter/commit/30d76314d66c392e36411229afeaed045f491d7a))

## [v1.2.0](https://github.com/continew-org/continew-starter/compare/v1.1.1...v1.2.0) (2024-01-20)

### ✨ 新特性

* 【extension/crud】新增 Easy Excel 枚举接口转换器 ([8936268](https://github.com/continew-org/continew-starter/commit/8936268038b4f554d00f738a2311b560bda205d8))
* 【extension/crud】适配 Crane4j 数据填充组件 ([5d26f34](https://github.com/continew-org/continew-starter/commit/5d26f343da7c467905fd08dfd06aaa2c50e8bcce))
* 【extension/crud】新增钩子方法，用于增强增、删、改方法 ([43dba72](https://github.com/continew-org/continew-starter/commit/43dba72cee9cb148a53ec2df23b0ac2854a0a42d))
* 【extension/crud】新增 IService 通用业务接口 ([926c92c](https://github.com/continew-org/continew-starter/commit/926c92cc321e5da9279400741986f71173a3eda3))
* 【extension/crud】新增启用注解，便于灵活控制启用/关闭 CRUD REST API、全局异常处理器增强 ([9398d68](https://github.com/continew-org/continew-starter/commit/9398d686bbd3b87a2a82e273a5bda37d05ca6f30))
* 【cache/springcache】新增 Spring Cache 自动配置 ([e090083](https://github.com/continew-org/continew-starter/commit/e090083ba26342aaf8378206949d6350f4f1444f))
* 【cache/jetcache】新增 JetCache 自动配置 ([156b02b](https://github.com/continew-org/continew-starter/commit/156b02b3d77fa9f0476c23182d35df030a3ea66a))
* 【web】新增 Web 模块，从核心模块拆分 Web 相关自动配置 ([9cf76fe](https://github.com/continew-org/continew-starter/commit/9cf76fe61f2368244a501c1c036c0a55502f5c0a))

### 💎 功能优化

- 新增部分 Maven 插件版本锁定 ([be14bca](https://github.com/continew-org/continew-starter/commit/be14bca2ca6ba5a808f7feebaafcf9356d338643))
- 移除部分无用 Maven 配置 ([6d9e8b4](https://github.com/continew-org/continew-starter/commit/6d9e8b43ebe8d891ab459a2c2f21e06936abdc1d))
- 全局统一 Hutool 版本，精简各模块 Hutool 依赖 ([Gitee PR#6](https://gitee.com/continew/continew-starter/pulls/6))
- 调整部分类的所在包 ([b4b40b4](https://github.com/continew-org/continew-starter/commit/b4b40b4cb929824e44bc7ad8737cbe73b283b34d))

### 🐛 问题修复

- 【log/httptrace-pro】修复隐藏接口仍然被记录请求日志的问题 ([f3ad2c4](https://github.com/continew-org/continew-starter/commit/f3ad2c48a9511ef611d414596539e838adef8e45))

### 💥 破坏性变更

- 【extension/crud】移动全局异常处理器到 Web 模块 ([ec0ebd0](https://github.com/continew-org/continew-starter/commit/ec0ebd00e49a2e67daa97d4a4f531f49acd5d89d))

## [v1.1.2](https://github.com/continew-org/continew-starter/compare/v1.1.0...v1.1.2) (2024-01-11)

> 由于发布 `v1.1.1` 至 Maven 仓库时出现异常，且按其规则无法修改错误数据，改为递增版本号为 `v1.1.2` 并发布。

### ✨ 新特性

* 【extension/crud】BaseService 增加 list 查询列表方法重载 ([81ed292](https://github.com/continew-org/continew-starter/commit/81ed29284090edcfc5ea5351442b5de2ce1622df))
* 【core】新增 SpringUtils 工具类 ([3de75cf](https://github.com/continew-org/continew-starter/commit/3de75cf7fe79bc86ca5022d56e5f46be4d90d623))

### 💎 功能优化

- 【log/httptrace-pro】优化日志过滤器，仅在需要记录请求体、响应体时进行过滤 ([d68d88d](https://github.com/continew-org/continew-starter/commit/d68d88db218d5008140c3056827dd6ac608a8b62))
- 【log/httptrace-pro】优化 @Log 注解信息获取优先级逻辑 ([Gitee PR#5](https://gitee.com/continew/continew-starter/pulls/5))
- 【extension/crud】优化 BaseServiceImpl 中获取各泛型参数类型的方式 ([6fc0b51](https://github.com/continew-org/continew-starter/commit/6fc0b51a574434db9d21d1f254b3fce344c9f2f6))
- 【extension/crud】减少查询列表时可能的无用转换 ([0565372](https://github.com/continew-org/continew-starter/commit/0565372e9aa8010a1c4625be4cf85d557a7eed7b))
- 使用常量优化部分配置属性名 ([2025068](https://github.com/continew-org/continew-starter/commit/20250681da7682de159b6259e80193b204e55047))
- 优化日志级别 info => debug ([1e7d4b2](https://github.com/continew-org/continew-starter/commit/1e7d4b2721fae3459cb6d1b57f208f0c38dbbc6f))
- 优化全局代码格式 ([57c21a9](https://github.com/continew-org/continew-starter/commit/57c21a9109a412ed78c6c9b8aa0cd0f0b5724432))

### 💥 破坏性变更

- 【extension/crud】PageDataResp => PageResp ([38d2800](https://github.com/continew-org/continew-starter/commit/38d28004d63a0218bfcae5689f9909ce6dcd824f))

## [v1.1.0](https://github.com/continew-org/continew-starter/compare/v1.0.1...v1.1.0) (2023-12-31)

### ✨ 新特性

* 【log/httptrace-pro】新增 continew-starter-log-httptrace-pro 日志模块（Spring Boot Actuator HttpTrace 重置增强版）
* 【storage/local】新增 continew-starter-storage-local 本地存储模块 ([cd6826a](https://github.com/continew-org/continew-starter/commit/cd6826a0abe0666f9fe867e92bf70abb47e5ff2e))
* 【cache/redisson】RedisUtils 新增限流方法 ([9cf3ae8](https://github.com/continew-org/continew-starter/commit/9cf3ae87a1a20db9ee8b2b7272e8328b5fc5c20c))
* 【data/mybatis-plus】新增数据权限默认解决方案 ([621a5e3](https://github.com/continew-org/continew-starter/commit/621a5e3b22db9b81d31c65b39ad387a8531e09af))
* 【captcha/behavior】新增 continew-starter-captcha-behavior 行为验证码模块 ([Gitee PR#1](https://gitee.com/continew/continew-starter/pulls/1))
* 【core】新增 PATH_PATTERN 字符串常量 ([76e282c](https://github.com/continew-org/continew-starter/commit/76e282c7965fdfa39854fe77397687bbc40d0f7f))

### 💎 功能优化

- 【core】优化跨域配置默认值 ([65f5fbd](https://github.com/continew-org/continew-starter/commit/65f5fbd6daa9ae2c8aedd13c487e8985523233ce))
- 【extension/crud】新增全局异常处理器 ([c4459d1](https://github.com/continew-org/continew-starter/commit/c4459d1b8d701a4405f74ea92cfc87752a285b55))
- 【extension/crud】移除部分方法中仅有单个非读操作的事务处理 ([70ae383](https://github.com/continew-org/continew-starter/commit/70ae383de62bc3c6ae0d2e1c3cf5c005d54f83f5))

### 📦 依赖升级

- 【dependencies】Spring Boot 3.1.5 => 3.1.7 ([72f5569](https://github.com/continew-org/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Just Auth 1.16.5 => 1.16.6 ([72f5569](https://github.com/continew-org/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Redisson 3.24.3 => 3.25.2 ([72f5569](https://github.com/continew-org/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Easy Excel 3.3.2 => 3.3.3 ([72f5569](https://github.com/continew-org/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Knife4j 4.3.0 => 4.4.0 ([72f5569](https://github.com/continew-org/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Hutool 5.8.23 => 5.8.24 ([72f5569](https://github.com/continew-org/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】MyBatis Plus 3.5.4.1 => 3.5.5（修复与 Spring Boot 3.1.7 的 DdlApplicationRunner冲突错误） ([556bfb9](https://github.com/continew-org/continew-starter/commit/556bfb924a1e5834fe0a101b9ff52cc5bb36d578))
- 【dependencies】新增 X File Storage 依赖版本 2.0.0 ([be7972c](https://github.com/continew-org/continew-starter/commit/be7972c00be8d62cc25332e053a985532016de2d))
- 【dependencies】ip2region 3.1.5.1 => 3.1.6 ([4dae89e](https://github.com/continew-org/continew-starter/commit/4dae89e0f21ac6c532101e983ee4007f3980c929))
- 【dependencies】新增 Amazon S3 依赖版本 1.12.626 ([48f894b](https://github.com/continew-org/continew-starter/commit/48f894b8b62f8b968091dcea51b57336b97e4a2d))

### 💥 破坏性变更

- 【captcha/graphic】优化图形验证码配置前缀 ([e0e5944](https://github.com/continew-org/continew-starter/commit/e0e5944b45bcbf8a4b7a5066ad347459a7b3e450))
- 【data/mybatis-plus】调整 IBaseEnum 所属包 enums => base ([22fee2f](https://github.com/continew-org/continew-starter/commit/22fee2f5bd8211e26c2f6a163a6298f5b522833c))
- 【auth/satoken】SaTokenDaoTypeEnum => SaTokenDaoType ([0a0d022](https://github.com/continew-org/continew-starter/commit/0a0d022586dc88a773512c5761c68d62786e35c4))
- 【core】使用常量优化部分魔法值，核心模块部分配置前缀调整 ([52dce2a](https://github.com/continew-org/continew-starter/commit/52dce2acdfa0296c3f6f4875f14a0299f999f899))

## [v1.0.1](https://github.com/continew-org/continew-starter/compare/v1.0.0...v1.0.1) (2023-12-13)

### 💎 功能优化

- 【data/mybatis-plus】QueryTypeEnum => QueryType，并取消实现 IBaseEnum 接口 ([bc00c9b](https://github.com/continew-org/continew-starter/commit/bc00c9bab0ed4508fd1dc0da8a76ef96739cce1d))
- 【api-doc】新增鉴权配置 ([7997267](https://github.com/continew-org/continew-starter/commit/7997267060b3e79f80dd73cec722bc295635a93b))

### 🐛 问题修复

- 【extension/crud】修复使用 @CrudRequestMapping 后自定义 API 不显示的问题 ([1adfddf](https://github.com/continew-org/continew-starter/commit/1adfddfa3b276e764b098512b2e9c75f007d13c1))

### 💥 破坏性变更

- 【extension/crud】调整通用查询注解所属模块 crud => mybatis-plus ([083bc7b](https://github.com/continew-org/continew-starter/commit/083bc7b38a861339ceb7a06acdd20ea64bc84990))
- 【extension/crud】调整校验工具类所属模块 crud => core ([083bc7b](https://github.com/continew-org/continew-starter/commit/083bc7b38a861339ceb7a06acdd20ea64bc84990))

## v1.0.0 (2023-12-02)

### ✨ 新特性

* 新增 continew-starter-core 核心模块（跨域、线程池等自动配置）
* 新增 continew-starter-file-excel 文件处理模块（Excel 相关配置）
* 新增 continew-starter-json-jackson JSON 模块（Jackson 自动配置）
* 新增 continew-starter-api-doc API 文档模块（Knife4j 自动配置）
* 新增 continew-starter-captcha-graphic 验证码模块（图形验证码）
* 新增 continew-starter-cache-redisson 缓存模块（Redisson 自动配置）
* 新增 continew-starter-data-mybatis-plus 数据访问模块（MyBatis Plus 自动配置）
* 新增 continew-starter-auth-satoken 认证模块（SaToken 自动配置）
* 新增 continew-starter-auth-justauth 认证模块（JustAuth 自动配置）
* 新增 continew-starter-messaging-mail 消息模块（邮件）
* 新增 continew-starter-messaging-sms 消息模块（短信）
* 新增 continew-starter-extension-crud CRUD 扩展模块
