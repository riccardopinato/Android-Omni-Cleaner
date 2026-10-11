#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app" / "src" / "main" / "res"
FILES = {
    "en": RES / "values" / "strings.xml",
    "it": RES / "values-it" / "strings.xml",
    "es": RES / "values-es" / "strings.xml",
    "fr": RES / "values-fr" / "strings.xml",
    "pt": RES / "values-pt" / "strings.xml",
}
PLACEHOLDER = re.compile(r"%\d+\$[sd]")


def load(path: Path) -> tuple[list[str], dict[str, str]]:
    root = ET.parse(path).getroot()
    names: list[str] = []
    values: dict[str, str] = {}
    for node in root.findall("string"):
        name = node.attrib["name"]
        names.append(name)
        values[name] = "".join(node.itertext())
    return names, values


def placeholders(value: str) -> tuple[str, ...]:
    return tuple(sorted(PLACEHOLDER.findall(value)))


def main() -> int:
    parsed = {lang: load(path) for lang, path in FILES.items()}
    base_names, base_values = parsed["en"]
    base_set = set(base_names)
    failures: list[str] = []

    for lang, (names, values) in parsed.items():
        counts = Counter(names)
        duplicates = sorted(name for name, count in counts.items() if count > 1)
        missing = sorted(base_set - set(names))
        extra = sorted(set(names) - base_set)

        if duplicates:
            failures.append(f"{lang}: duplicate keys: {duplicates}")
        if missing:
            failures.append(f"{lang}: missing keys: {missing}")
        if extra:
            failures.append(f"{lang}: extra keys: {extra}")

        for key in base_names:
            if key not in values:
                continue
            expected = placeholders(base_values[key])
            actual = placeholders(values[key])
            if actual != expected:
                failures.append(
                    f"{lang}: placeholder mismatch for {key}: "
                    f"expected={expected} actual={actual}"
                )

        print(
            f"[locales] {lang}: {len(names)} strings, "
            f"duplicates={len(duplicates)}, missing={len(missing)}, extra={len(extra)}"
        )

    if failures:
        print("\n".join(f"[locales] ERROR: {failure}" for failure in failures), file=sys.stderr)
        return 1

    print(
        f"[locales] PASS: {len(base_names)} keys with exact "
        "en/it/es/fr/pt key and placeholder parity"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
