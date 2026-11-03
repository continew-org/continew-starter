# 3.x 自动化测试体系设计方案

> 状态：阶段 0 + 阶段 1 实施中
> 适用版本：3.1.0（开发中）
> 执行角色：dev-team 主执行，qa-team 设计
> 最后更新：2026-11-03
> 参照物：continew-admin `testing-system-5x.md`（四层体系），本项目为多模块 Starter 库，不能照搬，见第 2 节

## 1. 目标

把 continew-starter 的测试从「17/46 模块有单测（53 类、317 用例，全部 Mock/内存 Fake）」推进到分层体系：

1. T1 单元测试：纯逻辑，毫秒级反馈（已有基础，保留）。
2. T2 自动配置契约测试：证明条件装配、属性绑定、fallback、imports 注册规则（本方案核心增量）。
3. T3 基础设施集成测试：Testcontainers 真实 Redis/MySQL，验证协议语义。
4. T4 消费方冒烟测试：组合依赖多个 Starter 后能启动、能装配、能响应 HTTP。
5. T5 UI E2E：不适用（无前端），不做。

硬约定：**默认 `./mvnw verify` 零 Docker 依赖**；T3/T4 全部走 `-Pintegration`。这是 Windows 无 Docker 开发者与 PR 反馈时间的保障。

## 2. 与 continew-admin 的对照

| admin 做法 | 结论 | 说明 |
|---|---|---|
| L1 Mockito 单元 | 可抄 | 已有 317 用例是基础 |
| 容器单例 + `@DynamicPropertySource` + `disabledWithoutDocker` | 可抄 | 实现下沉到 test-support |
| L3 REST Assured 打 compose 实例 | 不可抄 | starter 无应用可编排；等价物是 T2 契约 + T4 冒烟，用 Failsafe `*IT` + `-Pintegration` 替代 `@Tag("api")` 开关 |
| `ApiTestSupport`（RSA 登录、业务断言） | 不可抄 | 绑定 admin 用户体系；starter 只断言框架契约 |
| `scripts/test/*.sh` + compose | 不抄 | 多模块各自起容器，交给 Testcontainers |
| admin 缺口（无 Failsafe、无覆盖率门槛、无 test 资源约定） | 不继承 | 阶段 0 一次性补齐 |

## 3. 模块落位

| 模块 | 内容 | 发布语义 |
|---|---|---|
| `continew-starter-test-support` | 容器基座、`@EnabledIfDockerAvailable` 用法基类、imports 完整性断言工具 | 只在聚合 POM 注册，不进 BOM，`maven.deploy.skip=true`，release profile `excludeArtifacts` |
| `continew-starter-smoke-tests` | 消费方冒烟应用（S1 起步，S2-S5 阶段 2） | 同上 |
| 其余 46 个模块 | T1/T2/T3 就近测试 | 不变 |

先例（2026-11-03 核实）：Spring Boot 仓库根 `test-support/`（`spring-boot-test-support` 等 3 个模块）与 `smoke-test/`（约 90 个冒烟应用）、Kafka 根 `tests/`、Dubbo `dubbo-test/` 均为内部测试模块，Maven Central 检索 `spring-boot-test-support`（`org.springframework.boot` 坐标）、`kafka-tests`、`dubbo-test-common` 命中数均为 0。

AGENTS.md 同步增加豁免条款：「仅测试用途、artifactId 以 `-test-support` 或 `-tests` 结尾的模块，只在聚合 POM 注册，不进 BOM，必须设置 deploy 跳过并在 release profile 的 `excludeArtifacts` 中排除」。

## 4. T2 契约测试规范

每个含 `@AutoConfiguration` 的模块必须有一个契约测试类（当前 33 个 `AutoConfiguration.imports` 文件）。固定断言清单，按模块裁剪：

