#!/usr/bin/env bash
set -Eeuo pipefail

: "${RUNNER_TEMP:?RUNNER_TEMP is required}"

FIXTURE_ROOT="$RUNNER_TEMP/omni-perf-fixture"
DEVICE_ROOT="/sdcard/Download/OmniPerf"

rm -rf "$FIXTURE_ROOT"
mkdir -p "$FIXTURE_ROOT"/{bulk,same-size,duplicates}

python3 - "$FIXTURE_ROOT" <<'PY'
from pathlib import Path
import os
import sys

root = Path(sys.argv[1])

# Many small files stress inventory/list handling without bloating the artifact.
for i in range(1200):
    payload = (f"omni-perf-{i:04d}-".encode("utf-8") * 96)[:2048]
    (root / "bulk" / f"item-{i:04d}.dat").write_bytes(payload)

# Same-size, intentionally different files stress the duplicate prefilter.
size = 256 * 1024
for i in range(48):
    marker = i.to_bytes(4, "little")
    block = (marker + bytes([i % 251])) * (size // 5 + 1)
    (root / "same-size" / f"unique-{i:02d}.bin").write_bytes(block[:size])

# A real duplicate group still has to survive sample -> full SHA-256 -> byte verify.
duplicate = (b"OMNI_DUPLICATE_PAYLOAD_" * 65536)[:1024 * 1024]
for name in ("perf-duplicate-a.bin", "perf-duplicate-b.bin", "perf-duplicate-c.bin"):
    (root / "duplicates" / name).write_bytes(duplicate)

# Keep one archive/installer candidate in the large-library dataset.
(root / "perf-archive.zip").write_bytes(b"PK\x03\x04" + os.urandom(4092))
(root / "perf-installer.apk").write_bytes(b"PK\x03\x04" + os.urandom(4092))
PY

adb shell rm -rf "$DEVICE_ROOT"
adb shell mkdir -p "$DEVICE_ROOT"
adb push "$FIXTURE_ROOT/." "$DEVICE_ROOT/"

count="$(adb shell "find '$DEVICE_ROOT' -type f 2>/dev/null | wc -l" | tr -d '\r[:space:]')"
echo "[Omni/Perf] fixture files on device: $count"
if [[ "$count" -lt 1250 ]]; then
  echo "[Omni/Perf] fixture incomplete" >&2
  exit 1
fi
