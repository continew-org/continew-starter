#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Dependency-outdated scanner for continew-starter.

Reads `continew-starter-dependencies/pom.xml` <properties> for the ~43 locked
version properties, fetches each coordinate's Maven Central maven-metadata.xml,
computes the upgrade tier (Patch/Minor/Major) plus same-line-latest and absolute
release, merges curated upgrade notes from COORDINATES, and emits one JSON blob
to stdout for build_report.py / server.py to render.

STRICTLY READ-ONLY on the repo; only network reads (repo1.maven.org:443) + POM
parse. Never edits the POM (that's server.py's job, gated).

Output shape:
{
  "generated_at", "scan_seconds", "offline",
  "repo": {"pom_path", "revision"},
  "summary": {"total", "outdated", "green", "yellow", "red", "up_to_date",
              "unreachable", "tier_counts"},
  "top5": [{rank, property, artifact, tier, locked, target, gap, type, note}],
  "items": [{
     property, group_id, artifact_id, metadata_path, locked,
     release, same_line_latest, tier, target, gap_versions, gap,
     notes, constraints, unreachable, error
  }, ...],
  "unreachable": [property, ...]
}

Why maven-metadata.xml and not search.maven.org: see docs/adr/0001.
"""
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))
POM_PATH = os.path.join(REPO_ROOT, 'continew-starter-dependencies', 'pom.xml')
CENTRAL = 'https://repo1.maven.org/maven2'
TIMEOUT = 20
# 网络抓取重试：Maven Central 偶发抖动会误报 unreachable。指数退避后重试，
# 显著减少单点失败。MAX_RETRIES 含首次请求，故实际重试 MAX_RETRIES-1 次。
MAX_RETRIES = 4
RETRY_BACKOFF = 1.5  # 每次重试的基准延迟（秒），实际延迟 = BACKOFF * 2**(attempt-1)

# Coordinate catalog. Mirrors references/coordinates.md. Keys = version property
# name as it appears in <properties>. MUST stay in sync with coordinates.md when
# POM adds/removes deps. group_id/artifact_id case-sensitive (Central path rule).
# 'family' drives grouping in the report; 'notes'/'constraints' carry curated text.
COORDINATES = {
    # --- platform (coupled) ---
    'spring-boot.version': dict(group_id='org.springframework.boot', artifact_id='spring-boot-dependencies',
        family='平台级', constraints='与 spring-cloud 强联动，升 Boot 必查 Cloud 兼容矩阵'),
    'spring-cloud.version': dict(group_id='org.springframework.cloud', artifact_id='spring-cloud-dependencies',
        family='平台级', constraints='release train 须匹配 Spring Boot 版本区间'),
    # --- ORM / data ---
    'mybatis-plus.version': dict(group_id='com.baomidou', artifact_id='mybatis-plus-bom',
        family='ORM/数据', constraints='与 data-mp 耦合；InnerInterceptor seam 对 MP 内部 API 敏感'),
    'mybatis-flex.version': dict(group_id='com.mybatis-flex', artifact_id='mybatis-flex-dependencies',
        family='ORM/数据', constraints='与 data-mp 互斥；data-mf 有独立数据权限 SPI'),
    'dynamic-datasource.version': dict(group_id='com.baomidou', artifact_id='dynamic-datasource-spring-boot3-starter',
        family='ORM/数据', constraints='注意 spring-boot3 后缀，勿抓 Boot2 版'),
    'p6spy.version': dict(group_id='p6spy', artifact_id='p6spy', family='ORM/数据'),
    # --- auth ---
    'sa-token.version': dict(group_id='cn.dev33', artifact_id='sa-token-bom',
        family='认证', constraints='运行时构件 sa-token-spring-boot3-starter；勿抓父 sa-token'),
    'justauth.version': dict(group_id='me.zhyd.oauth', artifact_id='JustAuth',
        family='认证', constraints='artifactId 首字母大写；与 sa-token 互补'),
    # --- cache ---
    'redisson.version': dict(group_id='org.redisson', artifact_id='redisson-spring-boot-starter',
        family='缓存', constraints='缓存地基，Spring Cache/JetCache 建于其上'),
    'jetcache.version': dict(group_id='com.alicp.jetcache', artifact_id='jetcache-bom',
        family='缓存', constraints='建在 redisson starter 上，与 Spring Cache 互补'),
    # --- excel / doc ---
    'fastexcel.version': dict(group_id='cn.idev.excel', artifact_id='fastexcel',
        family='Excel/文档', constraints='与 poi 互斥；CRUD core 显式依赖 FastExcel'),
    'poi.version': dict(group_id='org.apache.poi', artifact_id='poi',
        family='Excel/文档', constraints='与 fastexcel 互斥；poi-ooxml 同 property'),
    'nextdoc4j.version': dict(group_id='top.nextdoc4j', artifact_id='nextdoc4j-bom-springboot3',
        family='Excel/文档', constraints='注意 springboot3 后缀'),
    # --- file storage ---
    'x-file-storage.version': dict(group_id='org.dromara.x-file-storage', artifact_id='x-file-storage-spring',
        family='文件存储'),
    'aws-sdk-v1.version': dict(group_id='com.amazonaws', artifact_id='aws-java-sdk-s3',
        family='文件存储', constraints='SDK v1 维护模式，仅取安全补丁，长期迁 v2'),
    'aws-sdk.version': dict(group_id='software.amazon.awssdk', artifact_id='bom',
        family='文件存储', constraints='artifactId 字面量 bom；与 aws-crt 配套'),
    'aws-crt.version': dict(group_id='software.amazon.awssdk.crt', artifact_id='aws-crt',
        family='文件存储'),
    'thumbnails.version': dict(group_id='net.coobird', artifact_id='thumbnailator',
        family='文件存储'),
    # --- task / comm ---
    'cosid.version': dict(group_id='me.ahoo.cosid', artifact_id='cosid-bom',
        family='任务/通信', constraints='分布式 ID，与数据层耦合'),
    'snail-job.version': dict(group_id='com.aizuda', artifact_id='snail-job-client-starter',
        family='任务/通信', constraints='三构件共用一 property，升一全升'),
    'sms4j.version': dict(group_id='org.dromara.sms4j', artifact_id='sms4j-spring-boot-starter',
        family='任务/通信'),
    'paho-mqttv3.version': dict(group_id='org.eclipse.paho', artifact_id='org.eclipse.paho.client.mqttv3',
        family='任务/通信', constraints='artifactId 含点号，路径原样保留'),
    # --- captcha ---
    'aj-captcha.version': dict(group_id='com.anji-plus', artifact_id='captcha',
        family='验证码'),
    'easy-captcha.version': dict(group_id='com.github.whvcse', artifact_id='easy-captcha',
        family='验证码'),
    # --- web / api ---
    'graceful-response.version': dict(group_id='com.feiniaojin', artifact_id='graceful-response',
        family='Web/API', constraints='版本号带 -boot3 后缀（Boot3 分支标记，非预发布）'),
    'spel-validator.version': dict(group_id='cn.sticki', artifact_id='spel-validator-jakarta',
        family='Web/API'),
    'crane4j.version': dict(group_id='cn.crane4j', artifact_id='crane4j-spring-boot-starter',
        family='Web/API'),
    'swagger.version': dict(group_id='io.swagger.core.v3', artifact_id='swagger-annotations-jakarta',
        family='Web/API', constraints='与 springdoc 版本需匹配；显式锁可能与 Boot 冲突'),
    'tlog.version': dict(group_id='com.yomahub', artifact_id='tlog-web-spring-boot-starter',
        family='Web/API'),
    # --- tools / misc ---
    'hutool.version': dict(group_id='cn.hutool', artifact_id='hutool-bom',
        family='工具类', constraints='core 重度依赖 hutool-all；6.x 是大重构'),
    'snakeyaml.version': dict(group_id='org.yaml', artifact_id='snakeyaml',
        family='工具类', constraints='显式锁，升 Boot 时核对冲突'),
    'nashorn.version': dict(group_id='org.openjdk.nashorn', artifact_id='nashorn-core',
        family='工具类'),
    'commons-fileupload.version': dict(group_id='commons-fileupload', artifact_id='commons-fileupload',
        family='工具类'),
    'commons-beanutils.version': dict(group_id='commons-beanutils', artifact_id='commons-beanutils',
        family='工具类'),
    'commons-io.version': dict(group_id='commons-io', artifact_id='commons-io',
        family='工具类'),
    'commons-compress.version': dict(group_id='org.apache.commons', artifact_id='commons-compress',
        family='工具类'),
    'truelicense.version': dict(group_id='de.schlichtherle.truelicense', artifact_id='truelicense-core',
        family='工具类'),
    'zip4j.version': dict(group_id='net.lingala.zip4j', artifact_id='zip4j',
        family='工具类'),
    'ttl.version': dict(group_id='com.alibaba', artifact_id='transmittable-thread-local',
        family='工具类'),
    'ip2region.version': dict(group_id='net.dreamlu', artifact_id='mica-ip2region',
        family='工具类'),
    # --- build plugins (detected the same way as artifacts; groupIds verified
    #     against Central. Marked plugin=True only for report grouping.) ---
    'flatten.version': dict(group_id='org.codehaus.mojo', artifact_id='flatten-maven-plugin',
        family='构建插件', plugin=True,
        constraints='绑定 process-resources 生成发布用 POM，升级后须确认 flattened POM 内容不变'),
    'spotless.version': dict(group_id='com.diffplug.spotless', artifact_id='spotless-maven-plugin',
        family='构建插件', plugin=True,
        constraints='绑定 compile 阶段会改写源码，升级后跑 spotless:check 确认格式结果一致'),
    'sonar.version': dict(group_id='org.sonarsource.scanner.maven', artifact_id='sonar-maven-plugin',
        family='构建插件', plugin=True,
        constraints='仅 -Psonar 使用，四段版本号（如 5.2.0.4988），不影响主构建'),
}

# Spring Cloud <-> Spring Boot 兼容性矩阵（官方权威，来自
# https://spring.io/projects/spring-cloud#overview ，由用户于 2026-08-02 录入）。
# 键 = Spring Boot 的 "major.minor" 前缀；值 = 对应的 Spring Cloud Release Train。
# 标注 * 的 train 已 EOL；项目当前 Boot 3.5 对应 2025.0.x (Northfields)，仍受支持。
SPRING_CLOUD_COMPAT = {
    '4.1': '2025.1.x (Oakwood)', '4.0': '2025.1.x (Oakwood)',
    '3.5': '2025.0.x (Northfields)', '3.4': '2024.0.x (Moorgate)',
    '3.3': '2023.0.x (Leyton)', '3.2': '2023.0.x (Leyton)',
    '3.1': '2022.0.x (Kilburn)', '3.0': '2022.0.x (Kilburn)',
    '2.7': '2021.0.x (Jubilee)', '2.6': '2021.0.x (Jubilee)',
    '2.5': '2020.0.x (Ilford)', '2.4': '2020.0.x (Ilford)',
    '2.3': 'Hoxton', '2.2': 'Hoxton',
    '2.1': 'Greenwich', '2.0': 'Finchley',
}
SPRING_CLOUD_COMPAT_SRC = 'https://spring.io/projects/spring-cloud#overview'

def boot_to_cloud_train(boot_version):
    """Return the Spring Cloud Release Train name for a given Spring Boot version,
    or None if the Boot version is unknown/out of the matrix."""
    if not boot_version or not isinstance(boot_version, str):
        return None
    parts = boot_version.split('.')
    if len(parts) < 2:
        return None
    return SPRING_CLOUD_COMPAT.get(parts[0] + '.' + parts[1])

def cloud_train_of(cloud_version):
    """Extract the Spring Cloud Release Train key (e.g. '2025.0.x') from a
    spring-cloud.version value like '2025.0.0' / '2024.0.3'."""
    if not cloud_version or not isinstance(cloud_version, str):
        return None
    parts = cloud_version.split('.')
    if len(parts) < 2:
        return None
    return parts[0] + '.' + parts[1] + '.x'

# Release-notes / changelog URLs, keyed by version property. '{v}' is replaced
# with the target version (tag-style links jump straight to that release).
# Every entry links to the upstream's own notes — we never invent changelog text.
CHANGELOG = {
    'spring-boot.version': 'https://github.com/spring-projects/spring-boot/releases/tag/v{v}',
    'spring-cloud.version': 'https://github.com/spring-cloud/spring-cloud-release/releases',
    'mybatis-plus.version': 'https://github.com/baomidou/mybatis-plus/releases/tag/v{v}',
    'mybatis-flex.version': 'https://gitee.com/mybatis-flex/mybatis-flex/releases',
    'dynamic-datasource.version': 'https://github.com/baomidou/dynamic-datasource/releases',
    'p6spy.version': 'https://github.com/p6spy/p6spy/releases',
    'sa-token.version': 'https://github.com/dromara/Sa-Token/releases/tag/v{v}',
    'justauth.version': 'https://github.com/justauth/JustAuth/releases',
    'redisson.version': 'https://github.com/redisson/redisson/releases/tag/{v}',
    'jetcache.version': 'https://github.com/alibaba/jetcache/releases',
    'fastexcel.version': 'https://github.com/CodePhiliaX/fastexcel/releases',
    'poi.version': 'https://poi.apache.org/changes.html',
    'nextdoc4j.version': 'https://github.com/nextdoc4j/nextdoc4j/releases',
    'x-file-storage.version': 'https://github.com/dromara/x-file-storage/releases',
    'aws-sdk-v1.version': 'https://github.com/aws/aws-sdk-java/blob/master/CHANGELOG.md',
    'aws-sdk.version': 'https://github.com/aws/aws-sdk-java-v2/releases/tag/{v}',
    'aws-crt.version': 'https://github.com/awslabs/aws-crt-java/releases/tag/v{v}',
    'thumbnails.version': 'https://github.com/coobird/thumbnailator/releases',
    'cosid.version': 'https://github.com/Ahoo-Wang/CosId/releases/tag/v{v}',
    'snail-job.version': 'https://gitee.com/aizuda/snail-job/releases',
    'sms4j.version': 'https://github.com/dromara/SMS-Aggregation/releases',
    'paho-mqttv3.version': 'https://github.com/eclipse-paho/paho.mqtt.java/releases',
    'aj-captcha.version': 'https://gitee.com/anji-plus/captcha/releases',
    'easy-captcha.version': 'https://github.com/whvcse/EasyCaptcha/releases',
    'graceful-response.version': 'https://github.com/feiniaojin/graceful-response/releases',
    'spel-validator.version': 'https://github.com/stickicn/spel-validator/releases',
    'crane4j.version': 'https://github.com/opengoofy/crane4j/releases',
    'swagger.version': 'https://github.com/swagger-api/swagger-core/releases/tag/v{v}',
    'tlog.version': 'https://github.com/dromara/TLog/releases',
    'hutool.version': 'https://github.com/dromara/hutool/releases/tag/v{v}',
    'snakeyaml.version': 'https://bitbucket.org/snakeyaml/snakeyaml/wiki/Changes',
    'nashorn.version': 'https://github.com/openjdk/nashorn/releases',
    'commons-fileupload.version': 'https://commons.apache.org/proper/commons-fileupload/changes.html',
    'commons-beanutils.version': 'https://commons.apache.org/proper/commons-beanutils/changes.html',
    'commons-io.version': 'https://commons.apache.org/proper/commons-io/changes.html',
    'commons-compress.version': 'https://commons.apache.org/proper/commons-compress/changes.html',
    'truelicense.version': 'https://github.com/christian-schlichtherle/truelicense/releases',
    'zip4j.version': 'https://github.com/srikanth-lingala/zip4j/releases',
    'ttl.version': 'https://github.com/alibaba/transmittable-thread-local/releases/tag/v{v}',
    'ip2region.version': 'https://gitee.com/dromara/mica-ip2region/releases',
    'flatten.version': 'https://github.com/mojohaus/flatten-maven-plugin/releases',
    'spotless.version': 'https://github.com/diffplug/spotless/blob/main/CHANGES.md',
    'sonar.version': 'https://github.com/SonarSource/sonar-scanner-maven/releases',
}


def changelog_url(prop, version):
    """Changelog link for a property, with '{v}' bound to the target version.
    Returns None when we have no curated link (better than a guessed 404)."""
    tpl = CHANGELOG.get(prop)
    if not tpl:
        return None
    return tpl.replace('{v}', str(version)) if version else tpl.split('/tag/')[0]


_GH_RE = re.compile(r'github\.com/([^/]+)/([^/]+?)(?:/|$)')


def github_repo_of(prop):
    """Best-effort GitHub 'owner/repo' parsed from a property's changelog URL.
    Used to build compare (interval) links between two versions. Returns None
    when the upstream isn't on github.com (e.g. Gitee / Apache / Bitbucket)."""
    url = CHANGELOG.get(prop, '')
    m = _GH_RE.search(url)
    if not m:
        return None
    owner, repo = m.group(1), m.group(2)
    # some changelog URLs point at a tag-style path; strip trailing '/releases'
    return '{}/{}'.format(owner, repo)


