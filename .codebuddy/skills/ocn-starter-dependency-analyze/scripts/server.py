#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Interactive server for the ocn-starter-dependency-upgrade report.

Serves the HTML report on 127.0.0.1:<random port> with a random token, and
exposes a POST /apply endpoint that edits POM <version> properties DIRECTLY on
the current git branch. Accepts either a single {property,target} or a batch
{items:[{property,target},...]} for one-click patch/minor upgrades.

By default NO maven build is run: the edit is a mechanical single-line
substitution and compiling after every click was too slow, so the user compiles
once at the end. Pass {"compile":true} to re-enable the old compile gate, which
runs `mvn -pl :continew-starter-dependencies -am compile` and reverts the POM in
place on non-zero exit. Never creates a branch, commits, pushes, or runs
install/verify/test.

Security model (mirrors storage-analyzer/server.py, ported to source writes):
  - binds 127.0.0.1 only
  - random port + random token in URL path
  - Host header must be 127.0.0.1 / localhost (blocks DNS-rebinding)
  - every apply confirms in-browser (client-side) before the POST
  - only the property names registered in scan.py COORDINATES are editable
  - POM edit is a fully-anchored regex on the exact <prop.version>X</prop.version>
    line, so it can't accidentally rewrite a different property

Note: there is NO clean-working-tree precondition and, by default, no build
signal. Undoing an apply is an ordinary `git checkout` of the one edited POM.

See docs/adr/0002-write-mode-git-scoped-and-compile-gated.md and
docs/adr/0003-batch-apply-and-compile-opt-in.md for the rationale.

Usage:
  python server.py <scan.json> [--open]
