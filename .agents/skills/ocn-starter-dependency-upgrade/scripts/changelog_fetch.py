#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Fetch upstream changelog / release notes for a version bump interval.

Usage:
    python changelog_fetch.py --group org.springframework.cloud \
        --artifact spring-cloud-dependencies --from 2025.0.1 --to 2025.0.3
    python changelog_fetch.py --property spring-boot.version --from 3.5.13 --to 3.5.16
    python changelog_fetch.py --property hutool.version --from 5.8.44 --to 5.8.47
    # jar 反查兜底：显式给 group/artifact/to + 要核对的类
    python changelog_fetch.py --group com.baomidou --artifact mybatis-plus-bom \
        --to 3.5.17 --class com.baomidou.mybatisplus.extension.repository.CrudRepository

For github-releases sources, when --from/--to are both given the script first
calls the GitHub compare API (via `gh api` if installed, else unauthenticated
curl/urllib) and surfaces removed/renamed .java files — the strongest breaking
signals. It then appends the release notes text. If the API yields nothing
useful, pass --class to get a ready-to-run `mvn dependency:get` + `javap`
reverse-lookup recipe (jar never lies about real class signatures).

This is a READ-ONLY network helper. It does NOT edit anything. Output is plain
text (the breaking-change digest) for the agent to read; pass -o to dump JSON.

Zero third-party deps: urllib + re + subprocess(gh) only.
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request

TIMEOUT = 25

# Curated official changelog / release-notes / migration entry points.
# key = group:artifact OR property name. 'type' drives how we parse.
#   'github-releases' : GitHub Releases API (per-tag or list), we filter tags in [from,to]
#   'github-tags'     : same as releases but repo uses tags page
#   'migration'       : a static migration doc URL (version-agnostic, fetched as-is)
#   'changelog-md'    : a CHANGELOG.md raw URL (fetched, grepped for version headers)
CHANGELOG_SOURCES = {
    'org.springframework.boot:spring-boot-dependencies': dict(
        type='github-releases', repo='spring-projects/spring-boot'),
    'org.springframework.cloud:spring-cloud-dependencies': dict(
        type='github-releases', repo='spring-cloud/spring-cloud-release'),
    'com.baomidou:mybatis-plus-bom': dict(
        type='github-releases', repo='baomidou/mybatis-plus'),
    'com.mybatis-flex:mybatis-flex-dependencies': dict(
        type='github-releases', repo='mybatis-flex/mybatis-flex'),
    'cn.hutool:hutool-bom': dict(
        type='changelog-md',
        url='https://raw.githubusercontent.com/chinabugotech/hutool/master/CHANGELOG.md'),
    'com.alicp.jetcache:jetcache-bom': dict(
        type='github-releases', repo='alicp/jetcache'),
    'cn.dev33:sa-token-bom': dict(
        type='github-releases', repo='dromara/Sa-Token'),
    'software.amazon.awssdk:bom': dict(
        type='changelog-md',
        url='https://raw.githubusercontent.com/aws/aws-sdk-java-v2/main/CHANGELOG.md'),
    'software.amazon.awssdk.crt:aws-crt': dict(
        type='github-releases', repo='awslabs/aws-crt-java'),
    'me.ahoo.cosid:cosid-bom': dict(
        type='github-releases', repo='Ahoo-Wang/CosId'),
    'com.aizuda:snail-job-client-starter': dict(
        type='github-releases', repo='aizuda/snail-job'),
    'cn.crane4j:crane4j-spring-boot-starter': dict(
        type='github-releases', repo='crane4j/crane4j'),
    'io.swagger.core.v3:swagger-annotations-jakarta': dict(
        type='github-releases', repo='swagger-api/swagger-core'),
    'com.diffplug.spotless:spotless-maven-plugin': dict(
        type='github-releases', repo='diffplug/spotless'),
    'org.codehaus.mojo:flatten-maven-plugin': dict(
        type='github-releases', repo='mojohaus/flatten-maven-plugin'),
    'org.sonarsource.scanner.maven:sonar-maven-plugin': dict(
        type='github-releases', repo='SonarSource/sonar-maven-plugin'),
    'top.nextdoc4j:nextdoc4j-bom-springboot3': dict(
        type='github-releases', repo='nextdoc4j/nextdoc4j'),
    'cn.idev.excel:fastexcel': dict(
        type='github-releases', repo='fast-excel/fastexcel'),
    'org.apache.poi:poi': dict(
        type='changelog-md',
        url='https://raw.githubusercontent.com/apache/poi/trunk/RELEASE-NOTES.txt'),
    # property-name aliases so --property works directly:
    'spring-boot.version': dict(ref='org.springframework.boot:spring-boot-dependencies'),
    'spring-cloud.version': dict(ref='org.springframework.cloud:spring-cloud-dependencies'),
    'mybatis-plus.version': dict(ref='com.baomidou:mybatis-plus-bom'),
    'mybatis-flex.version': dict(ref='com.mybatis-flex:mybatis-flex-dependencies'),
    'hutool.version': dict(ref='cn.hutool:hutool-bom'),
    'jetcache.version': dict(ref='com.alicp.jetcache:jetcache-bom'),
    'sa-token.version': dict(ref='cn.dev33:sa-token-bom'),
    'aws-sdk.version': dict(ref='software.amazon.awssdk:bom'),
    'aws-crt.version': dict(ref='software.amazon.awssdk.crt:aws-crt'),
    'cosid.version': dict(ref='me.ahoo.cosid:cosid-bom'),
    'snail-job.version': dict(ref='com.aizuda:snail-job-client-starter'),
    'crane4j.version': dict(ref='cn.crane4j:crane4j-spring-boot-starter'),
    'swagger.version': dict(ref='io.swagger.core.v3:swagger-annotations-jakarta'),
    'spotless.version': dict(ref='com.diffplug.spotless:spotless-maven-plugin'),
    'flatten.version': dict(ref='org.codehaus.mojo:flatten-maven-plugin'),
    'sonar.version': dict(ref='org.sonarsource.scanner.maven:sonar-maven-plugin'),
    'nextdoc4j.version': dict(ref='top.nextdoc4j:nextdoc4j-bom-springboot3'),
    'fastexcel.version': dict(ref='cn.idev.excel:fastexcel'),
    'poi.version': dict(ref='org.apache.poi:poi'),
}

