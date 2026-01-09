## [v2.15.0](https://github.com/continew-org/continew-starter/compare/v2.14.0...v2.15.0) (2025-12-28)

### ✨ 新特性

- 【core】添加 ContiNew Starter 版本信息类 ([33748f7](https://github.com/continew-org/continew-starter/commit/33748f7cec77bda4b692b2d859b8668aa062e03b))
- 将默认 API 文档 UI 从 Knife4j 替换为 NextDoc4j 1.1.5 ([a60d452](https://github.com/continew-org/continew-starter/commit/a60d452aee2ee10c1af1592e638b5960249b6f28)) ([f1937d3](https://github.com/continew-org/continew-starter/commit/f1937d3968b48616ef1b7a845d32422e08e23ecd)) ([58e234a](https://github.com/continew-org/continew-starter/commit/58e234a929d5cbc826b4a9251b074bb82862eaef)) ([deb330b](https://github.com/continew-org/continew-starter/commit/deb330b4cdcacaaab61002dfa8826b7f4e37d238)) ([730b39d](https://github.com/continew-org/continew-starter/commit/730b39d18ef9116f37a366be33000edddbfdc2cc)) ([4a82325](https://github.com/continew-org/continew-starter/commit/4a82325b5109439ff2ba6b1bff256e4f04d834ff)) ([776acc6](https://github.com/continew-org/continew-starter/commit/776acc6e5c652dd6cf9324dad5dd07f7d0d31305)) (Gitee#82@dom-w)
- 【messaging/mqtt】新增 MQTT 消息模块 ([ee75e84](https://github.com/continew-org/continew-starter/commit/ee75e849e21f787969c6ca1c1034913663f8386b)) (Gitee#85@dom-w)
- 【core】新增 MultipartFileUtils，支持 File 转换为 MultipartFile ([be17196](https://github.com/continew-org/continew-starter/commit/be17196ef3c9888167bf587013383c21da6f7ebf))
- 【auth/justauth】新增 JustAuth 自动配置，移除 justauth-starter ([0798424](https://github.com/continew-org/continew-starter/commit/0798424dc86887711f2708d4c84cc399b46781d1))
- 【cache/redisson】新增 RedisUtils#getOrDefault 方法 ([a512b29](https://github.com/continew-org/continew-starter/commit/a512b29082be98179f3aab009f40765af4be7b49))

### 💎 功能优化

- 【extension/datapermission】DataPermission 注解增加缓存处理，缓存 Mapper 接口方法上携带 DataPermission 的值 ([a4fe07b](https://github.com/continew-org/continew-starter/commit/a4fe07bc63eb49a7647347e6d504726cffe99c0e)) (Gitee#81@httpsjt)
- 【validation】EnumValue 比较枚举值时，不再区分大小写 ([a7f3df8](https://github.com/continew-org/continew-starter/commit/a7f3df82c7a6f144b29296aa9cdc9e698178387f))

### 🐛 问题修复

- 【security/xss】修复 XssServletRequestWrapper 读取请求体数据不全的问题 ([840e77d](https://github.com/continew-org/continew-starter/commit/840e77d1ca442914f48a4a5c229a8c31cf890197))
- 【messaging/mail】修复开启 SSL 后，mail.host 不被 JDK 信任的问题 ([17d62d0](https://github.com/continew-org/continew-starter/commit/17d62d041c1c2d2acb411d4c1e7f48f2ee1804f1))

### 📦 依赖升级

- spring-boot 3.3.12 => 3.4.10 ([e8d2bfd](https://github.com/continew-org/continew-starter/commit/e8d2bfddcf6d20ca6a5018343b1d8f6f48b5f384))
- spring-cloud 2023.0.5 => 2024.0.2
- redisson 3.49.0 => 3.52.0
- mybatis-plus 3.5.12 => 3.5.14
- mybatis-flex 1.10.9 => 1.11.3
- cosid 2.13.0 => 2.13.3
- snail-job 1.5.0 => 1.8.0
- fastexcel 1.2.0 => 1.3.0
- aws-sdk-v1 1.12.783 => 1.12.792
- aws-sdk 2.31.63 => 2.35.10
- aws-crt 0.38.5 => 0.39.3
- thumbnails 0.4.20 => 0.4.21
- spel-validator 0.5.2-beta => 0.6.0-beta
- ip2region 3.3.6 => 3.4.7
- hutool 5.8.38 => 5.8.41
- snakeyaml 2.4 => 2.5
- nashorn 15.6 => 15.7
- commons-io 2.17.0 => 2.20.0
- commons-compress 1.26.0 => 1.28.0
- flatten 1.7.0 => 1.7.3
- spotless 2.44.3 => 3.0.0
- sonar 3.11.0.3922 => 5.2.0.4988

## [v2.14.0](https://github.com/continew-org/continew-starter/compare/v2.13.4...v2.14.0) (2025-10-03)

### ✨ 新特性

- 【security/crypto】新增 API 加/解密功能 ([26effb6](https://github.com/continew-org/continew-starter/commit/26effb6ee2a98cbedc0dd3ea1d15b453cdf0c0d8)) (Gitcode#3@lishuyanla)
- 【core】MapUtils增加深度合并两个map的方法 ([5ca34ee](https://github.com/continew-org/continew-starter/commit/5ca34eebd1228b445d9882d0d2777affd4393bca))  (Github#16@luoqiz) 
- 【encrypt/password-encoder】新增密码编码器模块（经过考量重新拆分出来） ([e414abc](https://github.com/continew-org/continew-starter/commit/e414abc73536c57a774f2c6d5dbeff78349310ae))

### 💎 功能优化

- 【storage】重构存储模块为统一入口 ([ae1258a](https://github.com/continew-org/continew-starter/commit/ae1258aee65c534564372b6fa2a2cf37a74fc601)) ([e5002b8](https://github.com/continew-org/continew-starter/commit/e5002b8bfc4aa20b99be9f699ac55a300a2525fd)) ([7ead337](https://github.com/continew-org/continew-starter/commit/7ead337165f930771b86a1b5d36f4f4bdacfbb12)) (Gitcode#1@QAQ_Z)
- 【excel/fastexcel】移除 ExcelListConverter 中的冗余注解 ([5d7c3be](https://github.com/continew-org/continew-starter/commit/5d7c3bedd74274cedbf27b31ecd9b68458ab735f))
- 【extension/crud】优化树型结构字典配置相关命名及注释 DICT_TREE -> TREE_DICT ([3ee1112](https://github.com/continew-org/continew-starter/commit/3ee1112c4ce9b1acfa7ac5959a4e1ffd19ee0fce))
- 【encrypt】拆分字段加密、API 加密模块 ([e9bf92e](https://github.com/continew-org/continew-starter/commit/e9bf92ea1f090068c2f849f5be36c372ffb1bf3e))
- 统一过滤器配置 ([637d92b](https://github.com/continew-org/continew-starter/commit/637d92be237eced80c4c17acae344d31c4cc4eb2))
- 【core】重构线程池自动配置代码 ([6889578](https://github.com/continew-org/continew-starter/commit/68895787a758606f226da7fe7478ae9b60a8926e))
- 【data】移除 QueryIgnore 注解，并取消默认 eq 逻辑处理 ([e9a6f36](https://github.com/continew-org/continew-starter/commit/e9a6f36136319d65ba6379506264c94a4994d269))
- 【messaging/mail】提供 JavaMailSenderImpl 默认配置，并重构 MailConfigurer 配置代码 ([75aeb26](https://github.com/continew-org/continew-starter/commit/75aeb26a4f117acab8bf6e4b60c0efbc01a5d0fb))

### 🐛 问题修复

- 【auth/satoken】修复 ConditionalOnBean 校验导致的 SaToken 持久层 Redis 实现注册失败的问题 ([61fbb04](https://github.com/continew-org/continew-starter/commit/61fbb04a331358a7b18f5373625ab169f882ae4d))
- 【extension/tenant】修复多租户下开启多数据源拦截器返回结果异常的情况 ([d1db737](https://github.com/continew-org/continew-starter/commit/d1db737f7a62a8afe7585075789d05a72b9d55e9)) (Gitee#80@kiki1373639299) 

## [v2.13.4](https://github.com/continew-org/continew-starter/compare/v2.13.3...v2.13.4) (2025-07-26)

### ✨ 新特性

- 【cache/redisson】RedisUtils 新增 Hash 常用操作方法（hSet/hGet/hGetAll/hExists/hDel）(Gitee#77@kiki1373639299) ([8676f9b](https://github.com/continew-org/continew-starter/commit/8676f9b5912914f2bc1025d7695e2b11136bad20))
- 【extension/crud】CRUD API 新增 DICT（字典列表（下拉选项等场景））、DICT_TREE（字典树列表（树型结构下拉选项等场景）） ([ecabda6](https://github.com/continew-org/continew-starter/commit/ecabda6aecc70fdb49eebd2dcde5bddd63fe337b))
- 【security/crypto】新增密码编码器配置（由原 security/password 模块融合） ([0ba365d](https://github.com/continew-org/continew-starter/commit/0ba365dabc3c18a7b07eaec05c17a8848642e78f)) ([49c804a](https://github.com/continew-org/continew-starter/commit/49c804ac9e577ccc4019319c812ebcdf20ef5ad6))
- 【cache/redisson】新增 RedisLockUtils Redisson 分布式锁工具类 (Gitee#78@lishuyanla) ([48783db](https://github.com/continew-org/continew-starter/commit/48783db422525548d7eec5caba788f8ab53d7bec))

### 💎 功能优化

- 【cache/redisson】移除 RedisQueueUtils 类 ([e5354b7](https://github.com/continew-org/continew-starter/commit/e5354b765d27f4ba853865fa6290f706f14a990c))
- 【extension/crud】优化 CRUD API 自动配置代码，EnableCrudRestController => EnableCrudApi ([ca33851](https://github.com/continew-org/continew-starter/commit/ca33851fbd92f145229844c464a0cf1edbf7b9c7)) ([1fdb029](https://github.com/continew-org/continew-starter/commit/1fdb0291d20975e667232f866e3605712b29f8a9))
- 【cache/redisson】移除 RedisUtils 中的 Lock 相关工具方法（统一使用 RedisLockUtils） ([cff4f02](https://github.com/continew-org/continew-starter/commit/cff4f02d966836fd291c268d0e44b0047e35d053))

### 🐛 问题修复

- 【security/crypto】修复 构造默认加密上下文时缺失默认加密器 导致找不到加密器的问题 (Gitee#76@lishuyanla) ([d0eddcb](https://github.com/continew-org/continew-starter/commit/d0eddcb9f73282578df6ebb6b8c3d41fe164abd0))

## [v2.13.3](https://github.com/continew-org/continew-starter/compare/v2.13.2...v2.13.3) (2025-07-22)

### ✨ 新特性

- 【core】ReflectUtils 新增 createMethodReference 方法（由 CRUD 模块迁移） ([1eb1c2d](https://github.com/continew-org/continew-starter/commit/1eb1c2d845ded85197a222e492b0afe5bd8da48d))
- 【data】Query 注解新增多列查询逻辑关系支持（原来仅支持或者，现在也支持并且） ([3e822c0](https://github.com/continew-org/continew-starter/commit/3e822c0b8442a5a00840a9ae67d7fa03cd5d33b0))
- 【core】新增 OrderedConstants 统一登记过滤器和拦截器相关顺序常量，并调整相关过滤器和拦截器顺序 ([a392fab](https://github.com/continew-org/continew-starter/commit/a392fab78222db8f05933e398d8b0541aed07651))
- 【security/password】重构密码编码器，新增 PasswordEncoderUtil ([58f9687](https://github.com/continew-org/continew-starter/commit/58f9687c581c121d4688e2ab99678d94d262c60a))
- 【security/crypto】新增支持密码编码器加密 ([38b6428](https://github.com/continew-org/continew-starter/commit/38b6428662b909875df4ae8f36f180b0394accc1))

### 💎 功能优化

- 【extension/crud】重构查询树列表功能，增加重载方法，支持构建单个根节点或者多个根节点的树结构 (Gitee#75@lishuyanla) ([55660ba](https://github.com/continew-org/continew-starter/commit/55660ba18bb3b8b8cecc1c979aa71cde5b4b39d9)) ([a213537](https://github.com/continew-org/continew-starter/commit/a2135374b231ee410bafc8573e706443c6097353))
- 【core】TreeBuildUtils => TreeUtils ([c76d777](https://github.com/continew-org/continew-starter/commit/c76d777a2e3b20a0542ef606cb3a4c85068a25fe))
- 【extension/crud】优化部分代码 ([0a9027d](https://github.com/continew-org/continew-starter/commit/0a9027d91f3a2618f91e7b5417cbed5288e1e46b))
- 【web】拆分 default-web.yml 为 default-response.yml 和 default-server.yml 配置文件 ([e64553e](https://github.com/continew-org/continew-starter/commit/e64553e6205ca3473a656f60448304bf4c18ddca))

### 🐛 问题修复

- 【security/crypto】修复新版 API 未支持自定义加密器问题 (Gitee#74@lishuyanla) ([36c30a2](https://github.com/continew-org/continew-starter/commit/36c30a20ddff30832a31e7d6751d0140c45de3a7))

### 📦 依赖升级

- 【dependencies】spel-validator 0.5.1-beta => 0.5.2-beta ([9d39012](https://github.com/continew-org/continew-starter/commit/9d39012f0b53baa81040a863526048955cab6d11))

## [v2.13.2](https://github.com/continew-org/continew-starter/compare/v2.13.1...v2.13.2) (2025-07-21)

### ✨ 新特性

- 【core】新增 扩展 hutool TreeUtil 封装树构建的 TreeBuildUtils 工具类，其中包括扩展的（构建树形结构、构建多根节点的树结构（支持多个顶级节点））等方法。(Gitee#72@lishuyanla) ([90c11f6](https://github.com/continew-org/continew-starter/commit/90c11f60f9ba313acbfd76de66f3b4022bc8b270))
- 【security/crypto】重构加/解密模块业务逻辑，封装 EncryptHelper 工具类，提供统一的加/解密方法，方便使用者灵活处理加/解密 (Gitee#73@lishuyanla) ([5d10a28](https://github.com/continew-org/continew-starter/commit/5d10a28aa1c4ade0a51235e302c46143b90f7bb5))

### 💎 功能优化

- 【extension/tenant】移除超级租户 ID 配置属性 ([a778e31](https://github.com/continew-org/continew-starter/commit/a778e3182a8163e9e3ea5bbc677090da2efe0a31))
- 【extension/tenant】设置租户拦截器的优先级为最高 ([d8c4224](https://github.com/continew-org/continew-starter/commit/d8c4224030d6d2eb6eea3554e689165315924bf6))
- 【extension/tenant】优化租户忽略逻辑 ([35e7962](https://github.com/continew-org/continew-starter/commit/35e79620e40d8d4f121a24ec720dcd8968ce9104))
- 【extension/crud】 ([586322a](https://github.com/continew-org/continew-starter/commit/586322a180f2bce9faf1acbacb65ec09df921815))
- 【extension/datapermission】优化数据权限模块代码 ([5dd6808](https://github.com/continew-org/continew-starter/commit/5dd6808bea4483a7e69884b69bac4928cb95bd89))
- 【json/jackson】重构 JSON 工具类 ([43d1489](https://github.com/continew-org/continew-starter/commit/43d1489f1a850731b4fc27a2ae0cbab24a72025c))
- 解决部分 sonar 问题 ([ddd4e38](https://github.com/continew-org/continew-starter/commit/ddd4e38dca4c5f64b9fc999d57a13d827d29d474)) ([47165f8](https://github.com/continew-org/continew-starter/commit/47165f80a15cf7da346fbbb931894284b0cd7124))

### 🐛 问题修复

- 【validation】修复字符串值仅进行了 null 判空错误 ([12746d6](https://github.com/continew-org/continew-starter/commit/12746d62613f3e9d8cce4b4aea71d6466f345e0a))
- 【extension/tenant】将 TenantUtils.executeIgnore 方法改为静态方法 ([43ba770](https://github.com/continew-org/continew-starter/commit/43ba770971e5fb124272ed6d4fadef36be9c8fb8))

### 📦 依赖升级

- 【dependencies】spel-validator 0.5.0-beta => 0.5.1-beta ([601c071](https://github.com/continew-org/continew-starter/commit/601c0715052106f4cae3419fda0f276231cb3b13))

## [v2.13.1](https://github.com/continew-org/continew-starter/compare/v2.13.0...v2.13.1) (2025-07-17)

### ✨ 新特性

- 【validation】增强 EnumValue 枚举校验器，支持枚举值的数组和集合校验，增加对 BaseEnum 接口的支持 ([3dad27d](https://github.com/continew-org/continew-starter/commit/3dad27df3f747d60f47c1504286c86f6636e7242))
- 【extension/tenant】新增 TenantIgnoreAspect 切面，完善定时任务等需要忽略租户的场景 ([07e1637](https://github.com/continew-org/continew-starter/commit/07e1637363bce4cac3f215384c8bbf6235a30778))
- 【core】SpringUtils 工具类新增 getBean(Class<T> clazz, boolean ignoreNoSuchBeanEx) 方法 ([17272a7](https://github.com/continew-org/continew-starter/commit/17272a780905b554b1fb47e52667a51be0af7bbe))
- 【core】新增集合工具类 CollUtils（mapToList、mapToSet） ([3f7f118](https://github.com/continew-org/continew-starter/commit/3f7f118d3e6b38c2cb13a2661a37eda3325894a7))
- 【extension/tenant】新增 TenantUtils 替换 TenantHandler 接口及其实现类 DefaultTenantHandler ([2f2aae0](https://github.com/continew-org/continew-starter/commit/2f2aae08ab934009cd39bbe7ec3823c594fa48f8))
- 【core】ServletUtils 新增应 JSON 数据给客户端方法 ([67edb08](https://github.com/continew-org/continew-starter/commit/67edb0828dee355ff46d055935a76a42a5a6ebd8))

### 💎 功能优化

- 【extension/crud】完善树配置相关注释 ([3be0d90](https://github.com/continew-org/continew-starter/commit/3be0d900180e4ed528f32f2350b150552aee0420))
- 【extension/crud】移除 Crane4j 依赖方便使用者自定义实现 ([aefd61b](https://github.com/continew-org/continew-starter/commit/aefd61b855e376b0f509d34d441b1d1d5b831a39))
- 【extension/tenant】将"多租户"描述统一为"租户" ([d32c051](https://github.com/continew-org/continew-starter/commit/d32c05166d297d1665436eae63f41277f6dca2af))
- 【extension/tenant】将 dynamic-datasource 依赖设置为 optional ([778a861](https://github.com/continew-org/continew-starter/commit/778a861fa9f97a99a7014b72a37e984145872421))
- 【extension/datapermission】UserContext、RoleContext 重命名为 UserData、RoleData，以避免和应用冲突 ([a0b64b8](https://github.com/continew-org/continew-starter/commit/a0b64b81d560c3c4e7175685f66ab98406a31dcc))
- 使用 CollUtils 替代部分 Stream 集合转换 ([e05e0de](https://github.com/continew-org/continew-starter/commit/e05e0de7b81329512ea1f0ad5e9ed3c04bdfe752))

### 🐛 问题修复

- 【security/mask】修复部分注释错误 ([34deff9](https://github.com/continew-org/continew-starter/commit/34deff959aa9ba817d05b552b173b4cbaebd289a))
- 【dependencies】指定 Apache POI 依赖版本（解决版本冲突）并移除冗余包 ([b4cb147](https://github.com/continew-org/continew-starter/commit/b4cb147a77cdfeb754c061eab888eb10314231be))

## [v2.13.0](https://github.com/continew-org/continew-starter/compare/v2.12.2...v2.13.0) (2025-07-05)

### ✨ 新特性

- 【excel/poi】新增 continew-starter-excel-poi 模块，并使用 FastExcel 替换 EasyExcel (Gitee#64@jiang4yu)
- 【api-doc】ApiDocUtils 新增 buildGroupedOpenApi（构建分组接口文档） 方法 ([08abe94](https://github.com/continew-org/continew-starter/commit/08abe94c85f098c3a797fa5b3255136654dc2720))
- 【extension/crud】新增 Api.BATCH_DELETE 批量删除枚举，拆分单个删除和批量删除接口 ([bc53d5b](https://github.com/continew-org/continew-starter/commit/bc53d5bfffda10ace055817f0249995296675ac1))
- 【json/jackson】添加对Instant、Duration的序列化和反序列化处理 (Gitee#68@jiang4yu) ([ffa484d](https://github.com/continew-org/continew-starter/commit/ffa484d9452c176489b77d1bea892b33d35a7019)) ([df9255c](https://github.com/continew-org/continew-starter/commit/df9255ca3de2452f0de68e4ae35282a1a7727b65))
- 【validation】新增校验模块并引入 SpEL Validator 用于复杂校验场景 ([5ae5b26](https://github.com/continew-org/continew-starter/commit/5ae5b2602aaa8e08c03fa8881c8109b0a1831966)) ([68f1f41](https://github.com/continew-org/continew-starter/commit/68f1f41cc991e81179f475111403d8917b17c55f))
- 【validation】新增 Phone 手机号校验注解，支持校验座机号码、手机号码（中国大陆）、手机号码（中国香港）、手机号码（中国台湾）、手机号码（中国澳门） ([fa7af8e](https://github.com/continew-org/continew-starter/commit/fa7af8e7b7eed91d2917688817885349a2965f16)) ([8d5d97d](https://github.com/continew-org/continew-starter/commit/8d5d97dcf6c102217aa9a8b745f891b388da8a10))
- 【extension/crud】CrudService 增加 Valid 校验注解，Controller 层使用 Valid 替换部分 Validated 注解 ([cce1b55](https://github.com/continew-org/continew-starter/commit/cce1b5560b3f468998d38609fc30a01ebcb9aa20))

### 💎 功能优化

- 【extension/crud】AbstractBaseController => AbstractCrudController，BaseService => CrudService ([a7d1e71](https://github.com/continew-org/continew-starter/commit/a7d1e71ab39652a93653e127bfe4a1c167342743))
- 【excel】file 模块重命名为 excel ([5a53d95](https://github.com/continew-org/continew-starter/commit/5a53d953da8d0bb158bd94820a45c7b26887328f))
- 【core】ProjectProperties => ApplicationProperties ([27dc229](https://github.com/continew-org/continew-starter/commit/27dc2294e6a90c2a6b0e95b58e816ab6cb5cce26))
- 【core】调整 CheckUtils 等校验类到 util.validation 包下 ([78a7904](https://github.com/continew-org/continew-starter/commit/78a7904c2fba7c18869afb3f089b23b79666a3a7))
- 简化 package，对于互斥包不再单独命名，例如：data.mp、data.mf，统一为 data ([223236a](https://github.com/continew-org/continew-starter/commit/223236aad31c32a8e2c49802dbd1f2f34a3ca275))
- 【data/mp】将 MP 的 CrudRepository 迁移至 ServiceImpl 类中，减少两层继承，解决层级过多出现 Sonar 警告的问题 ([e1c9a91](https://github.com/continew-org/continew-starter/commit/e1c9a91c77127a1f2a87f6c2effa813a82fc1877))

### 📦 依赖升级

- 调整 groupId top.continew => top.continew.starter ([a0ace7d](https://github.com/continew-org/continew-starter/commit/a0ace7d60c06b38a2535a5f48e3dbd1f87e9faa7))
- commons-beanutils 1.9.4 => 1.11.0 ([f84a1dd](https://github.com/continew-org/continew-starter/commit/f84a1dddfd27e6d4c1ddd3469866e01f6ddd2931))
- spring-boot 3.3.11 => 3.3.12 ([1d47cc6](https://github.com/continew-org/continew-starter/commit/1d47cc6f9ccf9c0ed6e02744e56063876f9a1200))
- redisson 3.46.0 => 3.49.0
- cosid 2.12.3 => 2.13.0
- sa-token 1.42.0 => 1.44.0
- mybatis-plus 3.5.8 => 3.5.12 ([1d47cc6](https://github.com/continew-org/continew-starter/commit/1d47cc6f9ccf9c0ed6e02744e56063876f9a1200)) ([8806eb9](https://github.com/continew-org/continew-starter/commit/8806eb9942df210788510489e24cc5636b0fb77a))
- mybatis-flex 1.10.8 => 1.10.9
- snail-job 1.4.0 => 1.5.0
- sms4j 3.3.4 => 3.3.5
- aws-sdk 2.31.35 => 2.31.63
- aws-crt 0.36.1 => 0.38.1
- hutool 5.8.37 => 5.8.38
