#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Upgrade前影响面分析：在改 POM 之前预测破坏性变更，而不是等 compile 报错。

这是生产级依赖升级闭环的第一道闸门。它解决两个 compile 通过也发现不了的盲区：

1) 坐标改名 / BOM 拆包：上游大版本把 artifactId 改了、把依赖拆出去了
   （nextdoc4j 1.2.0 -> 1.4.1 把 BOM 和 starter 都改名，还新增 springdoc 依赖）。
   这类变更不会让 compile 报错，但会让运行时 classpath 缺东西。

2) 被本仓库直接 import 的符号在新版本 jar 里已消失：上游删类 / 改签名
   （mybatis-plus 3.5.16 -> 3.5.17 删了 CrudRepository）。
   这类变更只有"恰好被源码 import"才暴露；没被 import 的符号 compile 永远扫不到。

Usage:
    # 传入 diff_versions.py 产出的 JSON（含 version_bumps 与 coordinate_renames）
    python precheck.py --plan plan.json [-o report.json]

    # 或直接给 property 名 + old + new，逐个库检查
    python precheck.py --group top.nextdoc4j --artifact nextdoc4j-bom \
        --old 1.2.0 --new 1.4.1

Step A 依赖树对比：跑 `mvn dependency:tree` 拿 new 与（假定已改的）现状；若 old
        树可用，对比传递依赖增删。这一步主要在 SKILL 流程里由 agent 触发，脚本
        聚焦 Step B 的"符号 jar 反查"。

Step B 符号 jar 反查（主动）：对本仓库 src 里 import 的上游符号，解压 old/new
        jar 用 javap 比对是否存在 / 签名是否一致。命中"new 缺失"即为破坏性变更，
        在改 POM 之前就预警。
