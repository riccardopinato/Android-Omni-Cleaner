#!/usr/bin/env python3
"""Convert actual OMNI_PERF_V1 debug reports to Factory benchmark evidence JSONL.

Requires independently documented fixture ID and integrity digest; NEVER
derives them from a file count or fills physical measurements with guesses.
The JSONL comparator lives in pinned App-Factory-Core, not this app.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


def parse(report: str) -> dict[str, str]:
    lines = [line.strip() for line in report.splitlines() if line.strip()]
    if not lines or lines[0] != "OMNI_PERF_V1":
        raise ValueError("not an OMNI_PERF_V1 diagnostics report")
    fields: dict[str, str] = {}
    for line in lines[1:]:
        if "=" not in line:
            raise ValueError("unrecognized report line")
        key, val = line.split("=", 1)
        if key in fields or not re.fullmatch(r"[a-z0-9_]+", key):
            raise ValueError("duplicate or unsafe field name")
        fields[key] = val
    return fields


def row(report: str, sha: str, fixture: str, integrity_sha: str) -> dict:
    fields = parse(report)
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("full, lowercase 40-hex Git commit SHA required")
    if not re.fullmatch(r"[0-9a-f]{64}", integrity_sha):
        raise ValueError("independent full 64-hex integrity SHA-256 required")
    if not re.fullmatch(r"[A-Za-z0-9_-]{6,128}", fixture):
        raise ValueError("explicit fixture ID required")
    if fields.get("emulator_signals_detected") != "false":
        raise ValueError("emulator/unknown environment is not physical evidence")
    if fields.get("warm_persistent_hit") != "true":
        raise ValueError("warm persistent index not verified")
    if fields.get("warm_file_count_delta") != "0":
        raise ValueError("inventory changed during benchmark")
    if fields.get("destructive_actions") != "false":
        raise ValueError("non-readonly debug operation not admissible")
    if not fields.get("device") or not fields.get("sdk"):
        raise ValueError("missing device/API")

    keys = (
        "captured_at_ms", "sdk", "files_total", "cold_index_rows_reused",
        "cold_index_rows_replaced", "cold_index_file_rows_ms",
    )
    values = {}
    for key in keys:
        try:
            values[key] = int(fields[key])
        except (KeyError, ValueError) as error:
            raise ValueError("missing/invalid numeric phase measurement: " + key) from error
    if values["captured_at_ms"] <= 0 or values["files_total"] <= 0 or values["sdk"] < 21:
        raise ValueError("impossible timestamp/size/API")
    if min(values["cold_index_rows_reused"], values["cold_index_rows_replaced"], values["cold_index_file_rows_ms"]) < 0:
        raise ValueError("negative file-row evidence")
    if values["cold_index_rows_reused"] + values["cold_index_rows_replaced"] != values["files_total"]:
        raise ValueError("reconciliation counters do not match total")
    return {
        "schema_version": 1, "fixture_id": fixture,
        "device": fields["device"], "environment": "physical",
        "api_level": values["sdk"], "mode": "cold",
        "file_count": values["files_total"],
        "rows_reused": values["cold_index_rows_reused"],
        "rows_replaced": values["cold_index_rows_replaced"],
        "integrity_digest": "sha256:" + integrity_sha,
        "source_sha": sha, "file_rows_ms": values["cold_index_file_rows_ms"],
        "captured_at_ms": values["captured_at_ms"],
        "source_kind": "USER_SUPPLIED_PHYSICAL_DIAGNOSTICS_UNVERIFIED",
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, action="append", required=True,
                        help="actual copied OMNI_PERF_V1 report, repeat for each independent run")
    parser.add_argument("--source-sha", required=True)
    parser.add_argument("--fixture-id", required=True)
    parser.add_argument("--integrity-sha256", required=True,
                        help="independently measured/verified inventory digest, never guessed")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        if args.output.exists() or args.output.is_symlink():
            raise ValueError("will not overwrite existing evidence JSONL")
        rows = [
            row(path.read_text(encoding="utf-8"), args.source_sha,
                args.fixture_id, args.integrity_sha256)
            for path in args.input
        ]
        if len({r["captured_at_ms"] for r in rows}) != len(rows):
            raise ValueError("duplicated captured_at_ms; independent measurements required")
        if any(any(r[k] != rows[0][k] for k in (
            "fixture_id", "device", "environment", "api_level", "mode",
            "file_count", "rows_reused", "rows_replaced", "integrity_digest", "source_sha",
        )) for r in rows[1:]):
            raise ValueError("inconsistent device/library/reused rows; separate the series")
        args.output.write_text(
            "".join(json.dumps(r, sort_keys=True) + "\n" for r in rows), encoding="utf-8"
        )
        print(f"OMNI_PERF_EVIDENCE_EXPORTED: ${len(rows)} reports; still NOT_CERTIFIED")
        return 0
    except (OSError, ValueError) as error:
        print("BLOCKED_PERFORMANCE_EVIDENCE: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