1. imports 完整性：classpath 上可见的 imports 资源包含本模块包内扫描到的全部 `@AutoConfiguration` 类。
2. `enabled=true`：关键 bean `hasSingleBean(公开类型)`，不断言实现类与 bean 名。
3. `enabled=false` 或缺条件：`hasNotFailed()` + `doesNotHaveBean(...)`，确认无副作用 bean。
4. 属性绑定：默认值与覆盖值两组，预期来源是 `docs/reference/properties.mdx` 与 `@ConfigurationProperties` 默认值。
5. fallback：`withBean(...)` 后 `hasSingleBean` 仍是用户实例。
6. 可选依赖缺失：仅 `@ConditionalOnClass` 模块用 `FilteredClassLoader`。

模板要点：`ApplicationContextRunner` / `WebApplicationContextRunner`，不用 `@SpringBootTest`；类级 Javadoc + `@author` + `@since 3.1.0` + LGPL Header；新测试类放各模块 `src/test` 的 `autoconfigure` 对应包下。

## 5. T3/T4 技术选型（阶段 2 实施）

| 用途 | 选择 | 依据（2026-11-03 实测 Boot 4.1.1 BOM） |
|---|---|---|
| 容器 | Testcontainers 2.0.5（经 `testcontainers-bom` 管理） | core/junit-jupiter 字节码 major=52（Java 8），过 Enforcer `maxJdkVersion 17` |
| Docker 跳过 | `org.testcontainers.junit.jupiter.EnabledIfDockerAvailable`（TC 2.x 原生） | 不自研条件 |
| MySQL 容器 | `org.testcontainers.mysql.MySQLContainer`（2.x 正式包，`containers` 包为弃用别名） | |
| Redis 容器 | `GenericContainer` + redis 镜像常量 | |
| HTTP 冒烟 | TestRestTemplate + AssertJ | rest-assured 不在 Boot 4.1.1 BOM 管理内，不引入 |
| 等待 | Awaitility 4.3.0（BOM 管理） | 禁止裸 sleep |

镜像常量集中在 test-support：`redis:7.2-alpine`、`mysql:8.0`，升级只改一处。

## 6. 构建与 CI

1. `continew-starter-dependencies/pom.xml`：`pluginManagement` 增加 Failsafe（版本对齐 surefire 3.5.6），默认不绑定执行。
2. 根 `pom.xml`：`<modules>` 增加 2 个测试模块；新增 `integration` profile（绑定 Failsafe `integration-test` + `verify`，`includes=**/*IT.java`）与 `coverage` profile（独立声明 jacoco `prepare-agent` + `report`；`sonar` profile 保持原样不动，二者并存）。
3. 两个测试模块 POM：`maven.deploy.skip=true`；release profile 的 `central-publishing-maven-plugin` 增加 `excludeArtifacts`（deploy 阶段被该插件 extensions 接管，仅 deploy.skip 不够）。
4. `ci.yml` 不动。新增 `integration.yml`：PR 到 dev、push dev、`workflow_dispatch`、每周 schedule；执行 `./mvnw -B verify -Pintegration -Pcoverage`；上传 surefire/failsafe XML 与 JaCoCo 报告（保留 7 天）；`timeout-minutes: 30`；并校验 failsafe 报告 `tests` 总数不低于下限，防止静默全跳过。首版不加 `-T` 与 paths 过滤。
5. 覆盖率门禁放阶段 3：Sonar 新代码 60%，关键模块叠加 `jacoco:check`。

## 7. 分阶段计划

| 阶段 | 目标 | 验收 | 用例下限 |
|---|---|---|---|
| 0 基础设施（本 PR） | 2 个测试模块、Failsafe、双 profile、CI workflow、AGENTS.md 豁免 | `./mvnw -B verify` 无 Docker 全绿；`./mvnw -B verify -Pintegration` 真跑不 skipped（CI）；构件清单不变 | 2 个 IT（Redis ping、MySQL `select 1`）+ 1 个冒烟场景 |
| 1 契约层（本 PR） | 33 个 imports 模块契约测试 | 每个含 `@AutoConfiguration` 的模块至少一个契约类；默认构建不显著变慢 | ≥60 方法 |
| 2 集成与冒烟（后续 PR） | 关键链路真实资源证据 | 刷新令牌轮换、租户隔离、数据权限、字段加密、多级缓存有证据 | ≥25 IT + S2-S5 冒烟 |
| 3 扩展与门禁（后续 PR） | P1 补齐、覆盖率可守 | 新代码覆盖率 ≥60%；flaky <2% | P1 模板化补齐 |

