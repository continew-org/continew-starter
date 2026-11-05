#!/usr/bin/env python3
"""Check every auto-configuration module declares a matching contract test."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IMPORTS_GLOB = "src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
TEST_GLOB = "src/test/java/**/*AutoConfigurationContractTest.java"


def collect(glob: str) -> set[str]:
    modules: set[str] = set()
    for path in ROOT.rglob(glob):
        parts = path.relative_to(ROOT).parts
        modules.add("/".join(parts[: parts.index("src")]))
    return modules


def main() -> int:
    imports_modules = collect(IMPORTS_GLOB)
    test_modules = collect(TEST_GLOB)

    if not imports_modules:
        print("No AutoConfiguration.imports found. Run from the repository root.", file=sys.stderr)
        return 1

    missing = sorted(imports_modules - test_modules)
    extra = sorted(test_modules - imports_modules)

    errors: list[str] = []
    if missing:
        errors.append("Modules declaring auto-configuration without a contract test:\n  " + "\n  ".join(missing))
    if extra:
        errors.append("Contract tests without a matching auto-configuration module:\n  " + "\n  ".join(extra))

    if errors:
        print("Contract test drift detected:\n" + "\n\n".join(errors), file=sys.stderr)
        return 1

    print(
        f"Contract tests are in sync: {len(imports_modules)} auto-configuration modules, "
        f"{len(test_modules)} contract test modules."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
