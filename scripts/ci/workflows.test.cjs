const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const { mkdtempSync, readFileSync, statSync, writeFileSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { test } = require('node:test');
const { validate } = require('./validate-pr-body.cjs');

const root = join(__dirname, '../..');
const template = readFileSync(join(root, '.github/PULL_REQUEST_TEMPLATE.md'), 'utf8');
const valid = template.replace('- [ ] 其他', '- [x] 其他')
  .replace('## 变更目的', '## 变更目的\n让文档 PR 得到必需检查。')
  .replace('## 解决方案', '## 解决方案\n保留 PR 触发。')
  .replace('## 测试情况', '## 测试情况\n```text\n7 workflows linted\n```')
  .replace('|      |           |                |', '| CI | 保留触发 | |');

test('Starter 模板：七段，无必填关联 Issue、影响范围或全勾选要求', () => {
  assert.equal(template.match(/^## /gm).length, 7);
  assert.deepEqual(validate(valid.replace(/\r?\n/g, '\r\n'), template), []);
  assert.deepEqual(validate(valid.replace('保留触发 | |', '保留触发 | Fixes continew-org/continew-starter#1 |'), template), []);
});

for (const [scenario, from, to] of [
  ['漏勾类型', '[x] 其他', '[ ] 其他'],
  ['多选类型', '[ ] 测试', '[X] 测试'],
  ['未知类型', '[x] 其他', '[x] 自造类型'],
  ['注释不是正文', '保留 PR 触发。', '<!-- 提示 -->'],
  ['示例标题不是段落', '## 提交前确认', '~~~~\n## 提交前确认\n```\n~~~~'],
  ['变更说明为空', '| CI | 保留触发 | |', '| CI | | |'],
  ['缺失表头', '| 模块 | Changelog', '| 模块 | 说明'],
  ['关键词缺号', '保留触发 | |', '保留触发 | Closes # |'],
]) {
  test(`Starter 模板：${scenario}`, () => assert.ok(validate(valid.replace(from, to), template).length));
}

for (const [name, count] of [['破坏性变更', 1], ['提交前确认', 9]]) {
  test(`Starter 模板：${name}的 ${count} 个标签逐一校验，允许全部不勾选`, () => {
    const section = template.split(`## ${name}`)[1].split(/^## /m)[0];
    const items = section.match(/^- \[ \] .+$/gm);
    assert.equal(items.length, count);
    assert.deepEqual(validate(valid, template), []);
    for (const item of items) {
      const label = item.slice('- [ ] '.length).trimEnd();
      for (const replacement of ['', '- [x] 其他确认项']) {
        assert.ok(validate(valid.replace(item, replacement), template).some(error => error.includes(label)), label);
      }
      assert.deepEqual(validate(valid.replace(item, item.replace('[ ]', '[X]')), template), []);
    }
  });
}

function workflow(file) {
  return readFileSync(join(root, '.github/workflows', file), 'utf8').replace(/\r\n/g, '\n');
}

test('PR 标题必检，正文随同一 required job 校验，只有正文步骤豁免机器人', () => {
  const yaml = workflow('pr-validation.yml');
  assert.match(yaml, /types: \[[^\]]*\bedited\b[^\]]*\]/);
  const title = yaml.split('  validate-pr-title:\n')[1].split(/^  [\w-]+:\s*$/m)[0];
  const [before, fetch, body] = title.split(/      - name: /);
  assert.match(before, /name: Validate PR title\n/);
  assert.match(before, /uses: amannn\/action-semantic-pull-request@/);
  assert.doesNotMatch(before, /^\s*if:/m);
  for (const step of [fetch, body]) assert.match(step, /if: github\.event\.pull_request\.user\.type != 'Bot'/);
  assert.match(body, /run: node scripts\/ci\/validate-pr-body\.cjs/);
});

test('特权上下文不检出仓库代码，模板与校验器按 workflow_sha 从 API 取得', () => {
  const yaml = workflow('pr-validation.yml');
  assert.doesNotMatch(yaml, /actions\/checkout@/);
  assert.match(yaml, /TRUSTED_SHA: \$\{\{ github\.workflow_sha \}\}/);
  const fetch = stepLiteral('pr-validation.yml', 'Fetch trusted template and validator', 'run');
  assert.match(fetch, /local path="\$1"/);
  assert.match(fetch, /contents\/\$path\?ref=\$\{TRUSTED_SHA\}/);
  assert.match(fetch, /Accept: application\/vnd\.github\.raw/);
  for (const file of ['.github/PULL_REQUEST_TEMPLATE.md', 'scripts/ci/validate-pr-body.cjs']) {
    assert.match(fetch, new RegExp(`fetch ${file.replace(/\./g, '\\.')}`));
  }
  assert.match(yaml, /working-directory: \$\{\{ runner\.temp \}\}\/pr-body/);
});

test('无 checkout 时按 API 组装可信文件，并在同构目录执行校验器', () => {
  const fetch = stepLiteral('pr-validation.yml', 'Fetch trusted template and validator', 'run');
  const dir = mkdtempSync(join(tmpdir(), 'pr-body-'));
  const runner = join(dir, 'runner');
  const src = root.replace(/\\/g, '/');
  // 用同名 shell 函数替换 gh，仅回放仓库内真实文件；fetch 步骤本身逐字取自工作流。
  const fakeGh = `gh() {
    local path="\${2#*contents/}"
    path="\${path%%\\?*}"
    case "$path" in
      .github/PULL_REQUEST_TEMPLATE.md) cat "$SRC/.github/PULL_REQUEST_TEMPLATE.md" ;;
      scripts/ci/validate-pr-body.cjs) cat "$SRC/scripts/ci/validate-pr-body.cjs" ;;
      *) return 1 ;;
    esac
  }`;
  const env = {
    ...process.env,
    RUNNER_TEMP: runner,
    GITHUB_REPOSITORY: 'continew-org/continew-starter',
    TRUSTED_SHA: 'e3e04c4c',
    SRC: src,
  };
  execFileSync('bash', ['-euc', `${fakeGh}\n${fetch}`], { env, stdio: 'pipe' });
  for (const file of ['.github/PULL_REQUEST_TEMPLATE.md', 'scripts/ci/validate-pr-body.cjs']) {
    assert.ok(statSync(join(runner, 'pr-body', file)).size > 0, file);
  }
  const event = join(dir, 'event.json');
  writeFileSync(event, JSON.stringify({ pull_request: { body: valid } }));
  execFileSync('node', ['scripts/ci/validate-pr-body.cjs'], {
    cwd: join(runner, 'pr-body'),
    env: { ...process.env, GITHUB_EVENT_PATH: event },
    stdio: 'pipe',
  });
});

test('OCR 不上传原始评审产物，探针只输出状态码', () => {
  const yaml = workflow('ocr-review.yml');
  assert.match(yaml, /upload_artifacts: 'false'/);
  assert.doesNotMatch(yaml, /uses: actions\/upload-artifact@/);
  assert.match(stepLiteral('ocr-review.yml', 'Diagnose LLM endpoint', 'run'), /-o \/dev\/null -w .*%\{http_code\}/);
});

test('OCR 未配置推理档位时不产生含空串 reasoning_effort 的 extra_body', () => {
  const yaml = workflow('ocr-review.yml');
  assert.match(yaml, /llm_extra_body: '\{\}'/);
  assert.doesNotMatch(yaml, /"reasoning_effort": "\$\{\{/);
  assert.match(yaml, /llm_reasoning_effort: \$\{\{ steps\.protocol\.outputs\.anthropic == 'false' && env\.OCR_REASONING_EFFORT \|\| '' \}\}/);
  const probe = stepLiteral('ocr-review.yml', 'Diagnose LLM endpoint', 'run');
  assert.match(probe, /\n  \*\) ;;/);
  assert.match(probe, /if \$effort != "" then \{reasoning_effort: \$effort\} else \{\} end/);
});

// 取指定步骤的 YAML literal，以生产脚本为被测对象，不发送请求或评论。
function stepLiteral(file, step, field) {
  const yaml = workflow(file);
  const block = yaml.split(`      - name: ${step}\n`)[1]?.split(/\n      - /)[0];
  const lines = block?.split('\n') ?? [];
  const key = lines.findIndex(line => line.trim() === `${field}: |`);
  assert.notEqual(key, -1, `${step}.${field}`);
  const indent = ' '.repeat(lines[key].indexOf(field) + 2);
  const code = [];
  for (const line of lines.slice(key + 1)) {
    if (line && !line.startsWith(indent)) break;
    code.push(line.slice(indent.length));
  }
  return code.join('\n');
}

function githubScript(file, step) {
  const AsyncFunction = Object.getPrototypeOf(async () => {}).constructor;
  return new AsyncFunction('github', 'context', 'core', stepLiteral(file, step, 'script'));
}

test('镜像：触发层仅允许数值维护分支、dev/main 和全部标签', () => {
  const push = workflow('mirror.yml').split('  push:\n')[1].split('  workflow_dispatch:')[0].trimEnd();
  assert.equal(push, "    branches:\n      - dev\n      - main\n      - '[0-9]+.[0-9]+.x'\n    tags:\n      - '**'");
});

test('镜像：按完整 ref 区分同名分支和标签，排除近似版本和斜杠分支', () => {
  const keep = ['dev', 'main', '1.5.x', '12.17.x'];
  const exclude = ['feat/next', '3.0-docs.x', '3.0/test.x', '2.17.1.x', '3.x', '3.a.x', '3x0.x'];
  const refs = [...keep, ...exclude].flatMap(name => [`refs/heads/${name}`, `refs/tags/${name}`]);
  const filter = stepLiteral('mirror.yml', '过滤镜像分支', 'run');
  const fakeGit = `git() {
    case "$3" in
      for-each-ref)
        [[ "$4" == '--format=%(refname:lstrip=2)' && "$5" == refs/heads/ ]] || exit 1
        while read -r ref; do
          if [[ $ref == "$5"* ]]; then printf '%s\\n' "\${ref#refs/heads/}"; fi
        done <<< "$REFS" ;;
      update-ref) [[ "$4" == '-d' ]] && printf 'filtered %s\\n' "$5" ;;
      *) exit 1 ;;
    esac
  }`;
  const output = execFileSync('bash', ['-c', `${fakeGit}\n${filter}`], {
    encoding: 'utf8', env: { ...process.env, REFS: refs.join('\n') },
  });
  assert.deepEqual(output.match(/^filtered .+$/gm), exclude.map(name => `filtered refs/heads/${name}`));
});

const protocol = stepLiteral('ocr-review.yml', 'Resolve OCR protocol', 'run');
for (const [setting, expected] of [['YES', 'true'], ['1', 'true'], ['True', 'true'], ['false', 'false'], ['0', 'false']]) {
  test(`OCR 协议别名：${setting}`, () => {
    // Git Bash 的 /dev/stdout 需要 Bash 自身创建的管道，而不是 Node 的 Windows 句柄。
    const output = execFileSync('bash', ['-ec', `result=$(\n${protocol}\n); printf '%s' "$result"`], {
      encoding: 'utf8', env: { ...process.env, USE_ANTHROPIC: setting, GITHUB_OUTPUT: '/dev/stdout' },
    });
    assert.equal(output.trim(), `anthropic=${expected}`);
  });
}

const greet = githubScript('greeting.yml', 'Greet first-time contributor');
const email = githubScript('pr-validation.yml', 'Check commit authors linked to GitHub');
const repo = { owner: 'org', repo: 'starter' };
const item = { user: { login: 'alice', type: 'User' }, author_association: 'NONE', number: 42 };
const bot = { login: 'github-actions[bot]' };

function api(history = [], comments = [], commits = []) {
  const calls = [];
  const issues = {
    listForRepo: 'history', listComments: 'comments',
    createComment: params => calls.push(['create', params]),
    updateComment: params => calls.push(['update', params]),
  };
  const paginate = async (method, params) => {
    assert.equal(params.per_page, 100);
    return method === 'commits' ? commits : comments.flat();
  };
  paginate.iterator = async function* (method, params) {
    assert.equal(params.per_page, 100);
    if (method === 'history') {
      assert.equal(params.creator, 'alice');
      assert.equal(params.state, 'all');
    }
    for (const data of method === 'history' ? history : comments) yield { data };
  };
  return { calls, github: { rest: { issues, pulls: { listCommits: 'commits' } }, paginate } };
}

for (const event of ['issue', 'pull_request']) {
  test(`首贡：${event} 按投稿者而非 actor 欢迎；后续编号与用户伪标记不阻止欢迎`, async () => {
    const { github, calls } = api([[{ number: 42 }, { number: 43 }]], [[{
      user: item.user, body: '<!-- continew-starter:greeting -->',
    }]]);
    await greet(github, { repo, actor: 'maintainer', payload: { [event]: item } });
    assert.equal(calls.length, 1);
    assert.match(calls[0][1].body, /@alice/);
    assert.equal(calls[0][1].issue_number, 42);
  });
}

for (const [name, changes, history, comments] of [
  ['跨页既有投稿', {}, [[], [{ number: 41 }]], []],
  ['跨页标记去重', {}, [], [[], [{ user: bot, body: '<!-- continew-starter:greeting -->' }]]],
  ['旧版欢迎去重', {}, [], [[{ user: bot, body: '🙏 感谢 @alice 向 ContiNew Starter 提交第一个 Issue！' }]]],
  ['仓库成员', { author_association: 'MEMBER' }, [], []],
  ['机器人', { user: { type: 'Bot', login: 'robot' } }, [], []],
  ['未知作者', { user: null }, [], []],
]) {
  test(`首贡：跳过${name}`, async () => {
    const { github, calls } = api(history, comments);
    await greet(github, { repo, payload: { issue: { ...item, ...changes } } });
    assert.deepEqual(calls, []);
  });
}

test('邮箱：第 101 个提交失败，忽略用户伪标记；修复后只更新原评论一次', async () => {
  const comments = [[{ user: item.user, body: '<!-- pr-validation-author-check -->' }]];
  const commits = Array.from({ length: 101 }, (_, i) => ({ sha: String(i).padStart(40, '0'), author: i < 100 ? item.user : null }));
  const { github, calls } = api([], comments, commits);
  const failures = [];
  const core = { setFailed: message => failures.push(message) };
  const context = { repo, payload: { action: 'edited', pull_request: { ...item, base: { sha: 'abc' }, commits: 101 } } };
  await email(github, context, core);
  assert.equal(failures.length, 1);
  assert.equal(calls[0][0], 'create');
  const owned = { user: bot, id: 5, body: calls[0][1].body };
  comments.push([owned]);
  commits[100].author = item.user;
  await email(github, context, core);
  assert.equal(calls[1][0], 'update');
  assert.equal(calls[1][1].comment_id, 5);
  owned.body = calls[1][1].body;
  await email(github, context, core);
  assert.equal(calls.length, 2);
  assert.equal(failures.length, 1);
});
