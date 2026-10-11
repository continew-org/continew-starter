#!/usr/bin/env python3
"""Check docs/reference/properties.mdx against generated Spring configuration metadata."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOC = ROOT / "docs" / "reference" / "properties.mdx"
METADATA_GLOB = "target/classes/META-INF/spring-configuration-metadata.json"

# These classes are upstream configuration objects. The reference page deliberately links their
# official configuration model instead of copying every nested option into a second hand-written table.
UPSTREAM_SOURCE_TYPES = {
    "org.redisson.config.SingleServerConfig",
    "org.redisson.config.ClusterServersConfig",
    "org.redisson.config.SentinelServersConfig",
    "top.continew.starter.web.autoconfigure.response.GlobalResponseProperties",
}

# Conditions read properties directly and therefore do not appear in generated metadata.
CONDITIONAL_PROPERTIES = {
    "continew-starter.idempotent.enabled",
    "spring.task.execution.extension.enabled",
    "spring.task.scheduling.extension.enabled",
}

# ContiNew Admin extends the tenant namespace; these are documented as consumer-owned examples.
CONSUMER_EXTENSION_PROPERTIES = {
    "continew-starter.tenant.tenant-code-header",
    "continew-starter.tenant.default-tenant-id",
    "continew-starter.tenant.ignore-menus",
}

# Spring Boot's configuration processor omits raw java.util.Properties fields, although the
# binder accepts them and they are declared by MqttProperties.
PROCESSOR_OMITTED_PROPERTIES = {
    "continew-starter.messaging.mqtt.ssl-client-props",
    "continew-starter.messaging.mqtt.custom-web-socket-headers",
}

PROPERTY_RE = re.compile(
    r"`((?:continew-starter|application|spring|server|sa-token|mybatis-plus|mybatis-flex)"
    r"[a-zA-Z0-9_.-]+)`"
)


def load_metadata() -> dict[str, dict]:
    properties: dict[str, dict] = {}
    for path in ROOT.rglob(METADATA_GLOB):
        data = json.loads(path.read_text(encoding="utf-8"))
        for item in data.get("properties", []):
            name = item.get("name")
            if name:
                properties[name] = item
    if not properties:
        raise RuntimeError(
            "No generated spring-configuration-metadata.json found. Run ./mvnw compile first."
        )
    return properties


def is_group_key(name: str, metadata_names: set[str]) -> bool:
    return any(candidate.startswith(name + ".") for candidate in metadata_names)


def is_documented(name: str, text: str) -> bool:
    if f"`{name}`" in text:
        return True

    # MQTT producer/consumer/executor tables use a section prefix followed by short property names.
    mqtt_prefixes = (
        "continew-starter.messaging.mqtt.producer.",
        "continew-starter.messaging.mqtt.consumer.",
    )
    for prefix in mqtt_prefixes:
        if name.startswith(prefix):
            short_name = name.removeprefix(prefix)
            if f"`{short_name}`" in text:
                return True
            # producer.executor.* and consumer.executor.* share one table headed by both prefixes.
            if short_name.startswith("executor."):
                return (
                    "`producer.executor.*`" in text
                    and "`consumer.executor.*`" in text
                    and f"`{short_name.removeprefix('executor.')}`" in text
                )
            return False
    return False


def main() -> int:
    metadata = load_metadata()
    metadata_names = set(metadata)
    text = DOC.read_text(encoding="utf-8")
    documented = set(PROPERTY_RE.findall(text))

    unknown = sorted(
        name
        for name in documented
        if name != "spring-configuration-metadata.json"
        and name not in metadata_names
        and name not in CONDITIONAL_PROPERTIES
        and name not in CONSUMER_EXTENSION_PROPERTIES
        and name not in PROCESSOR_OMITTED_PROPERTIES
        and not is_group_key(name, metadata_names)
    )

    missing = sorted(
        name
        for name, item in metadata.items()
        if item.get("sourceType") not in UPSTREAM_SOURCE_TYPES
        and not is_documented(name, text)
    )

    errors: list[str] = []
    if unknown:
        errors.append("Documented keys absent from metadata/allowlists:\n  " + "\n  ".join(unknown))
    if missing:
        errors.append("Generated Starter keys missing from the reference page:\n  " + "\n  ".join(missing))

    if errors:
        print("Configuration reference drift detected:\n" + "\n\n".join(errors), file=sys.stderr)
        return 1

    print(
        f"Configuration reference is in sync: {len(metadata_names)} generated keys, "
        f"{len(documented)} documented full keys."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