def compare_url(prop, from_ver, to_ver):
    """GitHub compare link: all commits/changes between two (tag-prefixed) versions.
    Only generated for upstreams whose changelog URL carries a '/tag/v{v}' template,
    so the link targets real git tags (e.g. v3.5.13...v3.5.16) and never 404s.
    Upstreams with bare '{v}' tag templates (redisson -> 'redisson-3.x.x',
    aws-sdk-v2 -> 'v2.x.x') have non-derivable tag names, so we skip them rather
    than emit a broken link. Returns None in all such cases."""
    if not from_ver or not to_ver or from_ver == to_ver:
        return None
    tpl = CHANGELOG.get(prop, '')
    if '/tag/v{v}' not in tpl:
        return None
    repo = github_repo_of(prop)
    if not repo:
        return None
    return 'https://github.com/{}/compare/v{}...v{}'.format(repo, from_ver, to_ver)

# True pre-release qualifiers. A hyphen suffix is only a pre-release when it
# matches one of these; suffixes like '-boot3' / '-jre' / '-jakarta' are LINEAGE
# markers (a parallel GA release line), not pre-releases. Treating them as
# pre-releases used to make every graceful-response version non-GA, which is why
# absolute_release() fell back to the stale <release> field and reported a
# bogus "downgrade".
PRERELEASE_RE = re.compile(
    r'[-.](?:M|RC|CR|A|B|ALPHA|BETA|EA|PRE|SNAPSHOT|DEV|INCUBATING|TEMP)\d*$',
    re.I)

