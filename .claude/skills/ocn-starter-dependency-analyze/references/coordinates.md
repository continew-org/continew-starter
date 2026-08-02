# 坐标目录（Coordinate Catalog）

本文件是 `scan.py` 内置映射（`COORDINATES` 字典）的人工可读副本，也是各坐标「升级约束/迁移要点」的唯一维护面。
**POM 加新依赖必须同步本文件 + `scan.py` 的 `COORDINATES`。** 漏了仍能分级，只是报告里该坐标「升级要点」为空。
当前锁定版本以 POM 为准（`scan.py` 实时读 `<properties>`），本文件不写死版本号。

每条字段：**property**（POM `<properties>` 里的版本 property 名）· **坐标**（`groupId:artifactId`）·
**metadata 路径**（artifactId 大小写须原样）· **升级约束**（联动/互斥/特殊行为）· **迁移要点**（🔴 大版本或已知
破坏性 minor 的核验清单，日常补丁留空）。

---

## 平台级（联动升级，最敏感）

### spring-boot
- property: `spring-boot.version` · 坐标: `org.springframework.boot:spring-boot-dependencies`（BOM）
- metadata: `org/springframework/boot/spring-boot-dependencies/maven-metadata.xml`
- **约束**：与 `spring-cloud.version` 强联动（见 special-cases.md）。升 Boot 必须确认 Cloud release train 兼容该
  Boot 版本区间，否则禁止单独升 Boot。
- **迁移到 4.x 要点**：Jakarta EE 基线、`@ConfigurationProperties` 绑定变更、移除的废弃 API、Spring Security 7 联动
  （若用到）。4.0 起多个 starter 的 auto-config 类路径/条件有变，必须全 reactor `mvn compile` + 人工跑 demo 验证。
  这是本仓库最高风险升级，逐 starter 核验。

### spring-cloud
- property: `spring-cloud.version` · 坐标: `org.springframework.cloud:spring-cloud-dependencies`（BOM）
- metadata: `org/springframework/cloud/spring-cloud-dependencies/maven-metadata.xml`
- **约束**：release train 名（如 `2025.0.x`）对应特定 Spring Boot 版本区间，升 Cloud 前先查官方兼容矩阵，不可与
  Boot 版本错配（矩阵见 special-cases.md §1）。

---

## ORM / 数据访问

### mybatis-plus
- property: `mybatis-plus.version` · 坐标: `com.baomidou:mybatis-plus-bom`（BOM）
- metadata: `com/baomidou/mybatis-plus-bom/maven-metadata.xml`
- **约束**：与 `data-mp` 强耦合（CRUD 实现基于 MP）。auto-config 收集 `InnerInterceptor` bean 的 seam 对 MP 内部 API
  敏感，跨 minor 看 MP changelog 有无 `InnerInterceptor`/`MybatisMapperRegistry` 变更。

### mybatis-flex
- property: `mybatis-flex.version` · 坐标: `com.mybatis-flex:mybatis-flex-dependencies`（BOM）
- metadata: `com/mybatis-flex/mybatis-flex-dependencies/maven-metadata.xml`
- **约束**：与 `data-mp` 互斥实现（二选一）。`data-mf` 内有独立数据权限机制，升 Flex 看其数据权限 SPI 有无签名变更。

### dynamic-datasource
- property: `dynamic-datasource.version` · 坐标: `com.baomidou:dynamic-datasource-spring-boot3-starter`
- metadata: `com/baomidou/dynamic-datasource-spring-boot3-starter/maven-metadata.xml`
- **约束**：注意 `spring-boot3` 后缀，别误抓 `spring-boot-starter`（Boot 2）的 metadata。

### p6spy
- property: `p6spy.version` · 坐标: `p6spy:p6spy`（groupId 即 artifactId）
- metadata: `p6spy/p6spy/maven-metadata.xml`

---

## 认证 / 授权