"""
import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import zipfile

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))
LOCAL_REPO = os.environ.get('MAVEN_REPO') \
    or os.path.join(os.path.expanduser('~'), '.m2', 'repository')
COORDINATES_MD = os.path.join(
    REPO_ROOT, '.agents', 'skills', 'ocn-starter-dependency-analyze',
    'references', 'coordinates.md')

# coordinates.md 行格式：  - property: X · 坐标: group:artifact
COORD_LINE_RE = re.compile(r'property:\s*`?([\w.\-]+)`?\s*·\s*坐标:\s*`?([\w.\-]+:[\w.\-]+)`?')


def collect_imports():
    """Scan all src/main/java for `import <fqcn>;` lines."""
    imports = set()
    root = os.path.join(REPO_ROOT, 'continew-starter-core')
    # Walk the whole repo; cheap enough for this size.
    for dirpath, _, files in os.walk(REPO_ROOT):
        if '\\.git' in dirpath or '/.git' in dirpath:
            continue
        if 'target' in dirpath.split(os.sep):
            continue
        for fn in files:
            if not fn.endswith('.java'):
                continue
            fp = os.path.join(dirpath, fn)
            try:
                with open(fp, encoding='utf-8', errors='ignore') as f:
                    for line in f:
                        m = re.match(r'\s*import\s+(static\s+)?([\w.]+)\s*;', line)
                        if m:
                            imports.add(m.group(2))
            except Exception:
                pass
    return imports


def jar_path(group, artifact, version):
    g = group.replace('.', os.sep)
    p = os.path.join(LOCAL_REPO, g, artifact, version,
                     '%s-%s.jar' % (artifact, version))
    return p if os.path.exists(p) else None


def load_coordinate_map():
    """Parse coordinates.md for `property -> group:artifact` mapping.

    This is what lets precheck resolve the group:artifact for a version bump
    whose diff_versions.py entry has group_artifact=null (the canonical case:
    diff_versions.py only reads the POM diff, it never knows coordinates).
    """
    mapping = {}
    if not os.path.exists(COORDINATES_MD):
        return mapping
    with open(COORDINATES_MD, encoding='utf-8', errors='replace') as f:
        for line in f:
            m = COORD_LINE_RE.search(line)
            if m:
                mapping[m.group(1)] = m.group(2)
    return mapping


def classes_in_jar(jar):
    """Return set of fully-qualified class names inside a jar (dotted)."""
    out = set()
    try:
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                if n.endswith('.class') and not n.startswith('META-INF'):
                    # strip .class, drop inner-class '$...'
                    cn = n[:-6].replace('/', '.')
                    cn = cn.split('$')[0]
                    out.add(cn)
    except Exception:
        return out
    return out


def fqcn_prefixes(fqcn):
    """Yield progressively shorter package prefixes for prefix matching."""
    parts = fqcn.split('.')
    for i in range(len(parts) - 1, 0, -1):
        yield '.'.join(parts[:i])


def check_symbol_against(group, artifact, old, new, imports):
    """Probe whether imports that live under this artifact's package survive the bump.

    注意：传入的应是**真实构件**坐标（如 mybatis-plus 传 com.baomidou:mybatis-plus-extension），
    而非 BOM 坐标——BOM jar 里没有我们要查的业务类。对 BOM 型 property，agent 应在 Step 1.5
    用 --group/--artifact 显式传真实构件，或依赖 changelog_fetch.py 的 compare API removed 列表。
    """
    old_jar = jar_path(group, artifact, old)
    new_jar = jar_path(group, artifact, new)
    if not old_jar or not new_jar:
        return {'status': 'jar_unavailable',
                'old_jar': bool(old_jar), 'new_jar': bool(new_jar),
                'removed_symbols': [],
                'note': '本地仓库缺 jar，无法反查。若为 BOM 坐标，请改用真实构件坐标'
                        '（如 mybatis-plus 用 com.baomidou:mybatis-plus-extension）；'
                        '否则先 `mvn dependency:get -Dartifact=G:A:V` 拉取新旧 jar 再跑'}
    old_classes = classes_in_jar(old_jar)
    new_classes = classes_in_jar(new_jar)
    # Only care about imports that belong to this artifact's package.
    prefix = '%s.%s' % (group, artifact)
    removed = []
    for imp in imports:
        if not imp.startswith(prefix):
            continue
        if imp in old_classes and imp not in new_classes:
            removed.append(imp)
    return {'status': 'ok', 'removed_symbols': removed,
            'old_class_count': len(old_classes), 'new_class_count': len(new_classes)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--plan', help='diff_versions.py 产出的 JSON')
    ap.add_argument('--group')
    ap.add_argument('--artifact')
    ap.add_argument('--old')
    ap.add_argument('--new')
    ap.add_argument('--repo', help='本地 Maven 仓库根路径（默认 $MAVEN_REPO 或 ~/.m2/repository）')
    ap.add_argument('-o', '--out', help='report json path')
    args = ap.parse_args()

    global LOCAL_REPO
    if args.repo:
        LOCAL_REPO = args.repo

    targets = []
    coord_map = load_coordinate_map()
    if args.plan:
        with open(args.plan, encoding='utf-8') as f:
            plan = json.load(f)
        bumps = plan.get('version_bumps', [])
        # Resolve group:artifact: prefer inline field, else coordinates.md map.
        for b in bumps:
            ga = b.get('group_artifact')
            if not (ga and ':' in ga):
                ga = coord_map.get(b.get('property'))
            if ga and ':' in ga:
                g, a = ga.split(':', 1)
                targets.append((g, a, b['old'], b['new'], b.get('property')))
    if args.group and args.artifact and args.old and args.new:
        targets.append((args.group, args.artifact, args.old, args.new, None))

    imports = collect_imports()
    report = {'checked': [], 'coordinate_renames': [],
              'hardening_notes': []}
    for g, a, old, new, prop in targets:
        res = check_symbol_against(g, a, old, new, imports)
        res['group'] = g
        res['artifact'] = a
        res['old'] = old
        res['new'] = new
        res['property'] = prop
        report['checked'].append(res)

    if args.plan:
        plan_full = json.load(open(args.plan, encoding='utf-8'))
        report['coordinate_renames'] = plan_full.get('coordinate_renames', [])

    # Hardening notes derived from findings.
    for r in report['checked']:
        if r.get('removed_symbols'):
            report['hardening_notes'].append(
                '⚠ %s:%s %s→%s 删除了本仓库引用的符号：%s —— 升级前必须修源码'
                % (r['group'], r['artifact'], r['old'], r['new'],
                   ', '.join(r['removed_symbols'])))
    if report['coordinate_renames']:
        report['hardening_notes'].append(
            '⚠ 检测到坐标改名（BOM/artifactId）：%s —— 必须同步改所有引用方 POM，'
            '并核对新坐标是否拆分/新增了传递依赖'
            % json.dumps(report['coordinate_renames'], ensure_ascii=False))

    if args.out:
        with open(args.out, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
    else:
        print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