# Lineage suffix: a trailing '-<word>' that marks a parallel release line
# (e.g. '5.0.5-boot3', '32.0.0-jre', '2.0-jakarta'). Versions only compare
# meaningfully within the same lineage.
LINEAGE_RE = re.compile(r'-([A-Za-z][A-Za-z0-9]*)$')


def lineage_of(v):
    """'5.0.5-boot3' -> 'boot3'; '3.5.16' -> ''. Pre-release qualifiers are not
    lineages. Used to keep -boot3 and -boot2 from being compared against each
    other (they're separate lines, both GA)."""
    s = str(v).strip()
    if PRERELEASE_RE.search(s):
        return ''
    m = LINEAGE_RE.search(s)
    return m.group(1).lower() if m else ''


def parse_version(v):
    """Numeric version tuple, 4 segments. '3.5.13'->(3,5,13,0);
    '5.2.0.4988'->(5,2,0,4988) (sonar-maven-plugin uses 4 segments);
    '5.0.5-boot3'->(5,0,5,0). Keeping the 4th segment matters: truncating to 3
    made every 5.2.0.x sonar build look identical."""
    m = re.match(r'\s*(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:\.(\d+))?', str(v))
    if not m:
        return (0, 0, 0, 0)
    return tuple(int(g) if g is not None else 0 for g in m.groups())