### sa-token
- property: `sa-token.version` · 坐标: `cn.dev33:sa-token-bom`（BOM，实际构件 sa-token-spring-boot3-starter）
- metadata: `cn/dev33/sa-token-bom/maven-metadata.xml`
- **约束**：property 锁 BOM，但运行时构件是 `sa-token-spring-boot3-starter`（Boot 3 专用）。勿抓父 `sa-token`
  （无 `-spring-boot3`）的 metadata，那是旧聚合 POM，latest 停在 1.6.0 会误导。

### justauth
- property: `justauth.version` · 坐标: `me.zhyd.oauth:JustAuth`（artifactId 大小写敏感，首字母大写）
- metadata: `me/zhyd/oauth/JustAuth/maven-metadata.xml`
- **约束**：与 sa-token 互补（第三方 OAuth 登录），非互斥。artifactId 的 `J` 大写，路径里也大写。

---

## 缓存

### redisson
- property: `redisson.version` · 坐标: `org.redisson:redisson-spring-boot-starter`
- metadata: `org/redisson/redisson-spring-boot-starter/maven-metadata.xml`
- **约束**：缓存层地基，Spring Cache 和 JetCache 都建在它上面。升 Redisson 看 `RedissonAutoConfiguration` 的 bean
  是否被 data/web 模块 `@ConditionalOnMissingBean` 依赖。

### jetcache
- property: `jetcache.version` · 坐标: `com.alicp.jetcache:jetcache-bom`（BOM）
- metadata: `com/alicp/jetcache/jetcache-bom/maven-metadata.xml`
- **约束**：建在 redisson starter 之上，与 Spring Cache 互补。

---

## Excel / 文档

### fastexcel
- property: `fastexcel.version` · 坐标: `cn.idev.excel:fastexcel`
- metadata: `cn/idev/excel/fastexcel/maven-metadata.xml`
- **约束**：与 `poi` 互斥实现之一（`excel-fastexcel` vs `excel-poi`）。CRUD core 显式依赖 FastExcel。FastExcel 脱胎于
  EasyExcel，API 跨 minor 偶有 breaking，看其 release notes。

### poi
- property: `poi.version` · 坐标: `org.apache.poi:poi`（`poi-ooxml` 同 property）
- metadata: `org/apache/poi/poi/maven-metadata.xml`
- **约束**：`excel-poi` 互斥实现。POI 大版本（5→6）会动 OOXML schema 生成代码。

### nextdoc4j
- property: `nextdoc4j.version` · 坐标（BOM）: `top.nextdoc4j:nextdoc4j-bom`
- 运行时 starter: `top.nextdoc4j:nextdoc4j-spring-boot-starter`
- metadata（BOM）: `top/nextdoc4j/nextdoc4j-bom/maven-metadata.xml`
- **约束**：1.4.x 起坐标改名——`nextdoc4j-bom-springboot3` → `nextdoc4j-bom`，
  `nextdoc4j-springboot3-starter` → `nextdoc4j-spring-boot-starter`；旧 `springboot3`
  后缀坐标已废弃。升级时务必同步改 dependencies POM 与各 starter 的 `<dependency>`，
  并核对新版本是否把 springdoc-openapi 拆为新增传递依赖（本仓库 api-doc 模块已显式补该依赖）。
- **破坏性 minor 核验清单**：对照 `changelog_fetch.py --group top.nextdoc4j --artifact nextdoc4j-spring-boot-starter`
  的 compare API removed/renamed 文件列表，确认本仓库 import 的 nextdoc4j 类无缺失。

---

## 文件存储

### x-file-storage
- property: `x-file-storage.version` · 坐标: `org.dromara.x-file-storage:x-file-storage-spring`
- metadata: `org/dromara/x-file-storage/x-file-storage-spring/maven-metadata.xml`

