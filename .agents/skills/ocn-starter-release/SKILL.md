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

> 发版文件只有 6 类：`CHANGELOG.md` / `README.md` / `continew-starter-*/pom.xml` /
> `ContiNewStarterVersion.java` / `.gitignore` / `.github/ISSUE_TEMPLATE/*.yml`。
> 不检查：origin/dev 领先落后、GPG 私钥、Central 凭证（deploy 时构建工具自己会报）。

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
| `build(dependencies): X a.b.c => x.y.z` | 📦 依赖升级 | 按 pom property 顺序排；`X <= Y` 且 X<Y（pin）跳过、X>Y（降级）进 |
| `docs` / `ci` / `test` / 其它 `build:` | 跳过 | |

行格式：`- 【scope】subject ([短哈希](commit_url)) (#PR) @作者`。
🆕 新贡献者：本区间 commit 的 author email 不在 `${PREV_TAG}` 之前历史里（`git log ${PREV_TAG} --format=%ae` 比对）。

**写入前硬性校验（2.17.0 教训）**：每个写进 CHANGELOG 的提交 hash 必须真实存在于当前 dev——
逐条 `git cat-file -t <hash>` 或 `git merge-base --is-ancestor <hash> HEAD` 验证；用户校对时删掉的条目，
发版全程不得再补回（release notes、Gitee/GitHub release body 同步以用户校对版为唯一事实源）。

组装成段 prepend 到 `CHANGELOG.md` 顶部（不动 `CHANGELOG_1.x.x.md` / `CHANGELOG_2.0.0-2.12.2.md` 老归档），
`git diff CHANGELOG.md` 给用户校对。

README 徽章随发版切换：`badge/SNAPSHOT-vX.Y.Z` → `badge/RELEASE-v${NEW_VERSION}`（dev 进入下周期时再改回 SNAPSHOT）。

### 3. release commit

- `git add` 上述 6 类发版文件（README 有改动才加）→ `git commit -m "release: v${NEW_VERSION}"`。

### 4. ./mvnw clean deploy -Prelease,gpg（最危险，单独确认）

推全部模块的 jar + sources + javadoc + .asc 签名到 Maven Central，**无法撤回**。

> 已知问题：`central-publishing-maven-plugin` 0.4.0 会假失败（`UnrecognizedPropertyException`）
> 但 **bundle 已上传**——去 [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)
> 手动 Publish，已上传算半成功，继续后面步骤。升级插件到 0.7.0+ 留到下个 dev 周期的
> `build(dependencies):` commit。

### 5. tag + push（一次确认）

```bash
git tag -a ${NEW_TAG} -m "release: v${NEW_VERSION}"
git push origin dev
git push origin ${NEW_TAG}
```

（不用 `git push --tags`，避免误推其它本地 tag。）

### 6. 三平台 release（GitHub 确认一次，Gitee+AtomGit 合并确认一次）

> 凭证一律从本机环境变量取，不写入仓库任何文件；工具名随宿主而异，**不要假定某个 MCP 工具一定存在**，
> 有对应工具就用，没有就走下面的等价命令。

- **GitHub**（优先 `gh` CLI）：`gh release create ${NEW_TAG} --title "v${NEW_VERSION}" --generate-notes --target dev`，
  再 `gh release edit ${NEW_TAG} --notes-file <CHANGELOG 段>`——必须两步，把完整 CHANGELOG 盖上去才跟 Gitee 一致。
- **Gitee**：若宿主提供 Gitee MCP 建 release 工具（如 `mcp__gitee__create_release`），传
  `owner="continew"`、`tag_name=${NEW_TAG}`、`name="v${NEW_VERSION}"`、`body=<同一段>`、`target_commitish="dev"`；
  否则走 REST：
  ```bash
  curl -X POST "https://gitee.com/api/v5/repos/continew/continew-starter/releases?access_token=${GITEE_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"tag_name":"'${NEW_TAG}'","name":"v'${NEW_VERSION}'","body":"<同一段>","target_commitish":"dev"}'
  ```
  **注意 owner 是 `continew` 不是 `continew-org`。**
- **AtomGit**（REST API + curl）：
  仓库已关镜像（2026-10-06 起为普通仓库），无自动同步，需先手动推代码。
  域名不是笔误：git 远端与网页用 `gitcode.com`，REST API 用 `api.atomgit.com`，同一平台两个入口。
  `git remote add atomgit https://gitcode.com/continew/continew-starter.git`（首次），
  `git push atomgit dev && git push atomgit ${NEW_TAG} && git push atomgit ${MAINT_BRANCH}`，然后：
  （dev 是保护分支：若宿主提供 AtomGit MCP 的分支保护工具（如 `mcp__atomgit__remove_branch_protection`），
  强推类操作前先解除保护，完成后用
  `PUT /api/v5/repos/{owner}/{repo}/branches/setting/new` body `{"wildcard":"dev","pushers":"owner","mergers":"owner"}`
  恢复——MCP 的 protect_branch 工具走旧路径已 405，不可用；无 MCP 时在网页端改分支保护设置。）
  ```bash
  curl -X POST "https://api.atomgit.com/api/v5/repos/continew/continew-starter/releases?access_token=${ATOMGIT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"tag_name":"'${NEW_TAG}'","name":"v'${NEW_VERSION}'","body":"<同一段>","target_commitish":"dev"}'
  ```
  创建后必须再 PATCH 一次标记"最新版本"（POST 不带 release_status 字段；PATCH body 缺任何必填字段都 400）：
  ```bash
  curl -X PATCH "https://api.atomgit.com/api/v5/repos/continew/continew-starter/releases/${NEW_TAG}?access_token=${ATOMGIT_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"tag_name":"'${NEW_TAG}'","name":"v'${NEW_VERSION}'","body":"<同一段>","release_status":"latest"}'
  ```
  验证：`GET .../releases/latest` 应返回 ${NEW_TAG}。
  `${ATOMGIT_TOKEN}` / `${GITEE_TOKEN}` 由使用者自行配置到本机环境变量，不得硬编码进任何提交，
  也不要把本机凭证的存放路径写进仓库文件；接口文档：docs.atomgit.com/docs/apis/post-api-v-5-repos-owner-repo-releases。
  （历史备注：镜像仓库期间 REST 建 release 会 400 "image repository"，必须先同步出 tag——2.17.0 发版时遇到，随后关闭镜像解决。）

### 7. 维护分支

```bash
git checkout -b ${MAINT_BRANCH} ${NEW_TAG}
git push origin ${MAINT_BRANCH}
git checkout dev
```

（维护分支只是本次发版的快照，不做任何额外改动。）

## 完成

展示最终状态并提醒后续：

```
✅  v${NEW_VERSION} 发版完成
  - release commit / tag: ${NEW_TAG} (pushed) / maintenance: ${MAINT_BRANCH} (pushed)
  - GitHub: https://github.com/continew-org/continew-starter/releases/tag/${NEW_TAG}
  - Gitee:  https://gitee.com/continew/continew-starter/releases/tag/${NEW_TAG}
  - AtomGit: <链接或"请手动建">
  - Maven Central: https://central.sonatype.com/artifact/top.continew.starter/continew-starter/versions
后续：
  - Central 同步通常 5-30 分钟，期间仓库不可见属正常
  - 下个开发周期开始时，在 dev 手动 commit: "build: 更新项目版本号至<major>.<minor+1>.0-SNAPSHOT"
```
