#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Parse a user-supplied "upgraded libraries" text list into version triples.

Usage:
    python parse_list.py "spring-cloud 2025.0.1 => 2025.0.3, hutool 5.8.44 => 5.8.47"
    python parse_list.py -o out.json "spring-boot 3.5.13 -> 3.5.16"

Accepts free-form text where each item is "<lib> <old> <sep> <new>". Supported
separators: '=>', '->', '→', 'to', '=>'. Library name is matched against the
upgrade skill's coordinates.md property names / artifact aliases (best effort)
so we can recover group:artifact for changelog fetching.

Emits JSON array of {"property", "group_artifact", "old", "new"}.
"""
import json
import os
import re
import sys

# Best-effort alias -> property name (mirrors upgrade coordinates.md keys).
ALIASES = {
    'spring-boot': 'spring-boot.version', 'spring cloud': 'spring-cloud.version',
    'spring-cloud': 'spring-cloud.version', 'mybatis-plus': 'mybatis-plus.version',
    'mybatis-flex': 'mybatis-flex.version', 'dynamic-datasource': 'dynamic-datasource.version',
    'p6spy': 'p6spy.version', 'sa-token': 'sa-token.version', 'justauth': 'justauth.version',
    'redisson': 'redisson.version', 'jetcache': 'jetcache.version',
    'fastexcel': 'fastexcel.version', 'poi': 'poi.version', 'nextdoc4j': 'nextdoc4j.version',
    'x-file-storage': 'x-file-storage.version', 'aws-sdk-v1': 'aws-sdk-v1.version',
    'aws-sdk': 'aws-sdk.version', 'aws-crt': 'aws-crt.version', 'thumbnails': 'thumbnails.version',
    'cosid': 'cosid.version', 'snail-job': 'snail-job.version', 'sms4j': 'sms4j.version',
    'paho-mqttv3': 'paho-mqttv3.version', 'aj-captcha': 'aj-captcha.version',
    'easy-captcha': 'easy-captcha.version', 'graceful-response': 'graceful-response.version',
    'spel-validator': 'spel-validator.version', 'crane4j': 'crane4j.version',
    'swagger': 'swagger.version', 'tlog': 'tlog.version', 'hutool': 'hutool.version',
    'snakeyaml': 'snakeyaml.version', 'nashorn': 'nashorn.version',
    'commons-fileupload': 'commons-fileupload.version', 'commons-beanutils': 'commons-beanutils.version',
    'commons-io': 'commons-io.version', 'commons-compress': 'commons-compress.version',
    'truelicense': 'truelicense.version', 'zip4j': 'zip4j.version', 'ttl': 'ttl.version',
    'ip2region': 'ip2region.version', 'flatten': 'flatten.version', 'spotless': 'spotless.version',
    'sonar': 'sonar.version',
}

SEP_RE = re.compile(r'\s*(?:=>|->|→|to)\s*', re.IGNORECASE)
ITEM_SPLIT_RE = re.compile(r'[,;\n]+(?=(?:[a-z0-9\-]+\s+[\d.]+))', re.IGNORECASE)


def resolve_property(lib):
    lib = lib.strip().lower()
    if lib in ALIASES:
        return ALIASES[lib]
    # allow direct property name like "spring-cloud.version"
    if lib.endswith('.version') and lib in ALIASES.values():
        return lib
    return None


def parse(text):
    items = []
    # Split into candidate items, guarding against splitting inside versions.
    chunks = ITEM_SPLIT_RE.split(text)
    if len(chunks) <= 1:
        chunks = re.split(r'[,;\n]+', text)
    for chunk in chunks:
        chunk = chunk.strip()
        if not chunk:
            continue
        m = SEP_RE.search(chunk)
        if not m:
            continue
        left, right = chunk[:m.start()], chunk[m.end():]
        # left = "<lib> <old>", right = "<new>"
        lm = re.match(r'^(.*?)\s+([\d][\w.\-]*)\s*$', left.strip())
        if not lm:
            continue
        lib, old = lm.group(1), lm.group(2)
        new = right.strip().strip('.').strip()
        if not re.match(r'^[\d]', new):
            continue
        prop = resolve_property(lib)
        items.append({'property': prop, 'group_artifact': None,
                      'old': old, 'new': new})
    return items


def main():
    out_path = None
    args = sys.argv[1:]
    text_parts = []
    i = 0
    while i < len(args):
        a = args[i]
        if a == '-o' and i + 1 < len(args):
            out_path = args[i + 1]; i += 2; continue
        text_parts.append(a); i += 1
    text = ' '.join(text_parts)
    items = parse(text)
    if out_path:
        with open(out_path, 'w', encoding='utf-8') as f:
            json.dump(items, f, ensure_ascii=False, indent=2)
    else:
        print(json.dumps(items, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