### aws-sdk-v1
- property: `aws-sdk-v1.version` · 坐标: `com.amazonaws:aws-java-sdk-s3`
- metadata: `com/amazonaws/aws-java-sdk-s3/maven-metadata.xml`
- **约束**：AWS SDK v1 已进入维护模式（仅安全补丁），新功能在 v2。建议长期迁移到 `aws-sdk`（v2），短期 v1 升级仅取
  安全补丁。

### aws-sdk
- property: `aws-sdk.version` · 坐标: `software.amazon.awssdk:bom`（artifactId 字面量就是 `bom`）
- metadata: `software/amazon/awssdk/bom/maven-metadata.xml`
- **约束**：与 `aws-crt` 配套（CRT 是 v2 的性能组件）。

### aws-crt
- property: `aws-crt.version` · 坐标: `software.amazon.awssdk.crt:aws-crt`
- metadata: `software/amazon/awssdk/crt/aws-crt/maven-metadata.xml`

### thumbnails
- property: `thumbnails.version` · 坐标: `net.coobird:thumbnailator`
- metadata: `net/coobird/thumbnailator/maven-metadata.xml`

---

## 任务 / 调度 / 通信

### cosid
- property: `cosid.version` · 坐标: `me.ahoo.cosid:cosid-bom`（BOM）
- metadata: `me/ahoo/cosid/cosid-bom/maven-metadata.xml`
- **约束**：分布式 ID 生成，与数据层耦合。

### snail-job
- property: `snail-job.version` · 坐标: `com.aizuda:snail-job-client-starter`（retry-core/job-core 同 property）
- metadata: `com/aizuda/snail-job-client-starter/maven-metadata.xml`
- **约束**：三个构件共用一个 property，升一个就升全部。

### sms4j
- property: `sms4j.version` · 坐标: `org.dromara.sms4j:sms4j-spring-boot-starter`
- metadata: `org/dromara/sms4j/sms4j-spring-boot-starter/maven-metadata.xml`

### paho-mqttv3
- property: `paho-mqttv3.version` · 坐标: `org.eclipse.paho:org.eclipse.paho.client.mqttv3`（artifactId 含点）
- metadata: `org/eclipse/paho/org.eclipse.paho.client.mqttv3/maven-metadata.xml`
- **约束**：artifactId 含点号，metadata 路径里点号原样保留。

---

## 验证码

### aj-captcha
- property: `aj-captcha.version` · 坐标: `com.anji-plus:captcha`
- metadata: `com/anji-plus/captcha/maven-metadata.xml`

### easy-captcha
- property: `easy-captcha.version` · 坐标: `com.github.whvcse:easy-captcha`
- metadata: `com/github/whvcse/easy-captcha/maven-metadata.xml`

---

## Web / API 增强

### graceful-response
- property: `graceful-response.version` · 坐标: `com.feiniaojin:graceful-response`
- metadata: `com/feiniaojin/graceful-response/maven-metadata.xml`
- **约束（版本编号异常）**：`5.x-boot3` 是 Boot 3 专用线，`3.x` 是另一条线，且 `5.0.5-boot3` 比 Central release
  还新。`scan.py` 标为 `downgrade`（锁定比 release 新），报告单独提示「疑似项目重新编号或坐标指向不同分支」，**不
  当升级处理、不给应用按钮**。人工核验该坐标是否需要换 artifact 再决定。

### spel-validator
- property: `spel-validator.version` · 坐标: `cn.sticki:spel-validator-jakarta`
- metadata: `cn/sticki/spel-validator-jakarta/maven-metadata.xml`

### crane4j
- property: `crane4j.version` · 坐标: `cn.crane4j:crane4j-spring-boot-starter`
- metadata: `cn/crane4j/crane4j-spring-boot-starter/maven-metadata.xml`

### swagger
- property: `swagger.version` · 坐标: `io.swagger.core.v3:swagger-annotations-jakarta`
- metadata: `io/swagger/core/v3/swagger-annotations-jakarta/maven-metadata.xml`
- **约束**：API 文档注解，与 springdoc 版本（由 spring-boot-dependencies 管）需匹配。