VERSION_HEADER_RE = re.compile(r'^#{1,4}\s*[vV]?(\d[\w.\-]+)', re.MULTILINE)


def resolve_source(key):
    src = CHANGELOG_SOURCES.get(key)
    while src and 'ref' in src:
        src = CHANGELOG_SOURCES.get(src['ref'])
    return src


def fetch(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'dep-verify/1.0'})
    with urllib.request.urlopen(req, timeout=TIMEOUT) as r:
        return r.read().decode('utf-8', errors='replace')


def in_interval(ver, lo, hi):
    """Loose inclusive interval check on version strings (same lineage only)."""
    def norm(v):
        return [int(x) if x.isdigit() else x for x in re.findall(r'\d+|[A-Za-z]+', v)]
    try:
        a, b, c = norm(ver), norm(lo), norm(hi)
    except Exception:
        return True
    return a >= b and a <= c


def _gh_api(path, params=None):
    """Call `gh api` if available (authenticated, higher rate limit); fallback curl.
    Returns decoded JSON or None on failure."""
    q = ''
    if params:
        q = '?' + '&'.join(f'{k}={v}' for k, v in params.items())
    url = f'https://api.github.com/{path}{q}'
    if shutil.which('gh'):
        try:
            cmd = ['gh', 'api', url]
            out = subprocess.run(cmd, capture_output=True, text=True, timeout=TIMEOUT + 10)
            if out.returncode == 0 and out.stdout.strip():
                return json.loads(out.stdout)
            # gh present but auth failed (e.g. 401 Bad credentials): tell the
            # user how to fix instead of silently falling back.
            if 'Bad credentials' in out.stderr or '401' in out.stderr:
                sys.stderr.write(
                    '[gh] 登录态失效（token 无效）。请先刷新凭证：\n'
                    '      gh auth refresh -h github.com   # keyring 已有账号时\n'
                    '      或 gh auth login                 # 首次/账号丢失\n'
                    '    正在回退到未鉴权 API（限流 60/h，足够单次升级）。\n')
        except Exception:
            pass
    # fallback: unauthenticated curl/urllib
    try:
        return json.loads(fetch(url))
    except Exception:
        return None


def github_compare(repo, fro, to):
    """Use GitHub compare API to surface deleted/renamed/changed .java files.
    These are the strongest signals of a breaking change (e.g. a class removed).
    Note: compare API caps the `files` array at 250 commits; for very large
    version spans the list may be truncated — combine with jar反查 fallback."""
    if not fro or not to:
        return ''
    data = _gh_api(f'repos/{repo}/compare/v{fro.lstrip("vV")}...v{to.lstrip("vV")}')
    if not data or 'files' not in data:
        return ''
    suspicious = []
    for f in data.get('files', []):
        fn = f.get('filename', '')
        status = f.get('status', '')
        if not fn.endswith('.java'):
            continue
        # removed / renamed = most likely breaking; heavily modified = worth a look
        if status in ('removed', 'renamed'):
            suspicious.append(f'  [BREAKING?] {status:8} {fn}'
                              + (f'  -> {f.get("previous_filename")}' if f.get('previous_filename') else ''))
        elif status == 'modified' and (f.get('deletions', 0) >= 20 or f.get('changes', 0) >= 50):
            suspicious.append(f'  [changed]   {status:8} {fn} '
                              f'(+{f.get("additions",0)}/-{f.get("deletions",0)})')
    if not suspicious:
        return ('  (no .java file removals/renames detected via compare API)\n'
                '   ⚠ 盲区：compare 只能发现"区间内被删"的文件，无法发现"旧版已删、'
                '本仓库仍引用"的类（如某类早被上游移除）。\n'
                '   若 compile 报 cannot find symbol 但此处未列出该类，务必用 --class 走 jar 反查。')
    head = (f'### compare v{fro}...v{to} — suspected breaking .java changes in {repo}\n'
            f'    (compare API caps files at 250 commits; truncated spans need jar反查)\n'
            f'    ⚠ 盲区：以上只覆盖"区间内变动"；旧版已删但本仓库仍引用的类不会出现在列表，'
            f'需配合 --class jar 反查。\n')
    return head + '\n'.join(suspicious)