def is_ga(v):
    """GA = not a pre-release. Lineage suffixes ('-boot3', '-jre') stay GA."""
    return not PRERELEASE_RE.search(str(v).strip())


def fetch_pom_last_modified(group_id, artifact_id, version):
    """Best-effort publish timestamp for ONE version, from the .pom file's HTTP
    Last-Modified header on Central. Used to show 'staleness' (how long a locked
    version has sat unchanged) and when each upgrade target was released.
    Returns an RFC1123 string ('Thu, 26 Mar 2026 10:12:18 GMT') or None on any
    failure. Central-grade HEAD requests are cheap; we cache by coordinate+version
    so a locked version and its targets don't re-hit the network."""
    cache_key = (group_id, artifact_id, version)
    if cache_key in _LM_CACHE:
        return _LM_CACHE[cache_key]
    val = None
    path = '{}/{}/{}/{}-{}.pom'.format(
        group_id.replace('.', '/'), artifact_id, version, artifact_id, version)
    url = CENTRAL + '/' + path
    req = urllib.request.Request(url, method='HEAD',
                                 headers={'User-Agent': 'continew-starter-dep-upgrade/1.0'})
    # 网络抖动常见，重试（指数退避）以避免误报 unreachable。
    for attempt in range(MAX_RETRIES):
        try:
            with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
                val = resp.headers.get('Last-Modified')
            break
        except Exception:
            val = None
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BACKOFF * (2 ** attempt))
    _LM_CACHE[cache_key] = val
    return val


