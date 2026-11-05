---
name: ocn-starter-release
description: >
  continew-starter 仓库发版技能：预检 → CHANGELOG → release commit → ./mvnw deploy 推 Maven Central →
  打 tag+push → GitHub/Gitee/AtomGit 三平台 release → 切维护分支。版本号不手动传：从
  continew-starter-dependencies/pom.xml 的 <revision> 自动推导发版号（去掉 -SNAPSHOT）。
  用户说"发版"、"发布新版"、"release"、"准备发版"、"tag"、"deploy 到 Central"、"新建 GitHub Release"、
  "打 tag"、"切维护分支"，或想看发版流程清单时使用。
  专用于 continew-starter 仓库，不适用其它 Maven 项目。发版前自验（./mvnw install + continew-admin
  业务侧验证）由用户自行完成，skill 不做。
---

# continew-starter 发版

## 铁律

- **不可逆操作（./mvnw deploy / git push / 建 release）执行前必须展示命令，等用户确认。**
- 版本号不传参，全部由 LLM 从 `<revision>` 推导并代入（后面所有命令都用这些变量）：

  | 变量 | 含义 | 推导 |
  |:--|:--|:--|
  | `${NEW_VERSION}` | 发版号 | `<revision>` 去掉 `-SNAPSHOT`（如 `X.Y.Z-SNAPSHOT` → `X.Y.Z`） |
  | `${PREV_TAG}` | 上一个 tag | 本地无新增 tag 时取本地最新；否则比对 `git ls-remote --tags origin` 取远端最新发布 tag（维护分支 tag 不在 dev 上） |
  | `${NEW_TAG}` | 本次 tag | `v${NEW_VERSION}` |
  | `${MAINT_BRANCH}` | 维护分支 | `${NEW_VERSION}` 的 `major.minor` + `.x` |
  | `${TODAY}` | 发版日期 | 当天 `YYYY-MM-DD` |

- **三平台 release 内容一致**：GitHub / Gitee / AtomGit 都放同一份 CHANGELOG 段。
- 发版后 dev 保持 `<revision>=${NEW_VERSION}`（不回 SNAPSHOT）；下个开发周期开始时由用户手动
  `build: 更新项目版本号至<major>.<minor+1>.0-SNAPSHOT`。
- **维护分支修复必须即时合回 dev**：维护分支（x.y.x）上合入的每个 fix，合并时立即 cherry-pick
  （或按 dev 现状重写）到 dev，不要攒到发版前。2.16.x 的教训：SQL 注入修复（4cd39db8）只留在
  2.16.x，dev 靠手工重写（8cf81964）补回但 release commit（0681dc6f）的 CHANGELOG 段丢失，
  导致 2.17.0 发版时 CHANGELOG 断档、依赖升级清单以错误基准（2.16.0 而非 2.16.1）生成。

## 流程

### 1. 预检（任一失败即停）

| 检查 | 命令 | 失败时 |
|:--|:--|:--|
| 在 dev 分支 | `git branch --show-current` | `git checkout dev` |
| 工作区只有发版文件 | `git status --short` | 多余改动先 commit / stash |
| `<revision>` 是 SNAPSHOT | `grep '<revision>[0-9.]*-SNAPSHOT' continew-starter-dependencies/pom.xml` | 手动改 pom |
| bom/dependencies 两处 revision 一致 | `grep '<revision>' continew-starter-bom/pom.xml continew-starter-dependencies/pom.xml` | 不一致会导致反应堆解析错位（2.17.0 发版教训：bom 残留 2.16.0，全反应堆按旧版本编译） |
| 上一个发布 tag 已 fetch 且有提交 | `git fetch origin --tags`，比对本地与远端 tag 取最新为 ${PREV_TAG}，`git log ${PREV_TAG}..HEAD --oneline` | 区间空则停 |
| 四道静态门禁与单元测试全过 | `./mvnw verify` | 先修到全过（Enforcer → Spotless → Checkstyle → 单测 → SpotBugs，见 AGENTS.md） |
| 装配契约测试与自动配置模块零漂移 | `python -I scripts/check-contract-tests.py` | 新增/移除模块未同步契约测试，按 3.1.0 门禁固化要求补齐（见 #14） |
| 配置参考与生成 metadata 无漂移 | `python -I scripts/check-doc-properties.py`（需先 `./mvnw compile -DskipTests` 生成 metadata） | 先补 properties.mdx 缺失键或清理失效键 |
| 签名密钥未临近过期 | `gpg --list-secret-keys` 看 `expires` 列 | 快过期先续期（`--quick-set-expire`）并 `--send-keys` 重传公钥，否则 deploy 签名失败 |

