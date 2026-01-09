## [v2.12.2](https://github.com/continew-org/continew-starter/compare/v2.12.1...v2.12.2) (2025-06-13)

### 🐛 问题修复

- 【extension/datapermission】修复构建本部门及以下数据权限表达式问题。 (Gitee#65@httpsjt) ([c0aa863](https://github.com/continew-org/continew-starter/commit/c0aa86327acac94b55e2f7c4fa193da4e38af986))

## [v2.12.1](https://github.com/continew-org/continew-starter/compare/v2.12.0...v2.12.1) (2025-06-09)

### ✨ 新特性

- 【messaging/websocket】新增发送消息给所有客户端方法 ([fa2cdf4](https://github.com/continew-org/continew-starter/commit/fa2cdf4f80bf4ca8656b658657398908894dfa40))
- 【messaging/websocket】新增批量发送消息方法 ([b543b2f](https://github.com/continew-org/continew-starter/commit/b543b2f94d09658a276e3a77d3091e1ec32360f9))
- 【core】ExceptionUtils 新增 exToThrow 方法 ([4a6b462](https://github.com/continew-org/continew-starter/commit/4a6b4624c2ed769bba6c50efd90592f7719247e5))
- 【json/jackson】Jackson 大数值序列化增加多模式支持  (Gitee#63@httpsjt) ([918a0ab](https://github.com/continew-org/continew-starter/commit/918a0abfda61bda8199256e4d4ecd5e20564569e)) ([73e2b16](https://github.com/continew-org/continew-starter/commit/73e2b169f7bc4a02140f963fd7b90037be8ff2b8))
- 【idempotent】新增默认幂等名称生成器 ([6b95083](https://github.com/continew-org/continew-starter/commit/6b95083c63de6a8eb7a7d08e6d537ec7afdb32f8))
- 【cache/redisson】新增 RedisQueueUtils 队列工具类 ([c08b57c](https://github.com/continew-org/continew-starter/commit/c08b57cb489e29b44ae99ec5bd725b72ec9a83a3))

### 💎 功能优化

- 调整代码风格 null == xx => xx == null（更符合大众风格） ([265d90f](https://github.com/continew-org/continew-starter/commit/265d90fa4ca0db8ed2bada22bd2881d364efde6e))
- 调整 Web 工具类到 core 模块 ([f83a901](https://github.com/continew-org/continew-starter/commit/f83a90162623208d3be75b03450d7ca29780c2b9))
- 【security/crypto】优化字段加解密相关代码 ([a4823dc](https://github.com/continew-org/continew-starter/commit/a4823dcb0bf211e26ccb8816928b5332b2bfe216))

### 🐛 问题修复

- 【web】添加 Servlet 工具类对 getOs 和 getBrowser 方法中User-Agent 为空或解析失败时的非空判断 (Gitee#61@beginner_b) ([abc005e](https://github.com/continew-org/continew-starter/commit/abc005e69022e7e08a580cd8027a5a3fb73ba929))
- 【core】修复 application/x-www-form-urlencoded 请求体数据无法在 Controller 层获取的问题 (Gitee#65@httpsjt) ([eb7dfd4](https://github.com/continew-org/continew-starter/commit/eb7dfd4ed706ed6b72364e316c4278364a4d4af4))

## [v2.12.0](https://github.com/continew-org/continew-starter/compare/v2.11.0...v2.12.0) (2025-05-17)

### ✨ 新特性

- 【security/password】添加密码编码器相关常量类 ([1b7f541](https://github.com/continew-org/continew-starter/commit/1b7f541e7d133cd431a9ca097bdac46ea85073be))
- 【license】新增 License 模块 (Gitee#51@aiming317、dom-w、httpsjt) ([da4c815](https://github.com/continew-org/continew-starter/commit/da4c8154bf6ddae7c0d0c6719efcc36537ed5983)) ([1ce5c02](https://github.com/continew-org/continew-starter/commit/1ce5c023cf8fe78849fba9fe0f7c0fcfac09c491)) ([7d97026](https://github.com/continew-org/continew-starter/commit/7d97026480b244319fa322c854a5e2d2552cc786)) ([06f5a0f](https://github.com/continew-org/continew-starter/commit/06f5a0f34680c93efe525b8102d24622b8b20893)) ([5e9a3f3](https://github.com/continew-org/continew-starter/commit/5e9a3f3e93ab55a6bc09828e124705e23543f72e))
- 【core】新增 JSON 格式字符串校验器 ([cf5ef36](https://github.com/continew-org/continew-starter/commit/cf5ef36af5179550e9c8cb75332497813488aee3))
- 【extension/crud】PageQuery 和 SortQuery 完善带参构造方法 ([70f8fc0](https://github.com/continew-org/continew-starter/commit/70f8fc01c0cd5636316705f9f9c425cda3f1d736))
- 【core】新增双斜杠 DOUBLE_SLASH 字符串常量 ([ef6621b](https://github.com/continew-org/continew-starter/commit/ef6621bf92e61def508d4c133c7c17c3c7327bf8))

### 💎 功能优化

- 【web】优化文件工具类下载文件逻辑，减少堆内存占用 (GitHub#12@BruceMaa) ([4e7cd51](https://github.com/continew-org/continew-starter/commit/4e7cd5186850b5229233a744bc06b02188849029))
- 统一请求参数、响应参数注释 ([934a5f7](https://github.com/continew-org/continew-starter/commit/934a5f7b6d90e64ab070ff9067ff9a9b73e46f11))
- 【log】抽取 isRecord 方法方便复用，移除已过期的 timeTtl ([49bd289](https://github.com/continew-org/continew-starter/commit/49bd289e29c6ff3b2f1553ea9bf787ade65df810))
- 调整代码风格 null != xx => xx != null（更符合大众风格） ([ae7a267](https://github.com/continew-org/continew-starter/commit/ae7a267c1d5b4b0fafc54e08e915383b49e47b01))

### 🐛 问题修复

- 【cache/redisson】兼容redis没配置密码时出现redisson实例化失败的问题 (Gitee#54@muxuanya) ([335dc35](https://github.com/continew-org/continew-starter/commit/335dc35e2be11f7ebfa45e5334eb365f4ee229dc))
- 【idempotent】修复幂等处理切面，未设置超时时间的问题 (Gitee#56@sheng_chao_111) ([5129fea](https://github.com/continew-org/continew-starter/commit/5129fea34dfedea9d5d6df50dd4e65e7bec7e651))
- 【file/excel】修复导出时无法正确捕捉异常的问题 (Gitee#59@chengangi) ([d99a6a2](https://github.com/continew-org/continew-starter/commit/d99a6a2c2d7b8e513ad8cbb8b7deef6a6d4be04a))
- 【web】修复默认 Response 类 msg 传递污染的问题 ([3bbd04a](https://github.com/continew-org/continew-starter/commit/3bbd04add2946a9619e8edbd94cb9bbb23c688a8))
- 【web】修复 /file/ 注册资源映射时被解析为 /file//** 的问题 ([35e2cdc](https://github.com/continew-org/continew-starter/commit/35e2cdc3bf2c2137f86e72612b3572be148b5823))

### 📦 依赖升级

- spring-boot 3.3.9 => 3.3.11 ([62334d8](https://github.com/continew-org/continew-starter/commit/62334d882c174fd2de5fdeb081407356984d26bd))
- redisson 3.45.0 => 3.46.0
- jetcache 2.2.7 => 2.7.8
- cosid 2.11.0 => 2.12.3
- sa-token 1.41.0 => 1.42.0
- mybatis-flex 1.10.8 => 1.10.9
- aws-s3 1.12.782 => 1.12.783
- s3 2.30.35 => 2.31.35
- s3-crt 0.36.1 => 0.38.1
- hutool 5.8.36 => 5.8.37

## [v2.11.0](https://github.com/continew-org/continew-starter/compare/v2.10.0...v2.11.0) (2025-04-13)

### ✨ 新特性

- 【web】添加 Undertow 自定义配置和默认配置，默认禁止三个不安全的 HTTP 方法（如 CONNECT、TRACE、TRACK） (Gitee#50@httpsjt) ([49b1b6a](https://github.com/continew-org/continew-starter/commit/49b1b6a69073095859c7e692f4e5908144eb4ae6)) ([08068cb](https://github.com/continew-org/continew-starter/commit/08068cb9f7ad7e56ec4df662e2c1e8bf04cb45a1))
- 新增 continew-starter-bom 模块，用于集中管理所有子模块版本 ([5822d07](https://github.com/continew-org/continew-starter/commit/5822d073fb28c7178409010e6e4f9e78c1ae2d5a))
- 【cache/redisson】添加缓存键前缀支持 ([615dfdd](https://github.com/continew-org/continew-starter/commit/615dfdd03fb0bc8989b8a788c9babd30709ac643))

### 💎 功能优化

- 【dependencies】采取 bom 方式来管理 JetCache 依赖 (Gitee#44@jiang4yu) ([e2d8f45](https://github.com/continew-org/continew-starter/commit/e2d8f45206a55e333c26a48c501efbb82c89beea)) ([f662b74](https://github.com/continew-org/continew-starter/commit/f662b740610da3e1ff4c0fadf2e5b2a188b06d73)) ([3e0dd83](https://github.com/continew-org/continew-starter/commit/3e0dd83e2664e57d61c37e4ea7afa618c322b984))
- 替换 aspectjweaver 依赖为 Spring Boot Starter AOP ([ae2b898](https://github.com/continew-org/continew-starter/commit/ae2b898e57ca8e418289a2974c92447ec191e15f))
- 【dependencies】调整 sa-token 版本锁定为 bom 方式（PR by iang4yu） ([e242568](https://github.com/continew-org/continew-starter/commit/e24256818d716c4c2bbc50d6e7bd0df394bbbd4f))
- 【log】访问日志过滤资源路径 (Gitee#47@dom-w) ([a6a44cd](https://github.com/continew-org/continew-starter/commit/a6a44cd46131d41f8626fe67f6ad9e4d70f1d46c))
- 【log】重构请求和响应信息获取 (Gitee#47@dom-w) ([ca2c886](https://github.com/continew-org/continew-starter/commit/ca2c88651ff84b165ed9c9389fefb346d2be92ab))
- 【log】修复访问日志 JSON 数组打印 (Gitee#47@dom-w) ([199a83f](https://github.com/continew-org/continew-starter/commit/199a83fbea015415484a84b5e7cb535bf804e0bc))
- 删除多余依赖，格式化代码 (Gitee#47@dom-w) ([7e8a15a](https://github.com/continew-org/continew-starter/commit/7e8a15ae8ace650ca5ccf1b807a3149fc2d1e352))
- 优化部分代码及方法命名，移除 SpringWebUtils 部分重复方法 ([f2ba10b](https://github.com/continew-org/continew-starter/commit/f2ba10beae9d17fad7fa260924ede5cd8ca0cbe4))
- 调整  Spring Boot 等依赖为 bom 引入 (Gitee#49@jiang4yu) ([1d4f3a3](https://github.com/continew-org/continew-starter/commit/1d4f3a33b9ea6aa84c096b4d97cbb35220c13d86))
- 【auth/satoken】使用 satoken 官方插件替换 Redisson 缓存 DAO 支持 ([a871048](https://github.com/continew-org/continew-starter/commit/a87104830fa1660035718c4721aa5862433dad7f))
- 【extension/crud】重构删除接口，以解决批量删除时 URL 中 ID 列表过长问题 ([45da758](https://github.com/continew-org/continew-starter/commit/45da758dee5446f19a230c47a1f5135a86bbff63))
- 统一配置启用属性描述 ([efaef9d](https://github.com/continew-org/continew-starter/commit/efaef9d7e6a3584e92748e33bf333a2166a3b355))
- 【extension/crud】将 @DictField 注解重命名为 @DictModel，用于更清晰地表示字典结构映射 ([8766f11](https://github.com/continew-org/continew-starter/commit/8766f11eb2ea3c2cedc5754be4d8969298f95cb3))
- 【log】优化日志拦截器配置 ([0463c74](https://github.com/continew-org/continew-starter/commit/0463c74aa4737db69798da00c4f1975401a6659d))

### 🐛 问题修复

- 【data/mp】修改构建本部门及以下数据权限表达式 以支持PostgreSQL (Gitee#48@httpsjt) ([5a2621a](https://github.com/continew-org/continew-starter/commit/5a2621a030b8e4f437e2c37fc4021ee661f497c7))
- 解决部分传递依赖漏洞问题 ([1c65191](https://github.com/continew-org/continew-starter/commit/1c65191b8ab8023fb5990bd532397f3571406420))

### 📦 依赖升级

- sa-token 1.40.0 => 1.41.0 ([a871048](https://github.com/continew-org/continew-starter/commit/a87104830fa1660035718c4721aa5862433dad7f))

## [v2.10.0](https://github.com/continew-org/continew-starter/compare/v2.9.0...v2.10.0) (2025-03-26)

### ✨ 新特性

- 【idempotent】新增幂等模块 (Gitee#41@aiming317) ([199df87](https://github.com/continew-org/continew-starter/commit/199df874e54207d9b05230dcd2ec83be0e6d3f06)) ([27a71cf](https://github.com/continew-org/continew-starter/commit/27a71cf07675315405908a5befd25ad6e5c7471c)) ([f50b511](https://github.com/continew-org/continew-starter/commit/f50b51151391e36e49f31a0e553d8c86f1827821))
- 【cache/redisson】添加条件性缓存设置方法 setIfAbsent、setIfExists ([b199b65](https://github.com/continew-org/continew-starter/commit/b199b651ecf8a2de6cccafa4efc98c7d65446ebd))
- 【core】添加手机号校验注解并优化枚举校验提示信息 ([a6fb65f](https://github.com/continew-org/continew-starter/commit/a6fb65f97e22ea0e7eec7d9c523e3c550b1d73d0))
- 【web】新增日期类型转换器 ([d9ac276](https://github.com/continew-org/continew-starter/commit/d9ac2764aa78e83a11ee5440155a8cd7bf1cb8c8))
- 【security/xss】新增 XSS 过滤模块（原 web 模块内组件） ([b5bfe5c](https://github.com/continew-org/continew-starter/commit/b5bfe5c6813323d45cd5879a2e0f9bbd88d657e0))
- 【trace】新增链路追踪模块（原 web 模块内组件） ([85285e5](https://github.com/continew-org/continew-starter/commit/85285e56a83324d9a6542531dbdf3e82f8af0301))

### 💎 功能优化

- 【extension/crud】将详情方法命名还原为 get ([591a44d](https://github.com/continew-org/continew-starter/commit/591a44d861151b89f1f748d18092b546bb0935e0))
- 【extension/crud】将新增操作由 ADD 改为创建操作 CREATE ([1903520](https://github.com/continew-org/continew-starter/commit/19035204336f0c9d462e75e89561514aa1414f27))
- 【ratelimiter】将限流相关代码从 security 模块中分离，创建独立的 ratelimiter 模块 ([2b3de0c](https://github.com/continew-org/continew-starter/commit/2b3de0c67e1e6f4b29fed4a732a48e5512dad4ac))
- 优化部分错误提示信息和代码注释 ([c9c7c34](https://github.com/continew-org/continew-starter/commit/c9c7c345062a126e802f5d92d06710f503e8f733))
- 【log】重构访问日志 (Gitee#42@dom-w) ([da5e162](https://github.com/continew-org/continew-starter/commit/da5e162a2ab5c4a428bcdda4c8ea94d52722b7ad)) ([4c38592](https://github.com/continew-org/continew-starter/commit/4c385927b4e57402dc06e7713388984ead1186b3)) ([4c38592](https://github.com/continew-org/continew-starter/commit/4c385927b4e57402dc06e7713388984ead1186b3)) ([1613374](https://github.com/continew-org/continew-starter/commit/1613374fcca67381e9fcf6b3677527d66f6ea3db))

### 🐛 问题修复

- 【web】修复开启 i18n 后访问接口报错的问题 ([0d7f777](https://github.com/continew-org/continew-starter/commit/0d7f777fd56e08ef3842521285bb8c379e408874))
- 【web】优化默认全局响应实体 R ，为 status 字段添加默认值 DefaultResponseStatus (Gitee#39@zs3zs) ([e7d99e6](https://github.com/continew-org/continew-starter/commit/e7d99e65aa2a22154a81ba087dbc11a6aee9598f))
- 【cache/redisson】修复嵌套属性未添加注解导致无法注入的问题 ([92f5ea7](https://github.com/continew-org/continew-starter/commit/92f5ea799e9059f8c2e5bef37f0beb4074b894db))

### 📦 依赖升级

- Spring Boot 3.2.12 => 3.3.9 ([974efa3](https://github.com/continew-org/continew-starter/commit/974efa368a983548ac87f0fa4ee4e181a6383668))
- 新增 Spring Cloud 2023.0.5
- Redisson 3.41.0 => 3.45.0
- CosId 2.10.1 => 2.11.0
- sa-token 1.39.0 => 1.40.0
- snail-job 1.2.0 => 1.4.0
- sms4j 3.3.3 => 3.3.4
- nashorn 15.5 => 15.6
- s3 2.29.23 => 2.30.35
- s3-crt 0.33.5 => 0.36.1
- ip2region 3.2.12 => 3.3.6
- hutool 5.8.34 => 5.8.36
- mybatis-flex 1.10.3 => 1.10.8
- snakeyaml 2.3 => 2.4
- flatten 1.6.0 => 1.7.0
- spotless 2.43.0 => 2.44.3

## [v2.9.0](https://github.com/continew-org/continew-starter/compare/v2.8.3...v2.9.0) (2025-02-14)

### ✨ 新特性

- 【storage】新增 S3 存储模块，重构本地存储 (Gitee#37@dom-w) ([e11c7a0](https://github.com/continew-org/continew-starter/commit/e11c7a0d8c2bfb3d6532312eec6a10a098bb3f4c)) ([a040473](https://github.com/continew-org/continew-starter/commit/a040473746932e9003868a23b44083b5385180ee))
- 【security/sensitivewords】添加敏感词模块 (GitHub#11@luoqiz) ([82f1835](https://github.com/continew-org/continew-starter/commit/82f18356df9ef170d65801f8f3e5bac25508caa6)) ([bd8b189](https://github.com/continew-org/continew-starter/commit/bd8b1899c7f18dcdcfca18ed07d36ad6afe1eb55))
- 【data/mp】QueryWrapperHelper 新增 sort 方法 ([8cab7d1](https://github.com/continew-org/continew-starter/commit/8cab7d1e2b441c321f28d4cbc26ccd27b29887b3))

### 🐛 问题修复

- 【api-doc】修复全局自定义配置无法被覆盖的问题 ([f386fd7](https://github.com/continew-org/continew-starter/commit/f386fd7d95347246f5282e2c59e63d8b91c70ddd))

## [v2.8.3](https://github.com/continew-org/continew-starter/compare/v2.8.2...v2.8.3) (2025-01-16)

### 💎 功能优化

- 【log】调整所属模块、日志描述默认提示，不再直接抛出异常 ([326dd76](https://github.com/continew-org/continew-starter/commit/326dd76c34476141c39add5348da052bdb8c27cd))
- 【extension/crud】移除 BaseDO、BaseCreateDO、BaseUpdateDO 等（已移动到 Admin 项目内） ([6e621bc](https://github.com/continew-org/continew-starter/commit/6e621bc4597996c8f1f65c542f5faa922b95a900))

### 🐛 问题修复

- 【extension/crud】修复查询条件校验无效的问题 ([d771e12](https://github.com/continew-org/continew-starter/commit/d771e128399851fa78f1041fa4ffcd6af3332fcd))

## [v2.8.2](https://github.com/continew-org/continew-starter/compare/v2.8.1...v2.8.2) (2025-01-09)

### ✨ 新特性

- 【core】SpringUtils 新增获取代理对象方法 ([5f68227](https://github.com/continew-org/continew-starter/commit/5f6822742fd0f032bcc351155f0b966d24b05346))

### 💎 功能优化

- 【extension/crud】移除 CommonUserService、ContainerPool（已移动到 Admin 项目内） ([0b342d5](https://github.com/continew-org/continew-starter/commit/0b342d5c73e95b809337b939b4e1e957374bad85))

### 🐛 问题修复

- 【log】修复日志记录时所属模块和描述取值优先级失效的问题 ([4fe067a](https://github.com/continew-org/continew-starter/commit/4fe067a889f00617f03caf7ae3598466560dce33))

### 📦 依赖升级

- graceful-response 5.0.4-boot3 => 5.0.5-boot3（修复父类参数校验异常） ([aa463df](https://github.com/continew-org/continew-starter/commit/aa463dff37b658d1cb2a69e68f54790e03c4103d))

## [v2.8.1](https://github.com/continew-org/continew-starter/compare/v2.8.0...v2.8.1) (2025-01-06)

### ✨ 新特性

- 【core】BaseEnum 新增 getByValue、getByDescription、isValidValue 方法 ([279d72b](https://github.com/continew-org/continew-starter/commit/279d72b7242bf996f9b88d38ed0ea7aa0a0d1c46))

### 💎 功能优化

- 【extension/crud】移除 BaseResp、BaseDetailResp（已移动到 Admin 项目内） ([eb2cac5](https://github.com/continew-org/continew-starter/commit/eb2cac54f75b2850f2957b32190d12e63377c185))
- 【log】优化日志处理器解析 description、module 方法 ([a6c9d33](https://github.com/continew-org/continew-starter/commit/a6c9d33024ea70bb3dbe11981cbc9a3f9027bcd2))
- 解决 Sonar 问题，替换部分过期 API ([80c0700](https://github.com/continew-org/continew-starter/commit/80c070093498abb8dff5529d177e1e2519577bf0))

### 🐛 问题修复

- 【file/excel】优化 BaseEnum 转换器 (GitHub#10@Solution-Lin) ([b9779e8](https://github.com/continew-org/continew-starter/commit/b9779e894464ec534bebdd230a7239b6d1964ddb))

## [v2.8.0](https://github.com/continew-org/continew-starter/compare/v2.7.5...v2.8.0) (2024-12-25)

### ✨ 新特性

- 【log/aop】新增 log-aop 组件模块（基于 AOP 实现日志记录） (Gitee#36@dom-w) ([7c3f15a](https://github.com/continew-org/continew-starter/commit/7c3f15a6f647afabb061e560ad3335d47806d33f)) ([265c669](https://github.com/continew-org/continew-starter/commit/265c669eda7599172cc189c96428629423158e86))

### 💎 功能优化

- 【log】新增 LogHandler 提升日志模块的复用性 ([0d33452](https://github.com/continew-org/continew-starter/commit/0d334523e9d18b548740af6583521b47b8171446)) ([c5cb203](https://github.com/continew-org/continew-starter/commit/c5cb203532ea89b497121246f11ad858f1c3ac79)) ([7ff26c4](https://github.com/continew-org/continew-starter/commit/7ff26c45962e916370aeaeaa547974dbf727fdb4))
- 【extension/tenant】多租户组件适配动态隔离级别 (GitHub#8@xtanyu) ([c089df6](https://github.com/continew-org/continew-starter/commit/c089df66cf226aa8062dc7ac2c82fb0111cfc5c0)) ([613599f](https://github.com/continew-org/continew-starter/commit/613599f92199e0cde11896d41c2d090bfdc46dd3)) ([88d1102](https://github.com/continew-org/continew-starter/commit/88d11027dd18eab5a0a71f85b135a1ddc0f3942f)) ([f7ed2bb](https://github.com/continew-org/continew-starter/commit/f7ed2bbfb017667253ec50341a753b89d65562bb)) ([25f499d](https://github.com/continew-org/continew-starter/commit/25f499de7eb59b53548d9d3f6826358b2fd0c40b))

### 🐛 问题修复

- 【extension/crud】修复 PageResp 手动分页计算错误 ([dc407a8](https://github.com/continew-org/continew-starter/commit/dc407a82cca016db6896104804eef9b660d9d5a1))

### 📦 依赖升级

- Spring Boot 3.2.7 => 3.2.10 ([5ff9391](https://github.com/continew-org/continew-starter/commit/5ff93914854098ac05bf24559336e2155b8f1503))
- Spring Boot 3.2.10 => 3.2.12
- SnailJob 1.1.2 => 1.2.0
- JustAuth 1.16.6 => 1.16.7
- MyBatis Flex 1.9.7 => 1.10.3
- JetCache 2.7.6 => 2.7.7
- Redisson 3.36.0 => 3.41.0
- CosID 2.9.9 => 2.10.1
- nashorn-core 15.4 => 15.5
- aws-s3 1.12.771 => 1.12.780
- graceful-response 5.0.0-boot3 => 5.0.4-boot3
- ip2region 3.2.6 => 3.2.12
- Hutool 5.8.32 => 5.8.34

## [v2.7.5](https://github.com/continew-org/continew-starter/compare/v2.7.4...v2.7.5) (2024-12-06)

### 💎 功能优化

- 【extension/crud】重构 BaseController 内权限校验，BaseController => AbstractBaseController ([3edf79c](https://github.com/continew-org/continew-starter/commit/3edf79cf3bdba91010776c95902414fa7481c39e)) ([15f8706](https://github.com/continew-org/continew-starter/commit/15f87068c65e4afd83ee7020bde2707870012fb9)) ([16da470](https://github.com/continew-org/continew-starter/commit/16da470008d44ebf37cbe9e292ac8db8fc04ceb5)) ([2a5ace0](https://github.com/continew-org/continew-starter/commit/2a5ace003329422589af834431ac2bd8fa30ac28))
- 【extension/crud】调整 BaseController、BaseService 到 crud-core 模块 ([3a0c3e0](https://github.com/continew-org/continew-starter/commit/3a0c3e02b03837789bfc433a335b062ec5dab511))
- 【extension/crud】优化部分代码，ValidateGroup => CrudValidationGroup ([f2a30e8](https://github.com/continew-org/continew-starter/commit/f2a30e8b74b828644970be0b05920a32d6eb514a)) ([6a6c559](https://github.com/continew-org/continew-starter/commit/6a6c559d2f53f25888259a7e5d020281408aaa9f))
- 【data】使用 Hutool 工具方法替换反射 API，以解决扫描问题 ([93ab4e5](https://github.com/continew-org/continew-starter/commit/93ab4e50cc1957ab772b31c9c433f9fc3d29da33))

### 🐛 问题修复

- 【data/mp】修复 Query 范围查询数组类型数据解析错误 ([6b3bc83](https://github.com/continew-org/continew-starter/commit/6b3bc832de25acdb2be418ad7626e9b6e234adde))

## [v2.7.4](https://github.com/continew-org/continew-starter/compare/v2.7.3...v2.7.4) (2024-11-18)

### 💎 功能优化

- 【core】增加 + 号字符串/字符常量 ([464b87c](https://github.com/continew-org/continew-starter/commit/464b87c9c7789bc142538bc146ecfe4358c12a50))
- 【core】移除多余的校验工具类 ([fd9d2bb](https://github.com/continew-org/continew-starter/commit/fd9d2bb370caef4e9f9e3874e113c381ab4e5eb9))

### 🐛 问题修复

- 【extension/crud】修复新增响应结构 BaseIdResp 无法被继承问题 ([27ce092](https://github.com/continew-org/continew-starter/commit/27ce092b796a66f1c59d8715cf5648edab9efa65))
- 【json/jackson】修复 BaseEnum 反序列化数据类型仅支持数值的问题 ([b11013e](https://github.com/continew-org/continew-starter/commit/b11013ee80cb00022890d950ff7f666909de2082))

## [v2.7.3](https://github.com/continew-org/continew-starter/compare/v2.7.2...v2.7.3) (2024-11-15)

### ✨ 新特性

- 【cache/redisson】RedisUtils 新增 ZSet 相关方法 ([56edcee](https://github.com/continew-org/continew-starter/commit/56edceec7e7c61bbd06a1995c2351441302ac969))
- 【core】新增枚举校验器 EnumValue ([c7bee00](https://github.com/continew-org/continew-starter/commit/c7bee0033ef794784b9c9fd39f61a245abff0c62))

### 💎 功能优化

- 【extension/crud】查询详情命名调整，GET -> DETAIL，增加详情权限校验 ([4b77d5c](https://github.com/continew-org/continew-starter/commit/4b77d5cb3ff93e7d8d207196948352294c6cdcc6))
- 【core】拆分字符串常量和字符常量 ([2e9079a](https://github.com/continew-org/continew-starter/commit/2e9079a909db8df57ed7de49c95d9daeb9616f4a))

### 🐛 问题修复

- 【data/mp】修复普通枚举类型处理错误 ([efb84c9](https://github.com/continew-org/continew-starter/commit/efb84c936f1012c3ac4b6264599f6fb1f5ae5f97))

### 📦 依赖升级

- CosID 2.9.8 => 2.9.9 ([e3433be](https://github.com/continew-org/continew-starter/commit/e3433bed01e9bccc1179c04acc82df843434d9af))

## [v2.7.2](https://github.com/continew-org/continew-starter/compare/v2.7.1...v2.7.2) (2024-11-12)

### ✨ 新特性

- 【extension/crud】支持树结构全局配置 ([5891c4a](https://github.com/continew-org/continew-starter/commit/5891c4aa61b14ba11a387a478fb3616dfc52217c))
- 【extension/crud】查询字典列表新增支持 extraKeys 额外信息字段 ([9b7ea33](https://github.com/continew-org/continew-starter/commit/9b7ea33c0b6714e2ea631aa26f0650e78857079a)) ([a8c6ea3](https://github.com/continew-org/continew-starter/commit/a8c6ea30797811d885f294f28eb95afb935ad7b4))
- 【cache/redisson】RedisUtils 新增上锁、释放锁方法 ([04498ff](https://github.com/continew-org/continew-starter/commit/04498ffe56b062bce1200292b23d2c31341771e6))

### 💎 功能优化

- 【extension/crud】移除 TreeUtils ([f4b2310](https://github.com/continew-org/continew-starter/commit/f4b23102a9a31b2120f40a8288bc0aedc36e11b4))
- 【data/mp】移除冗余的数据库类型判空处理 ([bd60411](https://github.com/continew-org/continew-starter/commit/bd60411f3e4fa87c26e492df96fbfb088ea3ce85))
- 【core】重构 IP 工具类获取归属地的返回格式（更方便数据处理） ([e9b9d8b](https://github.com/continew-org/continew-starter/commit/e9b9d8b82e7e28be82c9ed518582d88f507cfac2))
- 【data】Query 范围查询支持数组数据 ([673e586](https://github.com/continew-org/continew-starter/commit/673e586aafc8578f0c7ab063ca9df9b1265f88d5))
- 【data】重构 MetaUtils 获取表信息方法 ([1ce5eb3](https://github.com/continew-org/continew-starter/commit/1ce5eb3b734b13ccd47e3848117daf3c2d7d0afa))

## [v2.7.0](https://github.com/continew-org/continew-starter/compare/v2.6.0...v2.7.0) (2024-09-28)

### ✨ 新特性

- 【data/mp】新增乐观锁插件启用配置（默认关闭） ([08ef09c](https://github.com/continew-org/continew-starter/commit/08ef09c9b594dca75b39e36add38998826b234bf))
- 【extension/tenant】新增 continew-starter-extension-tenant 多租户模块 ([1a97a1b](https://github.com/continew-org/continew-starter/commit/1a97a1b709ee0c04300fd39758506fd479da0713)) ([f843791](https://github.com/continew-org/continew-starter/commit/f8437918de342ee45d15df30c20de5e8d3b18379))
- 【extension/datapermission】新增数据权限模块（原 data/mp 中数据权限移除） ([7666d56](https://github.com/continew-org/continew-starter/commit/7666d56019bb309dca004d43b0717f6bb0e56c8f))

### 💎 功能优化

- 【data/mp】移除多数据源依赖，如需使用可手动引入 ([06d3a6c](https://github.com/continew-org/continew-starter/commit/06d3a6ca412b0bdeba9c0e460db6a0b05215b6b3))
- 完善 ConditionalOnProperty 配置 ([0cede6b](https://github.com/continew-org/continew-starter/commit/0cede6bf9fc89e0c5009e9721b5cea2cf73b890c))
- 优化部分代码写法 ([1fc80cd](https://github.com/continew-org/continew-starter/commit/1fc80cda9eb5b377b30d834692dff58d8f93053b))
- 优化代码格式 ([46773df](https://github.com/continew-org/continew-starter/commit/46773df9dd2dc473459d58fc17f650d3da260545))
- 【data/mp】移除 QueryIgnore 的无用属性 ([0c334da](https://github.com/continew-org/continew-starter/commit/0c334dadcce9d74301dbcc3c336dc28ffc4cf62e))
- 【file/excel】导出方法增加排除字段参数 ([3535ac6](https://github.com/continew-org/continew-starter/commit/3535ac64f79c7c3d8e03d8ed2a996ebdfab1ff92))
- 统一部分命名风格 ([f858395](https://github.com/continew-org/continew-starter/commit/f85839559ad7002dffbe3c5999a75e801ef9c4d1))
- 优化部分依赖传递范围 ([cd69b2a](https://github.com/continew-org/continew-starter/commit/cd69b2adb67cf17b12619f06b8e81492cbb41c26))

### 🐛 问题修复

- 【log/interceptor】修复 continew-starter.log.exclude-patterns 配置不生效的问题 ([ca1b92c](https://github.com/continew-org/continew-starter/commit/ca1b92cde3cf8f9d9ee0b7420f5b13f200e80781))
- 【log/interceptor】修复全局配置和局部配置包含请求、响应体冲突 ([be4dec5](https://github.com/continew-org/continew-starter/commit/be4dec5a3039625e62d346dbb148206b602af6aa))

### 📦 依赖升级

- Spring Boot 3.2.7 => 3.2.10 ([802dcb5](https://github.com/continew-org/continew-starter/commit/802dcb5735562e911e3a51741cfcf17dbe59a89e))
- MyBatis Plus 3.5.7 => 3.5.8
- Redisson 3.35.0 => 3.36.0
- CosID 2.9.6 => 2.9.8
- SMS4J 3.2.1 => 3.3.3
- X File Storage 2.2.0 => 2.2.1

## [v2.6.0](https://github.com/continew-org/continew-starter/compare/v2.5.2...v2.6.0) (2024-09-06)

### ✨ 新特性

- 【web】新增 isMatch 路径是否匹配方法 ([e55eb17](https://github.com/continew-org/continew-starter/commit/e55eb17d64d7b42c6e64b18a645cda9558f08d58))
- 【log】不记录日志也支持开启打印访问日志 ([16b6e9b](https://github.com/continew-org/continew-starter/commit/16b6e9b466578d935ab9a8a85a5eac4d09025dee))

### 💎 功能优化

- 【data】移除 DataPermission 注解的 value 属性 ([d3fa00d](https://github.com/continew-org/continew-starter/commit/d3fa00d14ce95ab1bedaebbce8304e29df5da8fd))
- 【data】mybatis-plus => mp，mybatis-flex => mf ([5e0eea2](https://github.com/continew-org/continew-starter/commit/5e0eea2ed801b526da9f65b8cb2942b3b7b050ef))
- 【web】提升接口文档响应类型优化扩展性 ([784a56f](https://github.com/continew-org/continew-starter/commit/784a56fd4f85ae170b8a56dd0a64a33a7bff98bc))
- 【web】链路追踪配置属性响应头名称 => 链路 ID 名称 ([260f484](https://github.com/continew-org/continew-starter/commit/260f484af98d112927edec4316c0375e628dd3ba))
- 【log】优化接口耗时相关时间类型使用 ([4caf0a0](https://github.com/continew-org/continew-starter/commit/4caf0a0db69779a5ea409ec7e01c4044817afc94))

### 🐛 问题修复

- 【log】修复日志全局 includes 配置会被局部修改的问题 ([eac5c1f](https://github.com/continew-org/continew-starter/commit/eac5c1fa79f7f91217b0525a07e0f1314f8efe24))
- 【data】还原 SQL 函数接口 ([9e5f33b](https://github.com/continew-org/continew-starter/commit/9e5f33b2c9b804741f9b77eab5b67ab3c4a8b220))
- 【crypto】修复由于升级 mybatis plus 引发的更新场景加密失效问题  ([e9b81f9](https://github.com/continew-org/continew-starter/commit/e9b81f946603e2f9e44b50471102b2551b9d50ac)) ([9fdbfdf](https://github.com/continew-org/continew-starter/commit/9fdbfdf8bb6719d3d044c71a2a8ceab85ccef8f1))

### 📦 依赖升级

- Graceful Response 4.0.1-boot3 => 5.0.0-boot3 ([2208dbd](https://github.com/continew-org/continew-starter/commit/2208dbd0282964aab76b02e811d6689ba69d75fd))
- Snail Job 1.1.0 => 1.1.2
- Sa Token 1.38.0 => 1.39.0
- MyBatis Flex 1.9.3 => 1.9.7
- Redisson 3.32.0 => 3.35.0
- Cos ID 2.9.1 => 2.9.6
- Hutool 5.8.29 => 5.8.32
- aws-java-sdk-s3 1.12.761 => 1.12.771
- snakeyaml 2.2 => 2.3

## [v2.5.2](https://github.com/continew-org/continew-starter/compare/v2.5.1...v2.5.2) (2024-08-14)

### 💎 功能优化

- 【api-doc】重构接口文档枚举展示处理 ([bf51837](https://github.com/continew-org/continew-starter/commit/bf51837991746c5576d7baffcc2296fe8ca91586)) ([4c4f98a](https://github.com/continew-org/continew-starter/commit/4c4f98a86e6d10b2b52814406bd0dc3d248c8486))
- 【web】针对最新响应风格增加全局响应格式 ([bf51837](https://github.com/continew-org/continew-starter/commit/bf51837991746c5576d7baffcc2296fe8ca91586)) ([4c4f98a](https://github.com/continew-org/continew-starter/commit/4c4f98a86e6d10b2b52814406bd0dc3d248c8486))

### 🐛 问题修复

- 【extension/crud】重构排序字段处理，预防 SQL 注入问题 ([c31fa75](https://github.com/continew-org/continew-starter/commit/c31fa753b6d9753d72a2e28bf3184981ed848ac2)) ([22ebdfe](https://github.com/continew-org/continew-starter/commit/22ebdfeb9f30a8832825424a05343acfaf31643b))
- 【security/crypto】修复 updateById 修改未正确加密的问题 ([b0a2a8c](https://github.com/continew-org/continew-starter/commit/b0a2a8c927171795e3ed89a820caf829f76c80ee))

## [v2.5.1](https://github.com/continew-org/continew-starter/compare/v2.5.0...v2.5.1) (2024-08-12)

### 🐛 问题修复

- 【data】移除 SQL 函数接口中的 SQL 拼接 ([6693cd4](https://github.com/continew-org/continew-starter/commit/6693cd49b93244b42dfadd4f4be28e526d764425))

## [v2.5.0](https://github.com/continew-org/continew-starter/compare/v2.4.0...v2.5.0) (2024-08-07)

### ✨ 新特性

- 【web】重构全局响应处理方案 ([0b41f2d](https://github.com/continew-org/continew-starter/commit/0b41f2d10c5dfb309585b36bde7008cdff6c912a))
- 【web】FileUploadUtils 新增下载重载方法 ([be3a121](https://github.com/continew-org/continew-starter/commit/be3a121c0901e7c31bd2d30ac3befa74197c8c35))

### 🐛 问题修复

- 【log】仅支持获取 JSON 结构响应体 ([6e76269](https://github.com/continew-org/continew-starter/commit/6e76269bb6f335fa655fab5a5bee85824c1ca9e3))

### 💎 功能优化

- 【web】BaseEnumConverterAutoConfiguration => WebMvcAutoConfiguration ([9ec2e6b](https://github.com/continew-org/continew-starter/commit/9ec2e6b98137ed54fbba25d4e895d28cc39e7d93))
- 【log】log-httptrace-pro => log-interceptor ([31c3162](https://github.com/continew-org/continew-starter/commit/31c3162563589b71f7bc09797d39abb0177bee27))

## [v2.4.0](https://github.com/continew-org/continew-starter/compare/v2.3.0...v2.4.0) (2024-07-31)

### ✨ 新特性

- 【json/jackson】新增枚举接口序列化及反序列化配置 ([32935fa](https://github.com/continew-org/continew-starter/commit/32935fa4fafad0a405153f408b2c16c2389b3aa3))
- 【api-doc】增加对 BaseEnum 枚举接口的详细展示 (Gitee#28) ([ebc73a9](https://github.com/continew-org/continew-starter/commit/ebc73a94e8824aa3233492ab4c013b5c70a71ee8))
- 【web】新增 BaseEnum 枚举接口参数转换器 ([bed954c](https://github.com/continew-org/continew-starter/commit/bed954ca94b8e2edd897c25d06475da079d0720a))
- 【web】SpringWebUtils 新增 match 路径匹配方法 ([702dcca](https://github.com/continew-org/continew-starter/commit/702dcca7012a4ac3d779396f12ef9eeb8371f7cb))

### 🐛 问题修复

- EasyExcel 4.0.1 => 3.3.4，暂时回退版本，解决版本冲突问题 ([1479c8d](https://github.com/continew-org/continew-starter/commit/1479c8d9396e92decfbaefd2d71145d2953674ba))

### 💎 功能优化

- 代码编译增加 -parameters 参数 ([c1ebc46](https://github.com/continew-org/continew-starter/commit/c1ebc4621c7c44334cf172708cb061f0fecb5a05))
- 【data/mybatis-plus】移动枚举接口到 core 模块，和 MP IEnum 枚举接口解耦 ([b27fbd4](https://github.com/continew-org/continew-starter/commit/b27fbd41eb9ec6020f3403a5e3beaef6e2a8fb62))
- 【extension/crud】移动 ExcelBaseEnumConverter 到 excel 模块 ([730df52](https://github.com/continew-org/continew-starter/commit/730df527970718aa1008d03a35b53064b20ef1ce))
- 【log】新增 excludePatterns 放行路由配置 ([bd07f9b](https://github.com/continew-org/continew-starter/commit/bd07f9b41f5eabe380c91c18877886fa97e1bc20))

## [v2.3.0](https://github.com/continew-org/continew-starter/compare/v2.2.0...v2.3.0) (2024-07-18)

### ✨ 新特性

- 【core】新增 JSR 303 校验器自动配置（从 web 模块迁移） ([6809600](https://github.com/continew-org/continew-starter/commit/6809600858ed597567f78581187f6d88a2ea899e))
- 新增 Snail Job 依赖版本 ([d31d8d2](https://github.com/continew-org/continew-starter/commit/d31d8d209a66884d046763bb8497b2c58cf88506))

### 🐛 问题修复

- 【extension/crud】修复 DictField 映射错误 ([65cfe91](https://github.com/continew-org/continew-starter/commit/65cfe917709320edd9db2ae55390afe64077e3d3))
- 【extension/crud】修复 Name for argument of type [java.lang.Long] not specified, and parameter name information not available via reflection. 错误 ([c17668c](https://github.com/continew-org/continew-starter/commit/c17668c2d1a9440dd0260fd7d8b2a28f104bbce6))
- 【web】修复文件上传异常单位显示错误 ([e7566d2](https://github.com/continew-org/continew-starter/commit/e7566d284b53b47577ade59c0b7e9262f9b43758))

### 💎 功能优化

- 【core】优化 JSR 303 校验方法 ([b0f5506](https://github.com/continew-org/continew-starter/commit/b0f55064242615717789b3d62880e482ea72a23a))
- 【extension/crud】调整 BaseService 相关泛型类型加载为懒加载 ([dca7157](https://github.com/continew-org/continew-starter/commit/dca715709faa9fbd61194ea4177c91475b768694))

### 📦 依赖升级

- SpringBoot 3.1.11 => 3.2.7（TaskExecutor => ThreadPoolTaskExecutor）
- MyBatisPlus 3.5.5 => 3.5.7（数据权限处理器调整）
- MyBatisFlex 1.8.9 => 1.9.3
- dynamic-datasource 4.3.0 => 4.3.1
- JetCache 2.7.5 => 2.7.6
- Redisson 3.30.0 => 3.32.0
- CosID 2.6.8 => 2.9.1
- EasyExcel 3.3.4 => 4.0.1
- XFileStorage 2.1.0 => 2.2.0
- Crane4j 2.8.0 => 2.9.0
- Hutool 5.8.27 => 5.8.29
- AWS S3 1.12.720 => 1.12.761
- IP2Region 3.1.11 => 3.2.6

## [v2.2.0](https://github.com/continew-org/continew-starter/compare/v2.1.1...v2.2.0) (2024-06-30)

### ✨ 新特性

- 新增国际化及全局异常码配置 (Gitee#25) ([ce08f28](https://github.com/continew-org/continew-starter/commit/ce08f28a618bbeb2c501defe71f9018972a4828b))
- 【core】新增 JSR 303 校验方法 ([3e9a152](https://github.com/continew-org/continew-starter/commit/3e9a15295a79901cf1c5fa603d6a7407e7e2a2ec))
- 【security/limiter】新增限流器 ([a89765f](https://github.com/continew-org/continew-starter/commit/a89765f49ef9d3b1ce4b3a420507b43792ed69a1)) ([51c4775](https://github.com/continew-org/continew-starter/commit/51c47751f4ef92bb111619ee9ceb7c3ce4e2dba4)) ([7bc25b2](https://github.com/continew-org/continew-starter/commit/7bc25b2f8bdb74ad295c54ab82cdae88f6264096)) ([13788d6](https://github.com/continew-org/continew-starter/commit/13788d6f5796e87900dd83ece955cd921ffc3946))
- 【core】新增表达式 SPEL 解析工具类 ([13b3f24](https://github.com/continew-org/continew-starter/commit/13b3f2484555b69dd25b280806f98d98d53f75fe))

### 🐛 问题修复

- 【api-doc】修复接口文档配置错误 ([82574cd](https://github.com/continew-org/continew-starter/commit/82574cd5cee923d6dfe447414c0a2453defc8790))

### 💎 功能优化

- 【core】重构线程池自动配置 ([de056aa](https://github.com/continew-org/continew-starter/commit/de056aa0c42621f5d5cf7690f2a42f54ffa1cd7e)) ([0ad7b18](https://github.com/continew-org/continew-starter/commit/0ad7b185212da31d8b6afdab6dd9cd8f72f83acb))
- 优化属性前缀命名 ([6b90880](https://github.com/continew-org/continew-starter/commit/6b90880c21d4fd7e603397692cf88a98f30194a0))
- 【captcha/behavior】默认启用行为验证码自动配置 ([635b664](https://github.com/continew-org/continew-starter/commit/635b664d5f92e5d01cadef4c868753eb41279c7d))
- 【messaging/mail】优化邮件配置服务命名 ([3e4b6ab](https://github.com/continew-org/continew-starter/commit/3e4b6ab3a9590639e1fa606b0d52b29e83ecb890))

## [v2.1.1](https://github.com/continew-org/continew-starter/compare/v2.1.0...v2.1.1) (2024-06-23)

### ✨ 新特性

- 【data/mybatis-plus】新增防全表更新与删除插件启用配置 ([c84539b](https://github.com/continew-org/continew-starter/commit/c84539b4619d2064c8996cd3e574fa5141e124da))
- 【messaging/mail】邮件支持自定义发件人 ([27b949d](https://github.com/continew-org/continew-starter/commit/27b949dd9d62f68c74d8a12729c4de5fdcc414c9))
- 【cache/redisson】RedisUtils 新增 List 缓存操作方法 ([92fd0a8](https://github.com/continew-org/continew-starter/commit/92fd0a8ab2c0250981aceb006a86c1e911719970))

### 🐛 问题修复

- 【security/crypto】修复处理 MP Wrapper 时 无法加密的情况 (GitHub#4) ([a235a6e](https://github.com/continew-org/continew-starter/commit/a235a6ea8b574c3f719857bb99d05e874d4e9bd2))
- 【log/core】兼容日志记录 IPv6 IP 归属地场景 ([0bba30b](https://github.com/continew-org/continew-starter/commit/0bba30b9c4c14e5582b034420f47d0567fdc482a))
- 【extension/crud】排除 SaToken Starter 中的 Web 依赖 ([6e73472](https://github.com/continew-org/continew-starter/commit/6e73472589f65d63ef5d397e681f5af6b31b43c6))

### 💎 功能优化

- 【web】优化全局文件上传异常-超过上传大小限制的异常判断 ([1bc4ba7](https://github.com/continew-org/continew-starter/commit/1bc4ba76b20f06b2932c1bc20948a6d88d40ab2b))
- 【core】线程池配置增加默认线程前缀配置 ([a208fa5](https://github.com/continew-org/continew-starter/commit/a208fa59b2a9457bced4dfd3b24f92ce4a73f1f6))
- 【messaging/websocket】优化 WebSocket 相关配置及命名 ([6c10e80](https://github.com/continew-org/continew-starter/commit/6c10e80d71a7ce768d4fd44006c707c599b3960e))

## [v2.1.0](https://github.com/continew-org/continew-starter/compare/v2.0.2...v2.1.0) (2024-06-05)

### ✨ 新特性

- 【messaging/mail】新增动态邮箱配置 ([Gitee#19](https://gitee.com/continew/continew-starter/pulls/19)) ([ee30e86](https://github.com/continew-org/continew-starter/commit/ee30e861ff536ee3ed6f14ff5ded5af7a513941d)) ([7feda79](https://github.com/continew-org/continew-starter/commit/7feda79359ea40331eee1e3d4d5fd12000f027c5))
- 【data/mybatis-flex】新增 continew-starter-data-mybatis-flex 数据访问模块（Mybatis Flex 自动配置） ([Gitee#18](https://gitee.com/continew/continew-starter/pulls/18)) ([124c7ff](https://github.com/continew-org/continew-starter/commit/124c7ffe11a0e6563d9b513036c53ff66edbb9b3))
- 【extension/crud】新增查询字典列表方法 ([3d2a427](https://github.com/continew-org/continew-starter/commit/3d2a4271d5eed676f16f4728b461dc3b298a65a9))
- 【messaging/websocket】新增 continew-starter-messaging-websocket 消息模块 ([cc079e8](https://github.com/continew-org/continew-starter/commit/cc079e8bf422825bf9a96ddbd4329fc77d3cbf2c))

## [v2.0.2](https://github.com/continew-org/continew-starter/compare/v2.0.1...v2.0.2) (2024-05-20)

### ✨ 新特性

- 【file/excel】新增 Easy Excel List 集合转换器 ([1faa46e](https://github.com/continew-org/continew-starter/commit/1faa46e12505c025e5ca6f1a45158324ac210799))

### 🐛 问题修复

- 【captcha/behavior】修复行为验证码接口请求次数限制 ([Gitee#17](https://gitee.com/continew/continew-starter/pulls/17))
- 【extension/crud】修复封装分页 dataList 索引计算错误 ([3e60197](https://github.com/continew-org/continew-starter/commit/3e60197a31f140c863868440944df0427a9cf8e8))
- 移除部分错误依赖声明 ([881fd37](https://github.com/continew-org/continew-starter/commit/881fd37dd61836daf3343d9053e9a7c81b005923))

## [v2.0.1](https://github.com/continew-org/continew-starter/compare/v2.0.0...v2.0.1) (2024-05-13)

### ✨ 新特性

- 【cache/redisson】RedisUtils 新增递增、递减方法 ([596605b](https://github.com/continew-org/continew-starter/commit/596605b27b046fa0488b113b1c3d87c60277e4ec))
- 【core】新增字符串工具类 ([ca6c709](https://github.com/continew-org/continew-starter/commit/ca6c7098b124c3121fe626811ea61d53c80a9a4e))
- 【extension/crud】新增多种实体 Base 模型降低 BaseService 耦合 ([5b76534](https://github.com/continew-org/continew-starter/commit/5b76534df7a54aa6d696515cfbf8059fcdc4a067))

### 🐛 问题修复

- 【extension/crud】修复查询树列表方法中的错误判断 ([f138e5c](https://github.com/continew-org/continew-starter/commit/f138e5cd4526d8be0fe5e7ae54833b4541748346))

### 💎 功能优化

- 【web】优化部分方法使用 ([57eef27](https://github.com/continew-org/continew-starter/commit/57eef274a3e34e8bb4de6073452080b2bbdbee53))

### 📦 依赖升级

- 【dependencies】Spring Boot 3.1.10 => 3.1.11 ([GitHub#2](https://github.com/continew-org/continew-starter/pull/2))
- 【dependencies】SaToken 1.37.0 => 1.38.0 ([b5dd5c7](https://github.com/continew-org/continew-starter/commit/b5dd5c7f18603b5bd5a509c04ef410c1d64bc2e9))
- 【dependencies】Redisson 3.28.0 => 3.30.0 ([b5dd5c7](https://github.com/continew-org/continew-starter/commit/b5dd5c7f18603b5bd5a509c04ef410c1d64bc2e9))
- 【dependencies】Crane4j 2.7.0 => 2.8.0 ([b5dd5c7](https://github.com/continew-org/continew-starter/commit/b5dd5c7f18603b5bd5a509c04ef410c1d64bc2e9))
- 【dependencies】AWS S3 1.12.702 => 1.12.720 ([b5dd5c7](https://github.com/continew-org/continew-starter/commit/b5dd5c7f18603b5bd5a509c04ef410c1d64bc2e9))
- 【dependencies】IP2Region 3.1.10 => 3.1.11 ([b5dd5c7](https://github.com/continew-org/continew-starter/commit/b5dd5c7f18603b5bd5a509c04ef410c1d64bc2e9))

## [v2.0.0](https://github.com/continew-org/continew-starter/compare/v1.5.1...v2.0.0) (2024-04-17)

### ✨ 新特性

- 【web】新增 XSS 过滤器 ([2656da4](https://github.com/continew-org/continew-starter/commit/2656da450c866681c270c30131c028847e1e21d4)) ([2573fb0](https://github.com/continew-org/continew-starter/commit/2573fb04f0698db3ab662a0e7bf762c04300468b)) ([Gitee PR#13](https://gitee.com/continew/continew-starter/pulls/13))

### 🐛 问题修复

- 【cache/redisson】修复Failed to submit a listener notification task. Event loop shut down? 问题 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))

### 💎 功能优化

- 【core】应用关闭，支持优雅关闭自定义线程池 ScheduledExecutorService ([c4051a6](https://github.com/continew-org/continew-starter/commit/c4051a6465e0d70d119ec27c6ae4eb4d1893339a))
- 【extension/crud】优化部分注释 ([fe310bc](https://github.com/continew-org/continew-starter/commit/fe310bcb879d3f20eb8ead4b39436ec96b99e7f6))
- 移除 Qodana 扫描，License 已过期 ([b0e567d](https://github.com/continew-org/continew-starter/commit/b0e567d749b988e3f45772742a273a422a661edb))

### 📦 依赖升级

- 【dependencies】Spring Boot 3.1.9 => 3.1.10 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))
- 【dependencies】Redisson 3.27.2 => 3.28.0 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))
- 【dependencies】CosId 2.6.6 => 2.6.8 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))
- 【dependencies】SMS4J 3.1.1 => 3.2.1 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))
- 【dependencies】Easy Excel 3.3.3 => 3.3.4 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))
- 【dependencies】AWS S3 1.12.675 => 1.12.702 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))
- 【dependencies】Crane4j 2.6.1 => 2.7.0 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))
- 【dependencies】TLog 1.5.1 => 1.5.2 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))
- 【dependencies】Hutool 5.8.26 => 5.8.27 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))
- 【dependencies】IP2Region 3.1.9 => 3.1.10 ([2d71eca](https://github.com/continew-org/continew-starter/commit/2d71eca07505f143c82cca8d24bc6f54105d0bbb))

### 💥 破坏性变更

- groupId 及基础包名调整，更短的包名，聚合品牌形象。top.charles7c.continew => top.continew ([dbb7a63](https://github.com/continew-org/continew-starter/commit/dbb7a6319e9440e7a05f2eb4aab3b445f43197f7))