# Memoize Last-Modified lookups (a single artifact's locked version and its
# targets would otherwise each trigger a separate HEAD request).
_LM_CACHE = {}


def fetch_metadata(group_id, artifact_id):
    """Return (versions_list, release_str, last_updated) or raise.
    Central layout: maven2/<group-path>/<artifact>/maven-metadata.xml
    (artifact appears once — NOT group/artifact/artifact).

    NOTE: <release> is NOT always GA (e.g. jetcache-bom has <release>2.8.0.RC).
    Callers must recompute the absolute release as max(GA versions) via
    absolute_release(), not trust this field directly."""
    path = '{}/{}/maven-metadata.xml'.format(
        group_id.replace('.', '/'), artifact_id)
    url = CENTRAL + '/' + path
    req = urllib.request.Request(url, headers={'User-Agent': 'continew-starter-dep-upgrade/1.0'})
    # 网络抖动常见，重试（指数退避）以避免误报 unreachable。
    last_err = None
    for attempt in range(MAX_RETRIES):
        try:
            with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
                data = resp.read().decode('utf-8')
            break
        except Exception as e:
            last_err = e
            if attempt < MAX_RETRIES - 1:
                time.sleep(RETRY_BACKOFF * (2 ** attempt))
    else:
        # 全部重试仍失败，抛出最后一次异常（由调用方标 unreachable）。
        raise last_err if last_err is not None else RuntimeError('maven-metadata.xml 抓取失败')
    root = ET.fromstring(data)
    versioning = root.find('versioning')
    versions = [v.text.strip() for v in versioning.findall('versions/version') if v.text]
    release_el = versioning.find('release')
    release = release_el.text.strip() if release_el is not None and release_el.text else None
    lu = versioning.find('lastUpdated')
    last_updated = lu.text.strip() if lu is not None and lu.text else None
    return versions, release, last_updated


def _ga_in_lineage(versions, locked):
    """GA versions belonging to locked's release lineage, as (tuple, str) pairs.
    Filtering by lineage keeps '-boot2' out of a '-boot3' comparison."""
    lin = lineage_of(locked)
    out = []
    for v in versions:
        if not is_ga(v):
            continue
        if lineage_of(v) != lin:
            continue
        out.append((parse_version(v), v))
    out.sort()
    return out


def same_line_latest(versions, locked):
    """Highest GA sharing locked's major.minor — the 🟢 patch target
    (e.g. Spring Boot 3.5.13 -> 3.5.16). Falls back to locked if none."""
    lt = parse_version(locked)
    cands = [(t, v) for t, v in _ga_in_lineage(versions, locked)
             if t[0] == lt[0] and t[1] == lt[1]]
    return cands[-1][1] if cands else locked


def same_major_latest(versions, locked):
    """Highest GA sharing locked's major — the 🟡 minor target
    (e.g. Spring Boot 3.5.13 -> 3.5.16 today, 3.6.x when it ships)."""
    lt = parse_version(locked)
    cands = [(t, v) for t, v in _ga_in_lineage(versions, locked) if t[0] == lt[0]]
    return cands[-1][1] if cands else locked


def absolute_release(versions, locked, release_field):
    """Newest GA across all majors within locked's lineage — the 🔴 major target.
    We do NOT trust <release>: some projects set it to an RC (jetcache-bom), and
    for lineage artifacts it may point at a different line entirely."""
    cands = _ga_in_lineage(versions, locked)
    if cands:
        return cands[-1][1]
    # No same-lineage GA: fall back to newest GA anywhere, then to <release>.
    anyga = sorted((parse_version(v), v) for v in versions if is_ga(v))
    if anyga:
        return anyga[-1][1]
    return release_field


def classify(locked, release, sml, sml_major):
    """Return (tier, target).

    The tier is the *headline* risk — the biggest jump available. But every item
    now also carries patch/minor/major targets independently (see build_targets),
    so a 🔴 item like Spring Boot still surfaces its safe 3.5.16 patch. Previously
    the tier collapsed all three into one number and the patch was invisible.

    target = what the 'apply' button moves to; None for red/downgrade/uptodate.
    """
    if release is None:
        return 'unknown', None
    lt, rt = parse_version(locked), parse_version(release)
    if rt == lt:
        return 'uptodate', None
    if rt < lt:
        # locked newer than anything on Central: re-numbering or wrong coordinate.
        return 'downgrade', None
    if rt[0] != lt[0]:
        return 'red', None
    if rt[1] != lt[1]:
        target = sml_major if parse_version(sml_major) != lt else release
        return 'yellow', target
    return 'green', release