> 发版文件只有 6 类：`CHANGELOG.md` / `README.md` / `continew-starter-*/pom.xml` /
> `ContiNewStarterVersion.java` / `.gitignore` / `.github/ISSUE_TEMPLATE/*.yml`。
> 不检查：origin/dev 领先落后、Central 凭证（deploy 时构建工具自己会报）。

通过后展示并确认：

```
发版号: ${NEW_VERSION}   上一个 tag: ${PREV_TAG}   维护分支: ${MAINT_BRANCH}   日期: ${TODAY}
继续？[Enter 继续 / n 退出]
```

### 2. CHANGELOG 段

```bash
git log ${PREV_TAG}..HEAD --format='%h %aI %an %s'
```

按前缀分桶，**桶内按时间倒序**：

| commit 前缀 | 归到 | 说明 |
|:--|:--|:--|
| `feat` | ✨ 新特性 | |
| `fix` | 🐛 问题修复 | |
| `refactor` / `perf` / `chore` / `style` | 💎 功能优化 | chore/style 默认进，校对时删纯 housekeeping |
| `build(dependencies): X a.b.c => x.y.z` | 📦 依赖升级 | 按 pom property 顺序排；`X <= Y` 且 X<Y（pin）跳过、X>Y（降级）进；只列安全修复或使用者可感知的升级，纯 bump 不列 |
| 带 `!` 前缀或 `BREAKING CHANGE` 脚注 | ⚠️ 破坏性变更 | 主体条目照常按 feat/fix 归桶，另汇总进段顶「破坏性变更」小节 |
| `docs` / `ci` / `test` / 其它 `build:` | 跳过 | |

⚠️ 破坏性变更条目写两处：常规桶照常收录，另在版本段顶部单列「破坏性变更」小节；3.0.0 这类大版本段不可省略该小节。小节内按影响面排序：底座升级 → 全局行为 → 模块级。

条目面向升级者撰写，不照抄 commit subject：未随已发布版本到达用户的中间态不写（同版本内引入又移除的桥接层、过渡开关）；同一功能在同版本内的引入与后续修复合并为一条，同主题的治理提交也合并，成员链接全保留；修复已发布版本缺陷的 hotfix 必须单列。关键迁移动作写成行内一句话，不内链 docs/ 页面（文档重组会让历史段链接腐烂）。

行格式：`- 【scope】描述 ([#PR](pr_url)) @GitHub用户名`，无 PR 的直推提交链接 commit；作者取 GitHub 账号名（noreply email 反推，`gh api users/<name>` 验证），不用 git author name。

写入后做机器对账校验（2.17.0 断档教训，人工校对会失手），防止破坏性提交漏收。条目允许合并，校验口径是 hash 全覆盖对账而非计数等值：

```bash
git log ${PREV_TAG}..HEAD --format='%h %s' | grep '!' | cut -d' ' -f1   # 带 ! 的提交清单，每个 hash 都须在小节内有对应条目（合并条目的成员也算覆盖）
git log ${PREV_TAG}..HEAD --grep='BREAKING CHANGE' --format='%h %s'     # 输出的每个 hash 同样逐条对账
```

比对时在段内文本检索 `[hash]` 或 `[#PR]`（链接形态随条目：有 PR 链 PR，无 PR 链 commit）；任一 hash/PR 号出现在小节之外即有漏收，补齐后重新对账。含中文文本的检索建议用 `python -I`（git bash 的 grep 对中文文件不可靠）。带 `BREAKING CHANGE` 脚注的提交一般也带 `!`。

**写入前硬性校验（2.17.0 教训）**：每个写进 CHANGELOG 的提交 hash 必须真实存在于当前 dev——
逐条 `git cat-file -t <hash>` 或 `git merge-base --is-ancestor <hash> HEAD` 验证；用户校对时删掉的条目，
发版全程不得再补回（release notes、Gitee/GitHub release body 同步以用户校对版为唯一事实源）。

组装成段 prepend 到 `CHANGELOG.md` 顶部（不动 `CHANGELOG_1.x.x.md` / `CHANGELOG_2.0.0-2.12.2.md` 老归档），
`git diff CHANGELOG.md` 给用户校对。

README 徽章随发版切换：`badge/SNAPSHOT-vX.Y.Z` → `badge/RELEASE-v${NEW_VERSION}`（dev 进入下周期时再改回 SNAPSHOT）。

### 3. release commit

