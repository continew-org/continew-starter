## [v1.1.1](https://github.com/Charles7c/continew-starter/compare/v1.1.0...v1.1.1) (2024-01-11)

### ✨ 新特性

* 【extension/crud】BaseService 增加 list 查询列表方法重载 ([81ed292](https://github.com/Charles7c/continew-starter/commit/81ed29284090edcfc5ea5351442b5de2ce1622df))
* 【core】新增 SpringUtils 工具类 ([3de75cf](https://github.com/Charles7c/continew-starter/commit/3de75cf7fe79bc86ca5022d56e5f46be4d90d623))

### 💎 功能优化

- 【log/httptrace-pro】优化日志过滤器，仅在需要记录请求体、响应体时进行过滤 ([d68d88d](https://github.com/Charles7c/continew-starter/commit/d68d88db218d5008140c3056827dd6ac608a8b62))
- 【log/httptrace-pro】优化 @Log 注解信息获取优先级逻辑 ([Gitee PR#5](https://gitee.com/Charles7c/continew-starter/pulls/5))
- 【extension/crud】优化 BaseServiceImpl 中获取各泛型参数类型的方式 ([6fc0b51](https://github.com/Charles7c/continew-starter/commit/6fc0b51a574434db9d21d1f254b3fce344c9f2f6))
- 【extension/crud】减少查询列表时可能的无用转换 ([0565372](https://github.com/Charles7c/continew-starter/commit/0565372e9aa8010a1c4625be4cf85d557a7eed7b))
- 使用常量优化部分配置属性名 ([2025068](https://github.com/Charles7c/continew-starter/commit/20250681da7682de159b6259e80193b204e55047))
- 优化日志级别 info => debug ([1e7d4b2](https://github.com/Charles7c/continew-starter/commit/1e7d4b2721fae3459cb6d1b57f208f0c38dbbc6f))
- 优化全局代码格式 ([57c21a9](https://github.com/Charles7c/continew-starter/commit/57c21a9109a412ed78c6c9b8aa0cd0f0b5724432))

### 💥 破坏性变更

- 【extension/crud】PageDataResp => PageResp ([38d2800](https://github.com/Charles7c/continew-starter/commit/38d28004d63a0218bfcae5689f9909ce6dcd824f))

## [v1.1.0](https://github.com/Charles7c/continew-starter/compare/v1.0.1...v1.1.0) (2023-12-31)

### ✨ 新特性

* 【log/httptrace-pro】新增 continew-starter-log-httptrace-pro 日志模块（Spring Boot Actuator HttpTrace 重置增强版）
* 【storage/local】新增 continew-starter-storage-local 本地存储模块 ([cd6826a](https://github.com/Charles7c/continew-starter/commit/cd6826a0abe0666f9fe867e92bf70abb47e5ff2e))
* 【cache/redisson】RedisUtils 新增限流方法 ([9cf3ae8](https://github.com/Charles7c/continew-starter/commit/9cf3ae87a1a20db9ee8b2b7272e8328b5fc5c20c))
* 【data/mybatis-plus】新增数据权限默认解决方案 ([621a5e3](https://github.com/Charles7c/continew-starter/commit/621a5e3b22db9b81d31c65b39ad387a8531e09af))
* 【captcha/behavior】新增 continew-starter-captcha-behavior 行为验证码模块 ([Gitee PR#1](https://gitee.com/Charles7c/continew-starter/pulls/1))
* 【core】新增 PATH_PATTERN 字符串常量 ([76e282c](https://github.com/Charles7c/continew-starter/commit/76e282c7965fdfa39854fe77397687bbc40d0f7f))

### 💎 功能优化

- 【core】优化跨域配置默认值 ([65f5fbd](https://github.com/Charles7c/continew-starter/commit/65f5fbd6daa9ae2c8aedd13c487e8985523233ce))
- 【extension/crud】新增全局异常处理器 ([c4459d1](https://github.com/Charles7c/continew-starter/commit/c4459d1b8d701a4405f74ea92cfc87752a285b55))
- 【extension/crud】移除部分方法中仅有单个非读操作的事务处理 ([70ae383](https://github.com/Charles7c/continew-starter/commit/70ae383de62bc3c6ae0d2e1c3cf5c005d54f83f5))

### 📦 依赖升级

- 【dependencies】Spring Boot 3.1.5 => 3.1.7 ([72f5569](https://github.com/Charles7c/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Just Auth 1.16.5 => 1.16.6 ([72f5569](https://github.com/Charles7c/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Redisson 3.24.3 => 3.25.2 ([72f5569](https://github.com/Charles7c/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Easy Excel 3.3.2 => 3.3.3 ([72f5569](https://github.com/Charles7c/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Knife4j 4.3.0 => 4.4.0 ([72f5569](https://github.com/Charles7c/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】Hutool 5.8.23 => 5.8.24 ([72f5569](https://github.com/Charles7c/continew-starter/commit/72f55697cc8958bf3586daed03a8d1b3c8636605))
- 【dependencies】MyBatis Plus 3.5.4.1 => 3.5.5（修复与 Spring Boot 3.1.7 的 DdlApplicationRunner冲突错误） ([556bfb9](https://github.com/Charles7c/continew-starter/commit/556bfb924a1e5834fe0a101b9ff52cc5bb36d578))
- 【dependencies】新增 X File Storage 依赖版本 2.0.0 ([be7972c](https://github.com/Charles7c/continew-starter/commit/be7972c00be8d62cc25332e053a985532016de2d))
- 【dependencies】ip2region 3.1.5.1 => 3.1.6 ([4dae89e](https://github.com/Charles7c/continew-starter/commit/4dae89e0f21ac6c532101e983ee4007f3980c929))
- 【dependencies】新增 Amazon S3 依赖版本 1.12.626 ([48f894b](https://github.com/Charles7c/continew-starter/commit/48f894b8b62f8b968091dcea51b57336b97e4a2d))

### 💥 破坏性变更

- 【captcha/graphic】优化图形验证码配置前缀 ([e0e5944](https://github.com/Charles7c/continew-starter/commit/e0e5944b45bcbf8a4b7a5066ad347459a7b3e450))
- 【data/mybatis-plus】调整 IBaseEnum 所属包 enums => base ([22fee2f](https://github.com/Charles7c/continew-starter/commit/22fee2f5bd8211e26c2f6a163a6298f5b522833c))
- 【auth/satoken】SaTokenDaoTypeEnum => SaTokenDaoType ([0a0d022](https://github.com/Charles7c/continew-starter/commit/0a0d022586dc88a773512c5761c68d62786e35c4))
- 【core】使用常量优化部分魔法值，核心模块部分配置前缀调整 ([52dce2a](https://github.com/Charles7c/continew-starter/commit/52dce2acdfa0296c3f6f4875f14a0299f999f899))

## [v1.0.1](https://github.com/Charles7c/continew-starter/compare/v1.0.0...v1.0.1) (2023-12-13)

### 💎 功能优化

- 【data/mybatis-plus】QueryTypeEnum => QueryType，并取消实现 IBaseEnum 接口 ([bc00c9b](https://github.com/Charles7c/continew-starter/commit/bc00c9bab0ed4508fd1dc0da8a76ef96739cce1d))
- 【api-doc】新增鉴权配置 ([7997267](https://github.com/Charles7c/continew-starter/commit/7997267060b3e79f80dd73cec722bc295635a93b))

### 🐛 问题修复

- 【extension/crud】修复使用 @CrudRequestMapping 后自定义 API 不显示的问题 ([1adfddf](https://github.com/Charles7c/continew-starter/commit/1adfddfa3b276e764b098512b2e9c75f007d13c1))

### 💥 破坏性变更

- 【extension/crud】调整通用查询注解所属模块 crud => mybatis-plus ([083bc7b](https://github.com/Charles7c/continew-starter/commit/083bc7b38a861339ceb7a06acdd20ea64bc84990))
- 【extension/crud】调整校验工具类所属模块 crud => core ([083bc7b](https://github.com/Charles7c/continew-starter/commit/083bc7b38a861339ceb7a06acdd20ea64bc84990))

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