def build_targets(locked, sml, sml_major, release):
    """The three independent upgrade options for one dependency.

    Answers "show me patch, minor AND major" for every item, instead of only the
    single tier target. Each entry is None when it would be a no-op (equal to
    locked) or a duplicate of a lower-risk option.
    """
    lt = parse_version(locked)
    patch = sml if sml and parse_version(sml) > lt else None
    minor = sml_major if sml_major and parse_version(sml_major) > lt else None
    major = release if release and parse_version(release) > lt else None
    # de-dupe upward: a minor equal to the patch isn't a separate option
    if minor and patch and minor == patch:
        minor = None
    if major and ((patch and major == patch) or (minor and major == minor)):
        major = None
    return {
        'patch': patch,   # 🟢 same major.minor
        'minor': minor,   # 🟡 same major, newer minor
        'major': major,   # 🔴 newest GA, crosses major
    }


def parse_pom_properties(pom_path):
    """Return {property_name: locked_value} from the <properties> block, plus
    (revision, project_name). project_name is the POM <name>, else its artifactId,
    so the report header can show which project the dependency platform belongs to."""
    text = open(pom_path, 'r', encoding='utf-8').read()
    props = {}
    m = re.search(r'<properties>(.*?)</properties>', text, re.S)
    if m:
        for pm in re.finditer(r'<([a-z0-9-]+\.version)>\s*([^<]+?)\s*</\1>', m.group(1), re.I):
            props[pm.group(1)] = pm.group(2).strip()
    rev = re.search(r'<revision>\s*([^<]+?)\s*</revision>', text)
    # <name> often is a Maven placeholder (e.g. ${project.artifactId}); resolve
    # it to the real artifactId when it is, and always keep artifactId as fallback.
    artifact = re.search(r'<artifactId>\s*([^<]+?)\s*</artifactId>', text)
    artifact_val = artifact.group(1).strip() if artifact else ''
    name = re.search(r'<name>\s*([^<]+?)\s*</name>', text)
    name_val = name.group(1).strip() if name else ''
    if not name_val or '${' in name_val:
        name_val = artifact_val
    return props, (rev.group(1).strip() if rev else 'unknown'), name_val