def github_releases(repo, fro, to):
    # GitHub Releases list API (public, unauthenticated, low rate limit).
    api = f'https://api.github.com/repos/{repo}/releases?per_page=100'
    try:
        data = json.loads(fetch(api))
    except Exception as e:
        return f'[offline/failed] GitHub releases for {repo}: {e}'
    out = []
    for rel in data:
        tag = rel.get('tag_name', '') or rel.get('name', '')
        if fro and to and not in_interval(tag.lstrip('vV'), fro, to):
            continue
        body = rel.get('body', '') or ''
        out.append(f'## {tag}\n{body[:4000]}')
    if not out:
        return f'[no releases in {fro}..{to} for {repo}]'
    return '\n\n'.join(out)


def changelog_md(url, fro, to):
    try:
        txt = fetch(url)
    except Exception as e:
        return f'[offline/failed] {url}: {e}'
    # Split by version headers, keep sections whose header is in interval.
    parts = VERSION_HEADER_RE.split(txt)
    # parts: [pre, v1, body1, v2, body2, ...]
    kept = []
    for i in range(1, len(parts), 2):
        ver = parts[i]
        body = parts[i + 1] if i + 1 < len(parts) else ''
        if (not fro or not to) or in_interval(ver, fro, to):
            kept.append(f'## {ver}\n{body[:3000]}')
    if not kept:
        return f'[no version sections in {fro}..{to}]\n--- first 2000 chars ---\n{txt[:2000]}'
    return '\n\n'.join(kept)


def jar_reverse_lookup_hint(group, artifact, version, classname=None):
    """Print a ready-to-run fallback recipe when the GitHub API yields nothing
    useful. Downloads the artifact via Maven, unzips, and javap-greps the real
    class signatures on the classpath — 100% reflects what compile sees, unlike
    release notes which may omit breaking changes (e.g. MyBatis-Plus 3.5.17
    removing CrudRepository)."""
    g = group.replace('.', '/')
    lines = [
        '### jar 反查兜底（API 拉不到或想核对真实签名时）',
        'release notes 可能漏写破坏性变更；jar 不会骗人。步骤：',
        f'1) mvn dependency:get -Dartifact={group}:{artifact}:{version}',
        f'2) 找到 ~/.m2/repository/{g}/{artifact}/{version}/*.jar，解压到临时目录',
        f'3) javap -classpath <解压目录> <全限定类名>   # 例如 top.continew... 或上游类',
    ]
    if classname:
        lines.append(f'   重点核对: {classname}')
    lines.append('   对比旧版本 jar 的同类 javap 输出，差异即破坏性变更。')
    return '\n'.join(lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--group')
    ap.add_argument('--artifact')
    ap.add_argument('--property')
    ap.add_argument('--from', dest='fro')
    ap.add_argument('--to')
    ap.add_argument('--class', dest='cls', help='optional: fully-qualified class to jar-reverse-lookup')
    ap.add_argument('-o', dest='out')
    a = ap.parse_args()

    key = a.property
    if not key and a.group and a.artifact:
        key = f'{a.group}:{a.artifact}'
    if not key:
        print('[error] pass --property X or --group/--artifact', file=sys.stderr)
        sys.exit(2)
    src = resolve_source(key)
    if not src:
        print(f'[unknown] no curated changelog source for {key}; '
              f'fall back to WebSearch: "{key} {a.fro} to {a.to} breaking changes"')
        sys.exit(0)

    if src['type'] == 'github-releases':
        parts = []
        # 1) compare API: structural diff (removed/renamed .java = breaking signals)
        cmp = github_compare(src['repo'], a.fro, a.to)
        if cmp:
            parts.append(cmp)
        # 2) release notes: human-readable change summary
        rel = github_releases(src['repo'], a.fro, a.to)
        if rel and not rel.startswith('[no releases'):
            parts.append(rel)
        # 3) jar 反查兜底（显式 --class 或 compare/release 都空时提示）
        if a.cls and a.group and a.artifact:
            parts.append(jar_reverse_lookup_hint(
                a.group, a.artifact, a.to or '', a.cls))
        elif not parts and a.group and a.artifact and a.to:
            parts.append(jar_reverse_lookup_hint(a.group, a.artifact, a.to, None))
        text = '\n\n'.join(parts) if parts else (
            cmp or rel or f'[no changelog data for {key} {a.fro}..{a.to}]')
    elif src['type'] == 'changelog-md':
        text = changelog_md(src['url'], a.fro, a.to)
    else:
        text = f'[unsupported source type {src["type"]}]'

    if a.out:
        with open(a.out, 'w', encoding='utf-8') as f:
            f.write(text)
    else:
        print(text)


if __name__ == '__main__':
    main()
