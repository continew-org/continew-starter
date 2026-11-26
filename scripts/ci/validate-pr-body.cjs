const { readFileSync } = require('node:fs');

// 围栏里的标题、表格、复选框是示例，不属于模板结构；非空代码仍可作为测试记录。
function sections(markdown) {
  const result = new Map();
  let lines;
  let fence;
  for (const line of markdown.replace(/<!--[\s\S]*?(?:-->|$)/g, '').split(/\r?\n/)) {
    if (fence) {
      if (line.trim().startsWith(fence) && /^\s*(`+|~+)\s*$/.test(line)) fence = null;
      else if (line.trim()) lines?.push('[code]');
      continue;
    }
    const opener = /^ {0,3}(`{3,}|~{3,})/.exec(line);
    if (opener) {
      fence = opener[1];
      continue;
    }
    const title = /^##\s+(.+?)\s*$/.exec(line);
    if (title) {
      lines = [];
      result.set(title[1], lines);
    } else {
      lines?.push(line);
    }
  }
  return result;
}

function boxes(lines = []) {
  return lines.map(line => /^\s*[-*] \[([ xX])\]\s+(.+?)\s*$/.exec(line)).filter(Boolean);
}

function cells(line = '') {
  const row = line.trim();
  return row.startsWith('|') ? row.slice(1).replace(/\|$/, '').split(/(?<!\\)\|/).map(s => s.trim()) : [];
}

function validate(body, template) {
  const expected = sections(template);
  const actual = sections(body ?? '');
  const errors = [];
  for (const name of expected.keys()) {
    if (!actual.has(name)) errors.push(`缺少模板段落「${name}」。`);
  }
  const selected = boxes(actual.get('变更类型')).filter(m => m[1].toLowerCase() === 'x');
  if (selected.length !== 1 || !boxes(expected.get('变更类型')).some(m => m[2] === selected[0][2])) {
    errors.push('请在「变更类型」中勾选一个模板列出的类型。');
  }
  for (const name of ['变更目的', '解决方案', '测试情况']) {
    if (!actual.get(name)?.join('\n').trim()) errors.push(`请填写「${name}」。`);
  }
  for (const name of ['破坏性变更', '提交前确认']) {
    const labels = new Set(boxes(actual.get(name)).map(m => m[2]));
    for (const [, , label] of boxes(expected.get(name))) {
      if (!labels.has(label)) errors.push(`请保留「${name}」的模板项「${label}」；不适用可不勾选。`);
    }
  }

  const header = expected.get('Changelog').map(cells).find(row => row.length);
  const rows = (actual.get('Changelog') ?? []).map(cells);
  const start = rows.findIndex(row => row.length === header.length && row.every((v, i) => v === header[i]));
  const separator = rows[start + 1] ?? [];
  if (start < 0 || separator.length !== header.length || !separator.every(s => /^:?-{3,}:?$/.test(s))) {
    errors.push('请保留 Changelog 表头、列顺序与分隔行。');
  } else {
    let filled = false;
    for (const row of rows.slice(start + 2)) {
      if (!row.length) break;
      if (row.length !== header.length || !row[0] || !row[1] || /^[-—]+$/.test(row[1])) {
        errors.push('Changelog 每行须填写模块、变更说明并保留 Related issues 列。');
        break;
      }
      filled = true;
      if (/\b(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?)\b(?!\s+(?:[\w.-]+\/[\w.-]+)?#[1-9]\d*\b)/i.test(row[2])) {
        errors.push('Related issues 中的关闭关键词须跟 Issue 编号，如 Closes #123；无关联可留空。');
        break;
      }
    }
    if (!filled) errors.push('请填写至少一行 Changelog。');
  }
  return errors;
}

if (require.main === module) {
  const event = JSON.parse(readFileSync(process.env.GITHUB_EVENT_PATH, 'utf8'));
  const errors = validate(event.pull_request.body, readFileSync('.github/PULL_REQUEST_TEMPLATE.md', 'utf8'));
  for (const error of errors) console.error(`::error::${error}`);
  process.exitCode = errors.length ? 1 : 0;
}

module.exports = { validate };