### tlog
- property: `tlog.version` · 坐标: `com.yomahub:tlog-web-spring-boot-starter`
- metadata: `com/yomahub/tlog-web-spring-boot-starter/maven-metadata.xml`
- **约束**：与 `log-aop`/`log-interceptor` 是不同维度（tlog 是链路追踪），非互斥。

---

## 工具类 / 杂项

### hutool
- property: `hutool.version` · 坐标: `cn.hutool:hutool-bom`（BOM，hutool-all 同 property）
- metadata: `cn/hutool/hutool-bom/maven-metadata.xml`
- **约束**：core 模块重度依赖 hutool-all。Hutool 6.x 是大版本重构（包名/模块化有变），🔴 跨到 6 需全 reactor 核验。

### snakeyaml
- property: `snakeyaml.version` · 坐标: `org.yaml:snakeyaml`
- metadata: `org/yaml/snakeyaml/maven-metadata.xml`
- **约束**：通常由 spring-boot-dependencies 管，这里显式锁是为避开 CVE/控制版本。升 Boot 时留意是否冲突
  （见 special-cases.md §7）。

### nashorn
- property: `nashorn.version` · 坐标: `org.openjdk.nashorn:nashorn-core`
- metadata: `org/openjdk/nashorn/nashorn-core/maven-metadata.xml`

### commons-fileupload
- property: `commons-fileupload.version` · 坐标: `commons-fileupload:commons-fileupload`（groupId 即 artifactId）
- metadata: `commons-fileupload/commons-fileupload/maven-metadata.xml`

### commons-beanutils
- property: `commons-beanutils.version` · 坐标: `commons-beanutils:commons-beanutils`
- metadata: `commons-beanutils/commons-beanutils/maven-metadata.xml`

### commons-io
- property: `commons-io.version` · 坐标: `commons-io:commons-io`
- metadata: `commons-io/commons-io/maven-metadata.xml`

### commons-compress
- property: `commons-compress.version` · 坐标: `org.apache.commons:commons-compress`
- metadata: `org/apache/commons/commons-compress/maven-metadata.xml`

### truelicense
- property: `truelicense.version` · 坐标: `de.schlichtherle.truelicense:truelicense-core`
- metadata: `de/schlichtherle/truelicense/truelicense-core/maven-metadata.xml`

### zip4j
- property: `zip4j.version` · 坐标: `net.lingala.zip4j:zip4j`
- metadata: `net/lingala/zip4j/zip4j/maven-metadata.xml`

### ttl
- property: `ttl.version` · 坐标: `com.alibaba:transmittable-thread-local`
- metadata: `com/alibaba/transmittable-thread-local/maven-metadata.xml`

### ip2region
- property: `ip2region.version` · 坐标: `net.dreamlu:mica-ip2region`
- metadata: `net/dreamlu/mica-ip2region/maven-metadata.xml`

---

## 构建 / 工具（Maven 插件）

插件与普通构件**同样检测、同样分级、同样可一键应用**，property 在同一个 `<properties>` 块里，改法完全一致。

### flatten
- property: `flatten.version` · 坐标: `org.codehaus.mojo:flatten-maven-plugin`
- **约束**：绑定 `process-resources` 生成发布用（flattened）POM。升级后须确认 flattened POM 内容与升级前一致，
  避免影响已发布产物的依赖解析。

### spotless
- property: `spotless.version` · 坐标: `com.diffplug.spotless:spotless-maven-plugin`
- **约束**：绑定 `compile` 阶段，**会改写源码**（P3C 格式化 + license header + 去无用 import）。升级后跑
  `mvn spotless:check`，确认格式化结果没有大面积漂移，否则会污染 diff。

### sonar
- property: `sonar.version` · 坐标: `org.sonarsource.scanner.maven:sonar-maven-plugin`
- **约束**：仅 `-Psonar` profile 使用，不影响主构建。版本号为**四段式**（`5.2.0.4988`），比较时必须保留第四段。
