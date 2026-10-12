#!/usr/bin/env bash
set -Eeuo pipefail

HOST_ROOT="${RUNNER_TEMP:-/tmp}/omni-parity-fixture"
DEVICE_ROOT="/sdcard/Download/OmniParity"
HOST_THUMB="${RUNNER_TEMP:-/tmp}/omni-parity-thumb.bin"
DEVICE_THUMB_ROOT="/sdcard/DCIM/.thumbnails"

rm -rf "$HOST_ROOT" "$HOST_THUMB"
mkdir -p "$HOST_ROOT"/{empty,duplicates,large}

printf 'parity-fixture' > "$HOST_ROOT/duplicates/a.txt"
cp "$HOST_ROOT/duplicates/a.txt" "$HOST_ROOT/duplicates/b.txt"
printf 'erutxif-ytirap' > "$HOST_ROOT/duplicates/same-size-different.txt"
truncate -s $((12 * 1024 * 1024)) "$HOST_ROOT/large/parity-large.bin"
printf 'thumb-parity' > "$HOST_THUMB"

# Use adb sync/push instead of writing through an interactive device shell.
# Fresh hosted AVDs can temporarily reject shell redirections on emulated
# storage even after mkdir succeeds, while adb push remains reliable.
adb shell rm -rf "$DEVICE_ROOT"
adb shell mkdir -p "$DEVICE_ROOT"
adb push "$HOST_ROOT/." "$DEVICE_ROOT/"

adb shell mkdir -p "$DEVICE_THUMB_ROOT"
adb push "$HOST_THUMB" "$DEVICE_THUMB_ROOT/parity-thumb.bin"
adb shell sync

for required in   "$DEVICE_ROOT/duplicates/a.txt"   "$DEVICE_ROOT/duplicates/b.txt"   "$DEVICE_ROOT/duplicates/same-size-different.txt"   "$DEVICE_ROOT/large/parity-large.bin"   "$DEVICE_THUMB_ROOT/parity-thumb.bin"; do
  if ! adb shell test -f "$required"; then
    echo "[Omni/Parity] missing fixture file: $required" >&2
    exit 1
  fi
done

echo "[Omni/Parity] fixture prepared via adb push."
