---
name: ocn-starter-release
description: >
  continew-starter 仓库发版技能：预检 → CHANGELOG → release commit → mvn deploy 推 Maven Central →
  打 tag+push → GitHub/Gitee/AtomGit 三平台 release → 切维护分支。版本号不手动传：从
  continew-starter-dependencies/pom.xml 的 <revision> 自动推导发版号（去掉 -SNAPSHOT）。
  用户说"发版"、"发布新版"、"release"、"准备发版"、"tag"、"deploy 到 Central"、"新建 GitHub Release"、
  "打 tag"、"切维护分支"，或想看发版流程清单时使用。
  专用于 continew-starter 仓库，不适用其它 Maven 项目。发版前自验（mvn install + continew-admin
  业务侧验证）由用户自行完成，skill 不做。
---

# continew-starter 发版

## 铁律

- **不可逆操作（mvn deploy / git push / 建 release）执行前必须展示命令，等用户确认。**
- 版本号不传参，全部由 LLM 从 `<revision>` 推导并代入（后面所有命令都用这些变量）：

  | 变量 | 含义 | 推导 |
  |:--|:--|:--|
  | `${NEW_VERSION}` | 发版号 | `<revision>` 去掉 `-SNAPSHOT`（如 `X.Y.Z-SNAPSHOT` → `X.Y.Z`） |
  | `${PREV_TAG}` | 上一个 tag | `git describe --tags --abbrev=0`（如 `vX.Y.Z`） |
  | `${NEW_TAG}` | 本次 tag | `v${NEW_VERSION}` |
  | `${MAINT_BRANCH}` | 维护分支 | `${NEW_VERSION}` 的 `major.minor` + `.x` |
  | `${TODAY}` | 发版日期 | 当天 `YYYY-MM-DD` |

- **三平台 release 内容一致**：GitHub / Gitee / AtomGit 都放同一份 CHANGELOG 段。
- 发版后 dev 保持 `<revision>=${NEW_VERSION}`（不回 SNAPSHOT）；下个开发周期开始时由用户手动
  `build: 更新项目版本号至<major>.<minor+1>.0-SNAPSHOT`。

## 流程

### 1. 预检（任一失败即停）

| 检查 | 命令 | 失败时 |
|:--|:--|:--|
| 在 dev 分支 | `git branch --show-current` | `git checkout dev` |
| 工作区只有发版文件 | `git status --short` | 多余改动先 commit / stash |
| `<revision>` 是 SNAPSHOT | `grep '<revision>[0-9.]*-SNAPSHOT' continew-starter-dependencies/pom.xml` | 手动改 pom |
| 上一个 tag 存在且有提交 | `git describe --tags --abbrev=0` + `git log ${PREV_TAG}..HEAD --oneline` | `git fetch origin --tags`；区间空则停 |
| 编译通过 | `mvn compile -Dspotless.apply.skip=true` | 先修编译 |

> 发版文件只有 6 类：`CHANGELOG.md` / `README.md` / `continew-starter-*/pom.xml` /
> `ContiNewStarterVersion.java` / `.gitignore` / `.github/ISSUE_TEMPLATE/*.yml`。
> 不检查：origin/dev 领先落后、GPG 私钥、Central 凭证（mvn 自己会报）。

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

组装成段 prepend 到 `CHANGELOG.md` 顶部（不动 `CHANGELOG_1.x.x.md` / `CHANGELOG_2.0.0-2.12.2.md` 老归档），
`git diff CHANGELOG.md` 给用户校对。

### 3. release commit

- `bug.yml` / `question.yml` 的版本下拉：首位改为 `${NEW_VERSION}`（dev 周期时首位是
  `${NEW_VERSION}-SNAPSHOT (开发版本-dev 分支)`，去掉后缀即新 release）、删最旧，保持 3 个选项。
- `git add` 上述 6 类发版文件（README 有改动才加）→ `git commit -m "release: v${NEW_VERSION}"`。

### 4. mvn clean deploy -Prelease,gpg（最危险，单独确认）

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

- **GitHub**（gh CLI，MCP 不可用）：`gh release create ${NEW_TAG} --title "v${NEW_VERSION}" --generate-notes --target dev`，
  再 `gh release edit ${NEW_TAG} --notes-file <CHANGELOG 段>`——必须两步，把完整 CHANGELOG 盖上去才跟 Gitee 一致。
- **Gitee**（MCP）：`mcp__gitee__create_release(owner="continew", tag_name="${NEW_TAG}", name="v${NEW_VERSION}", body=<同一段>, target_commitish="dev")`。
  **注意 owner 是 `continew` 不是 `continew-org`。**
- **AtomGit**：无 MCP，需手动——先确保代码/tag 同步（无自动 mirror，网页点"同步"或配 PAT 后 `git push`），
  再在网页上新建 release，body 粘同一段。**告诉用户步骤，不要假装自动完成。**

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
