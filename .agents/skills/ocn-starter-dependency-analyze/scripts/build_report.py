#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Render scan.py JSON into a storage-analyzer-style interactive HTML report.

Usage:
  python build_report.py <scan.json> [output.html]

If output omitted, writes to stdout. The HTML mirrors storage-analyzer's visual
language: overview card with a tiered segment bar, Top5 table, priority advice,
🟢/🟡/🔴/⚪ collapsible cards with copyable POM-edit commands. In static mode
(no server) the "apply upgrade" buttons are absent — file:// pages can't touch
git/maven. Use server.py for clickable apply.

Input (scan.py output) — relevant fields:
{
  summary: {total, outdated, green, yellow, red, downgrade, up_to_date,
            unreachable, plugin, tier_counts},
  top5: [{rank, property, artifact, tier, locked, target, gap, type, note}],
  items: [{
    property, group_id, artifact_id, locked, release, same_line_latest,
    same_major_latest, tier, target, gap, gap_versions, same_line_gap,
    constraints, family, unreachable, error
  }, ...],
  unreachable: [...], offline, repo: {revision}, generated_at, scan_seconds
}
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
TEMPLATE_PATH = os.path.join(HERE, '..', 'assets', 'report_template.html')


def load_json(path):
    """Read a JSON file that may be UTF-8 (with/without BOM) or UTF-16.

    PowerShell's `>` redirection writes UTF-16 by default, so scan.py output that
    was accidentally redirected can't be read as UTF-8. Try UTF-8 first (the
    canonical output of scan.py -o), then UTF-16 for redirected copies. Returns
    the parsed object, or raises JSONDecodeError / UnicodeError."""
    raw = open(path, 'rb').read()
    if raw[:2] in (b'\xff\xfe', b'\xfe\xff'):
        text = raw.decode('utf-16')
    else:
        text = raw.decode('utf-8-sig').lstrip('\ufeff')
    return json.loads(text)

# Tier -> display label + color class (mirrors storage-analyzer green/yellow/red)
TIER_LABEL = {
    'green': '🟢 可直接升级（补丁）',
    'yellow': '🟡 需读变更记录（小版本）',
    'red': '🔴 需人工迁移（大版本）',
    'downgrade': '⚪ 版本编号异常（锁定比 release 新）',
    'uptodate': '已是最新',
    'plugin': 'Maven 插件',
    'unknown': '未能检测',
}


def pom_edit_command(property_name, new_version):
    """A copyable one-liner: sed to bump a <prop.version> value in the POM.
    Uses a fully-anchored regex so it only matches the exact property line."""
    prop = property_name.replace('.', r'\.')
    # Works on macOS (BSD sed) and Linux (GNU sed). Windows users: edit by hand
    # or use the server.py apply button (git + python, no sed needed).
    return ("sed -i.bak -E 's/<{0}>[^<]*<\\/{0}>/<{0}>{1}<\\/{0}>/' "
            "continew-starter-dependencies/pom.xml").format(prop, new_version)