版本号替换点：两处 `<revision>`、`ContiNewStarterVersion.java` 的 `VERSION` 常量、issue 模板版本示例、README 徽章（见第 2 步）。**必须用二进制替换（`rb` 读、只 replace 版本串、`wb` 写）**：文本模式的 universal newline 会把工作区文件 LF 化，Spotless 期望与仓库存储不一致时第一个模块即拦停（3.0.0 实测，deploy 失败零上传）。

- `git add` 上述 6 类发版文件（README 有改动才加）→ `git commit -m "release: v${NEW_VERSION}"`。

### 4. ./mvnw clean deploy -Prelease,gpg（最危险，单独确认）

推全部模块的 jar + sources + javadoc + .asc 签名到 Maven Central。成功标志：`Uploaded bundle successfully, deploymentId: …` + `has been validated`；构建中途失败 = 零上传（0.12.0 构建完成后整体打包上传），修复后安全重跑；网页 Publish 后才真正生效且不可撤回。
签名（`maven-gpg-plugin`）与发布插件都内置在 `release` profile（continew-starter-dependencies/pom.xml），POM 无 `gpg` profile；签名所需的 gpg 可执行文件与口令由使用者本机 settings.xml 的 `gpg` profile 提供，命令显式带 `gpg` 以兼容未常驻激活的环境。

> deploy 成功后 bundle 只是上传，尚未发布：`central-publishing-maven-plugin`（0.12.0，未开启
> autoPublish）把 bundle 放入部署队列，需到 [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)
> 手动 Publish 后才会出现在 Maven Central；校验不通过等失败也在此页面查看状态与原因。

### 5. tag + push（一次确认）

```bash
git tag -a ${NEW_TAG} -m "release: v${NEW_VERSION}"
git push origin dev
git push origin ${NEW_TAG}
```

（不用 `git push --tags`，避免误推其它本地 tag。）

github.com 连接 reset / 空响应时改走 gh API 重建远端 commit：blob 从 `git cat-file` 对象库取，`POST /git/trees` 用 JSON body 传 base_tree + 条目，最后 `PATCH /git/refs/heads/dev`；交付总结记录 local/remote SHA 对应关系（tree 应一致）。

### 6. 三平台 release（GitHub 确认一次，Gitee+AtomGit 合并确认一次）

> **前置**：`gh run list --workflow=mirror.yml` 确认 push 触发的镜像 run 已 completed/success（mirror.yml 在每次 push 时自动同步 dev / `3.0.x` 形态维护分支 / 全部 tag 到 Gitee 与 AtomGit，GitHub 是唯一写入点，**不需要也不应该手动向镜像平台 push 代码**）。tag 有时序缺口：tag push 晚于 dev push 时，要等下一次 push 触发，或手动 workflow_dispatch 跑一次 mirror。

> 凭证一律从本机环境变量取，不写入仓库任何文件；工具名随宿主而异，**不要假定某个 MCP 工具一定存在**，
> 有对应工具就用，没有就走下面的等价命令。宿主进程启动早于环境变量配置时，从注册表 `HKCU\Environment` 读（python winreg）。

- **GitHub**（优先 `gh` CLI）：`gh release create ${NEW_TAG} --title "v${NEW_VERSION}" --generate-notes --target dev`，
  再 `gh release edit ${NEW_TAG} --notes-file <CHANGELOG 段>`——必须两步，把完整 CHANGELOG 盖上去才跟 Gitee 一致。
  三平台 notes 统一取 CHANGELOG 段并**去掉首行 `## [版本]` 标题**（release 页自带标题与日期，正文重复无意义），Gitee body 需另含 `target_commitish`。段提取：CHANGELOG.md 中 `## [${NEW_TAG}]` 行至下一个 `## [` 行之间（`python -I` 处理，含中文勿用 grep）。
- **Gitee**：若宿主提供 Gitee MCP 建 release 工具（如 `mcp__gitee__create_release`），传
  `owner="continew"`、`tag_name=${NEW_TAG}`、`name="v${NEW_VERSION}"`、`body=<同一段去标题>`、`target_commitish=<tag 未同步时传 release commit 全 sha 让平台自动建 tag>`；
  否则走 REST：
  ```bash
  curl -X POST "https://gitee.com/api/v5/repos/continew/continew-starter/releases?access_token=${GITEE_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"tag_name":"'${NEW_TAG}'","name":"v'${NEW_VERSION}'","body":"<同一段>","target_commitish":"'${RELEASE_COMMIT_SHA}'"}'
  ```
  **注意 owner 是 `continew` 不是 `continew-org`。**