def main(output_path='dep_scan.json'):
    t0 = time.time()
    props, revision, project_name = parse_pom_properties(POM_PATH)
    items, unreachable = [], []
    offline = False
    for prop, locked in props.items():
        meta = COORDINATES.get(prop)
        entry = {
            'property': prop, 'locked': locked,
            'family': meta['family'] if meta else '未分类',
            'group_id': meta['group_id'] if meta else '',
            'artifact_id': meta['artifact_id'] if meta else '',
            'constraints': (meta or {}).get('constraints', ''),
            'plugin': bool((meta or {}).get('plugin')),
            'release': None, 'same_line_latest': locked, 'tier': 'unknown',
            'target': None, 'gap': '', 'gap_versions': [],
            'unreachable': False, 'error': '',
        }
        if not meta:
            entry['error'] = '未在坐标目录登记（references/coordinates.md + scan.py COORDINATES 需补）'
            entry['tier'] = 'unknown'
            items.append(entry)
            continue
        entry['metadata_path'] = '{}/maven-metadata.xml'.format(
            meta['group_id'].replace('.', '/') + '/' + meta['artifact_id'])
        # Maven plugins ARE detected now. Their metadata lives at the normal
        # Central path once the groupId is right (the old catalog had
        # flatten under com.maven.plugins and sonar under org.jacoco — both
        # wrong, which is why they were skipped as "unreachable").
        try:
            versions, release_field, last_updated = fetch_metadata(meta['group_id'], meta['artifact_id'])
        except urllib.error.URLError as e:
            entry['unreachable'] = True
            entry['error'] = '抓取失败（已重试 {} 次）: {}'.format(MAX_RETRIES, type(e).__name__)
            entry['tier'] = 'unknown'
            unreachable.append(prop)
            items.append(entry)
            continue
        except Exception as e:
            entry['unreachable'] = True
            entry['error'] = '解析失败（已重试 {} 次）: {}'.format(MAX_RETRIES, e)
            entry['tier'] = 'unknown'
            unreachable.append(prop)
            items.append(entry)
            continue
        # Recompute release from GA-filtered versions — <release> field can be an RC.
        release = absolute_release(versions, locked, release_field)
        entry['release'] = release
        entry['release_field_raw'] = release_field
        entry['last_updated'] = last_updated
        entry['lineage'] = lineage_of(locked)
        sml = same_line_latest(versions, locked)
        sml_major = same_major_latest(versions, locked)
        entry['same_line_latest'] = sml
        entry['same_major_latest'] = sml_major
        tier, target = classify(locked, release, sml, sml_major)
        entry['tier'] = tier
        entry['target'] = target
        # All three upgrade options, independent of tier, so a 🔴 item still
        # shows its safe patch (Spring Boot 3.5.13 → 3.5.16 alongside → 4.1.0).
        targets = build_targets(locked, sml, sml_major, release)
        entry['targets'] = targets
        # Per-target changelog links so each option is verifiable upstream.
        entry['changelog'] = changelog_url(prop, target or release)
        entry['changelogs'] = {k: changelog_url(prop, v)
                               for k, v in targets.items() if v}
        # Maven Central overview: lists every published version, each with its
        # own release notes — the reliable "see all versions between two points"
        # view when the upstream keeps no changelog index.
        entry['central_url'] = ('https://central.sonatype.com/artifact/{}/{}/overview'
                                .format(meta['group_id'], meta['artifact_id']))
        # Compare links: full diff/commit list from locked -> each target version.
        # Lets the user read the complete change set across the upgrade, not just
        # the single target's release notes.
        entry['compare_urls'] = {}
        for k, v in targets.items():
            if not v:
                continue
            cu = compare_url(prop, locked, v)
            if cu:
                entry['compare_urls'][k] = cu
        # Pull the publish date of the LOCKED version (for "staleness") plus each
        # upgrade target (so every tab can show when its version shipped). A HEAD
        # on the .pom + Last-Modified is cheap and cached per coordinate.
        entry['locked_released'] = fetch_pom_last_modified(meta['group_id'], meta['artifact_id'], locked)
        entry['released'] = {
            k: fetch_pom_last_modified(meta['group_id'], meta['artifact_id'], v)
            for k, v in targets.items() if v
        }
        lt = parse_version(locked)
        if tier == 'downgrade':
            # locked is newer than Central release — versioning oddity, not an upgrade.
            entry['gap'] = '{}（锁定）比 {}（Central release）新，疑似项目重新编号或坐标指向不同分支'.format(
                locked, release)
        else:
            # Per-tier gap_versions so each tab shows the gap *for that tier's target*
            # only (e.g. a 🔴 Spring Boot card shows "落后 3 个版本" on the 🟢 patch tab
            # meaning 3.5.13 → 3.5.16, NOT the 12-version cross-major jump to 4.1.0).
            gap_by_tier = {}
            if tier == 'red':
                # headline gap = locked → release (the migration target).
                entry['gap'] = '{} → {}（跨大版本，需迁移）'.format(locked, release)
                gap_by_tier['major'] = [v for v in versions if is_ga(v)
                                        and parse_version(v) > lt
                                        and parse_version(v) <= parse_version(release)]
            else:
                entry['gap'] = '{} → {}'.format(locked, target) if target else ''
            # patch/minor gaps use their own (safer) targets, independent of tier.
            for k, v in targets.items():
                if not v:
                    continue
                tv = parse_version(v)
                gap_by_tier[k] = [x for x in versions if is_ga(x)
                                  and parse_version(x) > lt
                                  and parse_version(x) <= tv][:20]
            # Backwards-compatible single 'gap_versions' = the headline (tier) gap.
            entry['gap_versions'] = gap_by_tier.get(
                'major' if tier == 'red' else ('minor' if tier == 'yellow' else 'patch'),
                [])
            entry['gap_versions_by_tier'] = gap_by_tier
            entry['same_line_gap'] = '{} → {}'.format(locked, sml) if sml != locked else ''
        items.append(entry)

    # --- Spring Cloud <-> Spring Boot compatibility cross-check ---
    # Both coordinates are optional (a project may manage only one). When Boot has
    # an upgrade target, map that Boot version to its required Cloud Release Train
    # and surface it on the Boot card. When Cloud is also present, check whether its
    # current train matches the Boot target — mismatches get an explicit warning.
    boot_items = [i for i in items
                  if (i.get('property') == 'spring-boot.version'
                      or i.get('group_id') == 'org.springframework.boot')]
    cloud_items = [i for i in items
                   if (i.get('property') == 'spring-cloud.version'
                       or i.get('group_id') == 'org.springframework.cloud')]
    if boot_items:
        boot = boot_items[0]
        # Map every available Boot target (and the locked version) to a Cloud train.
        boot_targets = dict(boot.get('targets') or {})
        if boot.get('locked'):
            boot_targets['__locked__'] = boot['locked']
        train_lines = []
        for key, bv in boot_targets.items():
            if not bv:
                continue
            train = boot_to_cloud_train(bv)
            if not train:
                continue
            label = '锁定' if key == '__locked__' else '升级到 ' + bv
            train_lines.append('{} → Spring Cloud {}'.format(label, train))
        if train_lines:
            note = ('Spring Cloud 须与 Spring Boot 同代（官方矩阵 '
                    + SPRING_CLOUD_COMPAT_SRC + '）：' + '；'.join(train_lines) + '。')
            boot['constraints'] = (boot.get('constraints') + ' ' if boot.get('constraints') else '') + note
            boot['cloud_compat'] = {'src': SPRING_CLOUD_COMPAT_SRC,
                                    'trains': {k: boot_to_cloud_train(v)
                                               for k, v in boot_targets.items() if v}}
        if cloud_items:
            cloud = cloud_items[0]
            cur_train = cloud_train_of(cloud.get('locked'))
            # required trains from Boot targets (exclude the locked Boot train itself
            # since that's the "from" side, not the "to" side)
            req_trains = set()
            for key, bv in (boot.get('targets') or {}).items():
                if bv:
                    tr = boot_to_cloud_train(bv)
                    if tr:
                        req_trains.add(tr.split(' ')[0])  # normalized "2025.0.x"
            if cur_train and req_trains and cur_train not in req_trains:
                req_display = ' / '.join(sorted(req_trains))
                warn = ('当前 Spring Cloud {} 与升级目标 Spring Boot 所需的 Release Train（{}）不匹配，'
                        '须同步升级 Spring Cloud。官方矩阵见 {}'.format(
                            cur_train, req_display, SPRING_CLOUD_COMPAT_SRC))
                cloud['constraints'] = (cloud.get('constraints') + ' ' if cloud.get('constraints') else '') + warn
                cloud['tier'] = 'yellow' if cloud.get('tier') in ('green', 'uptodate', None, 'unknown') else cloud.get('tier')
                cloud['cloud_compat_warn'] = warn
                boot['constraints'] = (boot.get('constraints') + ' ' if boot.get('constraints') else '') + (
                    'Spring Cloud 当前 {} 不匹配，须随 Boot 一起升级到 {}。'.format(cur_train, req_display))

    # offline: only flag if a meaningful share of non-plugin fetches failed.
    # A single unreachable plugin/coordinate shouldn't mark the whole report offline.
    fetchable = [i for i in items if i['tier'] != 'plugin']
    failed = [i for i in fetchable if i.get('unreachable')]
    offline = len(failed) > 0 and len(fetchable) > 0 and len(failed) / len(fetchable) > 0.5

    # summary
    tier_counts = {'green': 0, 'yellow': 0, 'red': 0, 'downgrade': 0,
                   'uptodate': 0, 'unknown': 0, 'plugin': 0}
    for it in items:
        tier_counts[it['tier']] = tier_counts.get(it['tier'], 0) + 1
    outdated = tier_counts['green'] + tier_counts['yellow'] + tier_counts['red']

    # Availability counts: how many deps have a patch / minor / major upgrade on
    # offer. These cut across tiers (a 🔴 dep usually also has a patch), so they
    # deliberately do NOT sum to `outdated`. Drives the report's three tabs.
    avail = {'patch': 0, 'minor': 0, 'major': 0}
    for it in items:
        t = it.get('targets') or {}
        for k in avail:
            if t.get(k):
                avail[k] += 1
    plugin_items = [i for i in items if i.get('plugin')]
    plugin_outdated = sum(1 for i in plugin_items
                          if i['tier'] in ('green', 'yellow', 'red'))

    # top5: outdated only, rank by gap span. Red sorts by locked→release distance
    # (target is None for red); green/yellow by locked→target distance. Red outranks
    # green/yellow of equal span because cross-major is higher-stakes.
    def gap_span(it):
        lt = parse_version(it['locked'])
        if it['tier'] == 'red':
            rt = parse_version(it['release'])
            return (1, rt[0] - lt[0], rt[1] - lt[1], rt[2] - lt[2])
        if it['tier'] in ('green', 'yellow') and it.get('target'):
            tt = parse_version(it['target'])
            return (0, 0, abs(tt[1] - lt[1]), abs(tt[2] - lt[2]))
        return (-1, 0, 0, 0)
    ranked = sorted([i for i in items if i['tier'] in ('green', 'yellow', 'red')],
                    key=gap_span, reverse=True)
    top5 = [{'rank': r + 1, 'property': i['property'], 'artifact': i['artifact_id'],
             'tier': i['tier'], 'locked': i['locked'],
             'target': i['target'] or i.get('release'),
             'gap': i['gap'], 'type': i['family'],
             'note': i['constraints']} for r, i in enumerate(ranked[:5])]

    result = {
        'generated_at': time.strftime('%Y-%m-%d %H:%M:%S', time.localtime()),
        'scan_seconds': round(time.time() - t0, 1),
        'offline': offline,
        'repo': {'pom_path': POM_PATH.replace('\\', '/').split('/continew-starter/')[-1]
                 if 'continew-starter' in POM_PATH else POM_PATH,
                 'revision': revision,
                 'project_name': project_name},
        'summary': {
            'total': len(items), 'outdated': outdated,
            'green': tier_counts['green'], 'yellow': tier_counts['yellow'],
            'red': tier_counts['red'], 'downgrade': tier_counts['downgrade'],
            'up_to_date': tier_counts['uptodate'],
            'unreachable': tier_counts['unknown'], 'plugin': len(plugin_items),
            'plugin_outdated': plugin_outdated,
            'available': avail,
            'tier_counts': tier_counts,
        },
        'top5': top5,
        'items': items,
        'unreachable': unreachable,
    }
    json.dump(result, sys.stdout, ensure_ascii=False, indent=2)
    print()  # trailing newline
    # Persist a UTF-8 (no BOM) copy to disk so callers can feed it straight to
    # server.py / build_report.py. Do NOT rely on shell `>` redirection for this:
    # PowerShell writes UTF-16 (or the system ANSI codec) by default, which would
    # corrupt the JSON — see the read helpers in server.py / build_report.py.
    try:
        with open(output_path, 'w', encoding='utf-8', newline='\n') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        sys.stderr.write('扫描结果已写入: {}\n'.format(output_path))
    except Exception as e:
        sys.stderr.write('写入 {} 失败: {}\n'.format(output_path, e))


if __name__ == '__main__':
    # Force UTF-8 on stdout — Windows default (cp936/GBK) would corrupt the JSON
    # and break build_report.py reading it back. ensure_ascii=False in json.dump
    # needs a UTF-8 sink to be safe.
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass
    import argparse
    ap = argparse.ArgumentParser(
        description='扫描 continew-starter 依赖并输出升级分析 JSON')
    ap.add_argument('-o', '--output', default='dep_scan.json',
                    help='输出 JSON 文件路径（UTF-8，默认 dep_scan.json）。'
                         '请用本参数落盘，勿用 shell 的 > 重定向（PowerShell 会写成 UTF-16）。')
    args = ap.parse_args()
    try:
        main(args.output)
    except KeyboardInterrupt:
        sys.exit(130)