def build_analysis(scan):
    """Transform scan.py output into the analysis JSON the template renders."""
    s = scan['summary']
    items = scan['items']

    # overview lead line
    parts = []
    if s['outdated']:
        parts.append('{} 个依赖落后'.format(s['outdated']))
    if s['red']:
        parts.append('{} 个跨大版本'.format(s['red']))
    if s['downgrade']:
        parts.append('{} 个版本编号异常'.format(s['downgrade']))
    overview = '、'.join(parts) + '。' if parts else '所有依赖均已最新。'

    # priority list
    priority = []
    # highest-stakes first: red with platform coupling
    reds = [i for i in items if i['tier'] == 'red']
    if reds:
        names = '、'.join(i['artifact_id'] for i in reds[:3])
        priority.append('🔴 跨大版本升级（{}）风险最高，需逐项读迁移要点、全 reactor compile + 运行 demo 验证，勿用「应用升级」按钮一键改。'.format(names))
    yellows = [i for i in items if i['tier'] == 'yellow']
    if yellows:
        priority.append('🟡 小版本升级（{} 等）读变更记录后可升，建议用「应用升级」按钮逐项改并跑 compile。'.format(
            '、'.join(i['artifact_id'] for i in yellows[:3])))
    greens = [i for i in items if i['tier'] == 'green']
    if greens:
        priority.append('🟢 补丁升级（{} 等）风险低，可放心用「应用升级」按钮批量处理。'.format(
            '、'.join(i['artifact_id'] for i in greens[:3])))
    # platform coupling warning
    sb = next((i for i in items if i['property'] == 'spring-boot.version'), None)
    sc = next((i for i in items if i['property'] == 'spring-cloud.version'), None)
    if sb and sb['tier'] in ('green', 'yellow', 'red'):
        priority.append('⚠️ Spring Boot 升级必须配 Spring Cloud 对应 release train，单独升 Boot 会被 server.py 拦下。查官方兼容矩阵后再动。')
    downgrades = [i for i in items if i['tier'] == 'downgrade']
    if downgrades:
        priority.append('⚪ {} 锁定版本比 Central release 新，疑似项目重新编号或坐标指向不同分支，请人工核验 artifact 是否需要更换。'.format(
            '、'.join(i['artifact_id'] for i in downgrades)))

    def card(it):
        tier = it['tier']
        targets = it.get('targets') or {}
        entry = {
            'property': it['property'],
            'name': it.get('artifact_id') or it['property'],
            'group_id': it.get('group_id', ''),
            'family': it.get('family', ''),
            'locked': it['locked'],
            'release': it.get('release'),
            'target': it.get('target'),
            'same_line_latest': it.get('same_line_latest'),
            'gap': it.get('gap', ''),
            'gap_versions': it.get('gap_versions', []),
            'gap_versions_by_tier': it.get('gap_versions_by_tier', {}),
            'constraints': it.get('constraints', ''),
            'tier': tier,
            # new: three independent options + their changelog links, so the
            # report can show a 🔴 dep's safe patch instead of only the major.
            'targets': targets,
            'changelog': it.get('changelog'),
            'changelogs': it.get('changelogs') or {},
            'central_url': it.get('central_url'),
            'compare_urls': it.get('compare_urls') or {},
            'plugin': bool(it.get('plugin')),
            # publish dates: staleness of the locked version + when each target shipped
            'locked_released': it.get('locked_released'),
            'released': it.get('released') or {},
        }
        # One copyable POM command per available option, labelled by risk.
        cmds = []
        for key, label in (('patch', '🟢 补丁'), ('minor', '🟡 小版本'), ('major', '🔴 大版本')):
            v = targets.get(key)
            if not v:
                continue
            cmds.append({
                'label': '{} → {}'.format(label, v),
                'cmd': pom_edit_command(it['property'], v),
            })
        entry['commands'] = cmds
        if tier == 'red':
            entry['migration'] = True
        return entry

    return {
        'overview': overview,
        'priority': priority,
        'long_term': [
            '定期巡检：每月跑一次 scan.py，趁补丁阶段（🟢）升级，避免积压成大版本（🔴）。',
            '大版本升级单独开分支，逐 starter 核验 auto-config 条件变化，跑全 reactor mvn verify。',
            '新增依赖时同步更新 references/coordinates.md 与 scan.py 的 COORDINATES，保持目录完整。',
            'Spring Boot 主版本升级前先看官方 Migration Guide，并确认 Spring Cloud 兼容 train。',
        ],
        'green': [card(i) for i in items if i['tier'] == 'green'],
        'yellow': [card(i) for i in items if i['tier'] == 'yellow'],
        'red': [card(i) for i in items if i['tier'] == 'red'],
        'downgrade': [card(i) for i in items if i['tier'] == 'downgrade'],
        # Already on the latest available GA — still shown so the user can see
        # how long each has sat unchanged and decide whether to re-evaluate.
        'uptodate': [card(i) for i in items if i['tier'] == 'uptodate'],
        'unreachable': [i['property'] for i in items if i.get('unreachable')],
        # Plugins are now scanned like any other coordinate, so they already
        # appear in the green/yellow/red lists above. This list is kept only for
        # the legacy "not detected" section, which is now normally empty.
        'plugins': [{'property': i['property'], 'locked': i['locked']}
                    for i in items if i['tier'] == 'plugin'],
    }


def main():
    if len(sys.argv) < 2:
        sys.stderr.write('用法: build_report.py <scan.json> [output.html]\n')
        sys.exit(2)
    scan_path = sys.argv[1]
    out_path = sys.argv[2] if len(sys.argv) > 2 else None

    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

    scan = load_json(scan_path)

    analysis = build_analysis(scan)
    payload = {
        'generated_at': scan.get('generated_at'),
        'scan_seconds': scan.get('scan_seconds'),
        'offline': scan.get('offline', False),
        'revision': scan.get('repo', {}).get('revision'),
        'project_name': scan.get('repo', {}).get('project_name'),
        'summary': scan['summary'],
        'top5': scan['top5'],
        'analysis': analysis,
        'server_mode': False,  # static build: no apply buttons
    }

    with open(TEMPLATE_PATH, 'r', encoding='utf-8') as f:
        tpl = f.read()
    html = tpl.replace('__REPORT_DATA__', json.dumps(payload, ensure_ascii=False))
    # static mode: no DELETE/APPLY config injected
    html = html.replace('__APPLY_CONFIG__', 'null')

    if out_path:
        with open(out_path, 'w', encoding='utf-8') as f:
            f.write(html)
        sys.stderr.write('报告已生成: {}\n'.format(out_path))
    else:
        sys.stdout.write(html)


if __name__ == '__main__':
    main()