"""
import argparse
import http.server
import json
import os
import re
import secrets
import shutil
import socket
import subprocess
import sys
import threading
import urllib.parse
import webbrowser

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(HERE, '..', '..', '..', '..'))
POM_PATH = os.path.join(REPO_ROOT, 'continew-starter-dependencies', 'pom.xml')
TEMPLATE_PATH = os.path.join(HERE, '..', 'assets', 'report_template.html')

IS_WIN = sys.platform == 'win32'
# On Windows, git/mvn are .exe / .cmd. shutil.which resolves them across PATH.
# shell=True is needed for .cmd/.bat (mvn.cmd) — without it WinError 2.
GIT = shutil.which('git') or 'git'
MVN = shutil.which('mvn') or 'mvn'
_SHELL = IS_WIN  # only shell=True on Windows (for .cmd resolution); off elsewhere for safety

# Reuse the coordinate registry so the server only edits known properties.
sys.path.insert(0, HERE)
import scan  # noqa: E402

# Whitelist of editable property names. Build plugins are included now that they
# are scanned like any other coordinate — their version properties live in the
# same <properties> block and are edited identically. They were previously
# excluded only because their versions could not be detected.
EDITABLE_PROPS = set(scan.COORDINATES)


def git(args, **kw):
    """Run a git command in REPO_ROOT, return CompletedProcess."""
    return subprocess.run([GIT] + args, cwd=REPO_ROOT, capture_output=True,
                          text=True, shell=_SHELL, **kw)


def edit_pom_property(prop, new_value):
    """Anchored regex replace of <prop>VALUE</prop> in the POM. Returns True if
    exactly one line was changed. Refuses if 0 or >1 matches (ambiguous)."""
    with open(POM_PATH, 'r', encoding='utf-8') as f:
        text = f.read()
    pat = re.compile(r'(<{}>)[^<]*(</{}>)'.format(re.escape(prop), re.escape(prop)))
    new_text, n = pat.subn(r'\g<1>{}\g<2>'.format(new_value), text)
    if n != 1:
        return False, n
    with open(POM_PATH, 'w', encoding='utf-8') as f:
        f.write(new_text)
    return True, n


def current_property_value(text, prop):
    """Return the current <prop>VALUE</prop> value from POM text, or ''."""
    m = re.search(r'<{}>([^<]*)</{}>'.format(re.escape(prop), re.escape(prop)), text)
    return m.group(1) if m else ''


def run_compile():
    """Run the compile gate. Returns (ok, message)."""
    try:
        comp = subprocess.run(
            [MVN, '-pl', ':continew-starter-dependencies', '-am', 'compile', '-q'],
            cwd=REPO_ROOT, capture_output=True, text=True, shell=_SHELL, timeout=600)
        if comp.returncode != 0:
            tail = (comp.stderr or comp.stdout or '').strip().splitlines()[-3:]
            return False, ' | '.join(tail)
        return True, ''
    except subprocess.TimeoutExpired:
        return False, 'mvn compile 超时'
    except Exception as e:
        return False, '异常: {}'.format(e)


def validate_one(prop, target):
    """Shared precondition check for a single (property, target) pair.
    Returns an error string, or '' when the pair is safe to write."""
    if prop not in EDITABLE_PROPS:
        return 'property 不在白名单（{} 未登记为可编辑坐标）'.format(prop)
    if not re.match(r'^[A-Za-z0-9._+-]+$', target):
        return '目标版本含非法字符: {}'.format(target)
    return ''


def do_apply(prop, target, compile_gate=False):
    """The gated write. Edits the POM on the CURRENT branch only — no new branch
    is created. Never commits, pushes, or runs install/verify/test.

    compile_gate defaults to False: the POM edit is a mechanical, fully-anchored
    single-line substitution, and running `mvn compile` after every click made
    each upgrade cost tens of seconds. The user compiles once at the end instead.
    When compile_gate is True the old behaviour applies — on compile failure the
    POM is restored to its original content in place (so the user's other
    uncommitted work is untouched).

    Returns dict {ok, error?, rolled_back?, compile_ok?, prop, target, old, name}."""
    err = validate_one(prop, target)
    if err:
        return {'ok': False, 'error': err}
    if not os.path.isfile(POM_PATH):
        return {'ok': False, 'error': '找不到 POM: {}'.format(POM_PATH)}

    # snapshot original POM content for rollback (independent of git, belt+suspenders)
    with open(POM_PATH, 'r', encoding='utf-8') as f:
        original = f.read()
    old_value = current_property_value(original, prop)

    # Edit directly on the current branch — no branch is created or switched.
    ok, n = edit_pom_property(prop, target)
    if not ok:
        return {'ok': False, 'error': 'POM 中 {} 匹配 {} 处（应为 1），未改动'.format(prop, n)}
    if not compile_gate:
        return {'ok': True, 'compile_ok': None, 'prop': prop,
                'target': target, 'old': old_value, 'name': scan.COORDINATES.get(prop, {}).get('artifact_id', prop)}
    cok, msg = run_compile()
    if not cok:
        with open(POM_PATH, 'w', encoding='utf-8') as f:
            f.write(original)
        return {'ok': False, 'rolled_back': True,
                'error': 'mvn compile 失败（已把 POM 改回原值）: ' + msg}
    return {'ok': True, 'compile_ok': True, 'prop': prop,
            'target': target, 'old': old_value, 'name': scan.COORDINATES.get(prop, {}).get('artifact_id', prop)}


def do_apply_batch(pairs, compile_gate=False):
    """Apply a list of [{property, target}] in one shot.

    Each entry is validated up front, so a single bad property rejects the whole
    batch before anything is written. Edits are then applied sequentially; if one
    substitution fails mid-way the already-written ones are kept (they are
    independent single-line edits) and reported per-item, because reverting the
    whole POM would also discard the user's own concurrent edits.

    Returns {ok, results:[{property,target,ok,old,name,error?}], applied, failed,
             compile_ok?, compile_error?}."""
    if not isinstance(pairs, list) or not pairs:
        return {'ok': False, 'error': '批量列表为空'}
    if not os.path.isfile(POM_PATH):
        return {'ok': False, 'error': '找不到 POM: {}'.format(POM_PATH)}
    with open(POM_PATH, 'r', encoding='utf-8') as f:
        original_text = f.read()

    # validate everything before writing anything
    for p in pairs:
        prop, target = p.get('property'), p.get('target')
        if not prop or not target:
            return {'ok': False, 'error': '批量项缺少 property 或 target'}
        err = validate_one(prop, target)
        if err:
            return {'ok': False, 'error': '批量被拒（{}）：{}'.format(prop, err)}

    results, applied = [], 0
    for p in pairs:
        prop, target = p['property'], p['target']
        ok, n = edit_pom_property(prop, target)
        old_value = current_property_value(original_text, prop)
        if ok:
            applied += 1
            results.append({'property': prop, 'target': target, 'ok': True,
                            'old': old_value,
                            'name': scan.COORDINATES.get(prop, {}).get('artifact_id', prop)})
        else:
            results.append({'property': prop, 'target': target, 'ok': False,
                            'old': old_value,
                            'name': scan.COORDINATES.get(prop, {}).get('artifact_id', prop),
                            'error': 'POM 中匹配 {} 处（应为 1），该项未改动'.format(n)})
    out = {'ok': applied > 0, 'results': results, 'applied': applied,
           'failed': len(pairs) - applied}
    if applied and compile_gate:
        cok, msg = run_compile()
        out['compile_ok'] = cok
        if not cok:
            out['compile_error'] = msg
    return out


def build_html(scan_path, token):
    # build_report.py lives in the same scripts/ dir (HERE is on sys.path).
    # load_json tolerates UTF-8/UTF-16, so a PowerShell `>` redirected scan.json
    # (UTF-16) still loads — see the helper in build_report.py.
    import build_report
    scan_data = build_report.load_json(scan_path)
    analysis = build_report.build_analysis(scan_data)
    payload = {
        'generated_at': scan_data.get('generated_at'),
        'scan_seconds': scan_data.get('scan_seconds'),
        'offline': scan_data.get('offline', False),
        'revision': scan_data.get('repo', {}).get('revision'),
        'summary': scan_data['summary'],
        'top5': scan_data['top5'],
        'analysis': analysis,
        'server_mode': True,
    }
    with open(TEMPLATE_PATH, 'r', encoding='utf-8') as f:
        tpl = f.read()
    html = tpl.replace('__REPORT_DATA__', json.dumps(payload, ensure_ascii=False))
    # server mode: inject apply config. endpoint path includes the token.
    apply_cfg = {'token': token, 'endpoint': '/apply/' + token}
    html = html.replace('__APPLY_CONFIG__', json.dumps(apply_cfg, ensure_ascii=False))
    return html.encode('utf-8')


class Handler(http.server.BaseHTTPRequestHandler):
    token = None
    html_bytes = None

    def _host_ok(self):
        h = self.headers.get('Host', '').split(':')[0]
        return h in ('127.0.0.1', 'localhost', '[::1]', '::1')

    def log_message(self, *a):
        pass  # quiet

    def do_GET(self):
        if not self._host_ok():
            self.send_error(403); return
        # serve report at /<token>, anything else 404
        path = urllib.parse.urlparse(self.path).path
        if path == '/' + self.token or path == '/':
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.send_header('Content-Length', str(len(self.html_bytes)))
            self.end_headers()
            self.wfile.write(self.html_bytes)
        else:
            self.send_error(404)

    def do_POST(self):
        if not self._host_ok():
            self.send_error(403); return
        path = urllib.parse.urlparse(self.path).path
        expected = '/apply/' + self.token
        if path != expected:
            self.send_error(403); return
        length = int(self.headers.get('Content-Length', 0))
        try:
            body = json.loads(self.rfile.read(length).decode('utf-8'))
        except Exception:
            self._json({'ok': False, 'error': '请求体不是合法 JSON'}); return
        if body.get('token') != self.token:
            self._json({'ok': False, 'error': 'token 不匹配'}); return
        # compile gate is opt-in; the UI does not request it by default.
        compile_gate = bool(body.get('compile'))
        items = body.get('items')
        if items is not None:
            self._json(do_apply_batch(items, compile_gate=compile_gate)); return
        prop = body.get('property')
        target = body.get('target')
        if not prop or not target:
            self._json({'ok': False, 'error': '缺少 property 或 target'}); return
        self._json(do_apply(prop, target, compile_gate=compile_gate))

    def _json(self, obj):
        data = json.dumps(obj, ensure_ascii=False).encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def find_free_port():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('127.0.0.1', 0))
    port = s.getsockname()[1]
    s.close()
    return port


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('scan_json')
    ap.add_argument('--open', action='store_true', help='open browser automatically')
    args = ap.parse_args()

    token = secrets.token_hex(8)
    Handler.token = token
    Handler.html_bytes = build_html(args.scan_json, token)

    port = find_free_port()
    url = 'http://127.0.0.1:{}/{}'.format(port, token)
    httpd = http.server.HTTPServer(('127.0.0.1', port), Handler)
    sys.stderr.write('依赖升级分析服务已启动: {}\n'.format(url))
    sys.stderr.write('Ctrl+C 停止。应用升级只改 POM，不跑 mvn；改完请自行 mvn compile 验证。\n')
    if args.open or '--no-open' not in sys.argv:
        try:
            webbrowser.open(url)
        except Exception:
            pass
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        sys.stderr.write('\n已停止。\n')
        httpd.server_close()


if __name__ == '__main__':
    main()
