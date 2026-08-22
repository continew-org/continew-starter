#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Extract version-property changes from git diff of the dependencies POM.

Usage:
    python diff_versions.py [-p POM_PATH] [-o out.json]

Parses `git diff` of continew-starter-dependencies/pom.xml and pulls only the
`<xxx.version>OLD</xxx.version>` -> `<xxx.version>NEW</xxx.version>` property
edits (the lines Maven Central upgrade touches). Emits a JSON array of
{"property", "group_artifact", "old", "new"} so the verify skill knows exactly
which libraries moved and can target changelog fetches + compile-error grouping.

If git is unavailable or the diff is empty, prints [] and exits 0 (caller should
fall back to parse_list.py with a user-supplied text list).

Requires the companion coordinates catalog to map property -> group:artifact;
we reuse upgrade skill's coordinates.md by best-effort regex, but primarily we
emit property + old + new, letting the skill read coordinates.md for the rest.
"""
import json
import os
import re
import subprocess
import sys

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))
DEFAULT_POM = os.path.join(REPO_ROOT, 'continew-starter-dependencies', 'pom.xml')

# A version property line looks like:
#   <spring-boot.version>3.5.13</spring-boot.version>
PROP_RE = re.compile(r'^\s*<([\w.\-]+)>\s*([^<]+?)\s*</\1>\s*$')

# A coordinate line inside a <dependency> block looks like:
#   <artifactId>nextdoc4j-springboot3-starter</artifactId>
#   <groupId>top.nextdoc4j</groupId>
# We track these to DETECT artifactId / groupId RENAMES, which the version
# property diff completely misses (the canonical nextdoc4j 1.2.0 -> 1.4.1 bump
# silently renamed both the BOM and the starter artifact).
COORD_RE = re.compile(r'^\s*<(groupId|artifactId)>\s*([^<]+?)\s*</\1>\s*$')

# Dependency blocks often carry a comment naming the logical library, e.g.
#   <!-- NextDoc4J -->
# Capture the most recent comment before a coordinate change so we can label
# which library the rename belongs to.
COMMENT_RE = re.compile(r'^\s*<!--\s*(.+?)\s*-->\s*$')


def git_diff(pom_path):
    try:
        out = subprocess.run(
            ['git', 'diff', '--', pom_path],
            cwd=REPO_ROOT, capture_output=True,
            encoding='utf-8', errors='replace', timeout=30)
        if out.returncode != 0:
            return ''
        return out.stdout
    except Exception:
        return ''


def parse_coord_changes(diff):
    """Detect groupId/artifactId RENAMES inside <dependency> blocks.

    Returns a list of {"library", "kind", "old", "new"} where kind is
    "groupId" or "artifactId". This is the ONLY way to catch upgrades that
    silently rename coordinates (nextdoc4j 1.2.0 -> 1.4.1 renamed both its BOM
    and starter artifact); a bare version-property diff is blind to it.
    """
    renames = []
    last_comment = None
    for line in diff.splitlines():
        if not (line.startswith('-') or line.startswith('+')):
            cm = COMMENT_RE.match(line)
            if cm:
                last_comment = cm.group(1).strip()
            continue
        body = line[1:]
        m = COORD_RE.match(body)
        if not m:
            continue
        kind, val = m.group(1), m.group(2).strip()
        # Pair up the '-' and '+' lines of the SAME coordinate kind. Because git
        # emits - then + for a changed line, we buffer the '-' side and flush on '+'.
        if line.startswith('-'):
            renames.append({'library': last_comment, 'kind': kind,
                            'old': val, 'new': None, '_pending': True})
        else:
            # attach to the most recent pending entry of the same kind
            for r in reversed(renames):
                if r.get('_pending') and r['kind'] == kind:
                    r['new'] = val
                    r.pop('_pending', None)
                    break
    # Drop unpaired (pure add/remove) and collapse to clean records.
    clean = [{'library': r['library'], 'kind': r['kind'],
              'old': r['old'], 'new': r['new']}
             for r in renames if r.get('new') and r['old'] != r['new']]
    return clean


def parse_version_changes(diff):
    """Original behavior: pull <x.version>OLD -> NEW edits only."""
    changed = {}  # property -> {'old':.., 'new':..}
    for line in diff.splitlines():
        if not (line.startswith('-') or line.startswith('+')):
            continue
        body = line[1:]
        m = PROP_RE.match(body)
        if not m:
            continue
        prop, ver = m.group(1), m.group(2).strip()
        # Only care about *version properties* (ending .version) — those are the
        # ones the upgrade skill edits. Other property edits are out of scope.
        if not prop.endswith('.version'):
            continue
        if line.startswith('-'):
            entry = changed.setdefault(prop, {})
            entry['old'] = ver
        else:
            entry = changed.setdefault(prop, {})
            entry['new'] = ver

    items = []
    for prop, vals in changed.items():
        old = vals.get('old')
        new = vals.get('new')
        # Skip pure additions/removals (only one side present) — not an upgrade.
        if not old or not new:
            continue
        if old == new:
            continue
        items.append({'property': prop, 'group_artifact': None,
                      'old': old, 'new': new})
    return items


def main():
    pom = DEFAULT_POM
    out_path = None
    args = sys.argv[1:]
    i = 0
    while i < len(args):
        a = args[i]
        if a == '-p' and i + 1 < len(args):
            pom = args[i + 1]; i += 2; continue
        if a == '-o' and i + 1 < len(args):
            out_path = args[i + 1]; i += 2; continue
        i += 1

    diff = git_diff(pom)

    version_items = parse_version_changes(diff)
    version_items.sort(key=lambda x: x['property'])

    coord_renames = parse_coord_changes(diff)

    result = {
        'version_bumps': version_items,
        'coordinate_renames': coord_renames,
    }

    if out_path:
        with open(out_path, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
    else:
        print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
