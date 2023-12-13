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