明确不测：Excel 像素与文件兼容（只测读写往返）、License 证书链全组合（只测生成/校验/篡改）、api-doc 页面渲染（只测 JSON 可生成）、WebSocket 浏览器兼容（只测 session 注册与清理）、Captcha 识别准确率（只测生成/校验/过期/一次性）、各云厂商 S3 真机（MinIO 代表，阶段 2）。

## 8. 对抗评审记录（2026-11-03）

| # | 原案 | 评审发现（实测） | 裁定 |
|---|---|---|---|
| R1 | BOM 管理 rest-assured | `spring-boot-dependencies-4.1.1.pom` 无 rest-assured 管理 | 不引入，冒烟用 TestRestTemplate |
| R2 | `@EnabledIfDockerAvailable` 需自研 | TC 2.0.5 原生自带 | 用官方注解 |
| R3 | TC 2.0 字节码可能超 17 | core/junit-jupiter major=52 | 通过，阶段 0 validate 复核 |
| R4 | `MySQLContainer` 在 `containers` 包 | 2.x 正式包 `org.testcontainers.mysql` | 用新包 |
| R5 | deploy.skip 即可 | release 走 central-publishing extensions | 加 `excludeArtifacts` 双保险 |
| R6 | CI `-T 4` | 并行容器内存风险 | 首版不加 |
| R7 | integration.yml paths 过滤 | 漏触发风险 | 首版全触发 |
| R8 | coverage profile 拆出需改 sonar profile | 危及 Sonar 链路 | 双 profile 并存，sonar 不动 |
| R9 | 「44 个构件」 | 数字无出处 | 删除，改为发布前构件清单比对 |
| R10 | 契约断言含 bean 名 | 耦合实现 | 只断言公开类型 |
| R11 | 冒烟用 REST Assured | 不在 BOM 内 | TestRestTemplate |
| R12 | 并行代理各跑 mvnw | 同仓 target 与 ~/.m2 下载竞态 | 先 `install -DskipTests` 预热，代理跑 `-pl :模块 test -o` |
| R13 | `@since` 取 3.0.0 | 3.0.0 已发版 | 新文件统一 `@since 3.1.0` |
| R14 | AGENTS.md:81 「67 用例」 | 实际 72，漂移 | 本 PR 修正 |
| R15 | 镜像 tag 各处散写 | 升级困难 | 集中 test-support 常量 |

## 9. 风险与回滚

| 风险 | 缓解 | 回滚 |
|---|---|---|
| 测试变慢卡 PR | T3/T4 独立 workflow | 关闭 integration workflow，保留本地 `-Pintegration` |
| Testcontainers 环境不可用 | `EnabledIfDockerAvailable` 跳过 + 执行数下限校验 | 该模块降级 Mock，报告标注「未验证」 |
| 新模块误发布 Central | `excludeArtifacts` + deploy.skip + 发布前构件清单比对 | Central 不可撤回，必须前置校验 |
| 测试代码过不了 Checkstyle/Spotless | 模板预置 Header、Javadoc、`@author`、`@since`；`-Pformat` 后重跑 verify | 无回滚，必须修代码 |
| 为变绿弱化断言 | 评审清单禁止空断言、放宽 expected、无故 skip | 评审打回 |
| 测试模块与 AGENTS 三处注册冲突 | 豁免条款入 AGENTS.md | 若不采纳，改为各模块复制基类，成本约 +30% |