- **AtomGit**（REST API + curl）：
  代码与 tag 由 mirror.yml 自动同步，本步骤只建 release。域名不是笔误：git 远端与网页用 `gitcode.com`，REST API 用 `api.atomgit.com`，同一平台两个入口。
  ```bash
  curl -X POST "https://api.atomgit.com/api/v5/repos/continew/continew-starter/releases?access_token=${ATOMGIT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"tag_name":"'${NEW_TAG}'","name":"v'${NEW_VERSION}'","body":"<同一段>","target_commitish":"'${RELEASE_COMMIT_SHA}'"}'
  ```
  创建后必须再 PATCH 一次标记"最新版本"（POST 不带 release_status 字段；PATCH body 缺任何必填字段都 400）：
  ```bash
  curl -X PATCH "https://api.atomgit.com/api/v5/repos/continew/continew-starter/releases/${NEW_TAG}?access_token=${ATOMGIT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"tag_name":"'${NEW_TAG}'","name":"v'${NEW_VERSION}'","body":"<同一段>","release_status":"latest"}'
  ```
  验证：`GET .../releases/latest` 应返回 ${NEW_TAG}。
  `${ATOMGIT_TOKEN}` / `${GITEE_TOKEN}` 由使用者自行配置到本机环境变量，不得硬编码进任何提交，
  也不要把本机凭证的存放路径写进仓库文件；Gitee 令牌必须勾选 `projects` scope（401 `no 'projects' scope` = scope 缺失，重建令牌）；
  接口文档：docs.atomgit.com/docs/apis/post-api-v-5-repos-owner-repo-releases。
  （历史备注：镜像仓库期间 REST 建 release 会 400 "image repository"，必须先同步出 tag——2.17.0 发版时遇到，随后关闭镜像解决。）

### 7. 维护分支

```bash
git checkout -b ${MAINT_BRANCH} ${NEW_TAG}
git push origin ${MAINT_BRANCH}
git checkout dev
```

（维护分支只是本次发版的快照，不做任何额外改动。）

### 8. 收口 issue（GitHub 确认一次，Gitee/AtomGit 合并确认一次）

每次发版都解决了一批问题，对应平台的 open issue 应予关闭，但**动作前先出清单等用户确认**：

1. 从 CHANGELOG 段的 fix / 破坏性条目与 `${PREV_TAG}..HEAD` 提交正文提取涉及的平台 issue 编号（GitHub `#N`、Gitee `Gitee#N`）。
2. 逐平台列 open issue 对照：被本版本解决的给出「建议关闭 + 评论模板」（评论注明修复于 v${NEW_VERSION} 并链接 release 页）；部分修复或等业务回归的保持 open，登记进交付总结。
3. 用户确认后执行：GitHub `gh issue close <N> -c "<评论>"`；Gitee `PATCH /api/v5/repos/continew/continew-starter/issues/<number>`（body `state=closed`）+ 关闭评论接口；AtomGit 走对应 REST（docs.atomgit.com）。
4. 关闭属可逆操作（可 reopen），但按铁律仍逐平台确认后执行。

### 9. 清理分支（可选，清单经用户确认后执行）

发版周期积累的 feat/fix 等分支在合并后失去价值，不知不觉堆积。清理分两层：

1. 列清单：本地 `git branch --merged dev`（已合并进 dev 的）；远端 `gh pr list --state merged --json number,headRefName` 取已合并 PR 的 head 分支。**main / dev / `3.0.x` 形态维护分支永不进名单**。
2. 用户确认后执行：远端 `gh api -X DELETE repos/continew-org/continew-starter/git/refs/heads/<branch>`；本地 `git branch -D <branch>`——squash 合并的分支不在 dev 历史中，`-d` 会拒删，确认已合并后用 `-D`。
3. **Gitee/AtomGit 无需处理**：mirror.yml 推送带 `--prune`，GitHub 侧删除的分支会自动从镜像平台移除。
4. 删除清单与数量记入交付总结。

## 完成

展示最终状态并提醒后续：

```
✅  v${NEW_VERSION} 发版完成
  - release commit / tag: ${NEW_TAG} (pushed) / maintenance: ${MAINT_BRANCH} (pushed)
  - GitHub: https://github.com/continew-org/continew-starter/releases/tag/${NEW_TAG}
  - Gitee:  https://gitee.com/continew/continew-starter/releases/tag/${NEW_TAG}
  - AtomGit: https://gitcode.com/continew/continew-starter/releases/tag/${NEW_TAG}
  - Maven Central: https://central.sonatype.com/artifact/top.continew.starter/continew-starter/versions
后续：
  - Central 同步通常 5-30 分钟，期间仓库不可见属正常
  - 下个开发周期开始时，在 dev 手动 commit: "build: 更新项目版本号至<major>.<minor+1>.0-SNAPSHOT"
```
