#!/usr/bin/env bash
set -Eeuo pipefail

command -v ffmpeg >/dev/null || {
  echo "[Gallery QA] ffmpeg missing on runner" >&2
  exit 1
}

FIXTURE_ROOT="${RUNNER_TEMP:-/tmp}/omni-gallery-qa"
mkdir -p "$FIXTURE_ROOT"

ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i testsrc=size=360x640:rate=1 -frames:v 1 \
  "$FIXTURE_ROOT/OmniQA-portrait.jpg"

ffmpeg -hide_banner -loglevel error -y \
  -f lavfi -i testsrc=size=640x360:rate=15 \
  -t 15 -c:v libx264 -preset ultrafast -pix_fmt yuv420p \
  -movflags +faststart "$FIXTURE_ROOT/OmniQA-video.mp4"

adb shell mkdir -p /sdcard/DCIM/Camera /sdcard/Movies
adb push "$FIXTURE_ROOT/OmniQA-portrait.jpg" /sdcard/DCIM/Camera/
adb push "$FIXTURE_ROOT/OmniQA-video.mp4" /sdcard/Movies/
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
  -d file:///sdcard/DCIM/Camera/OmniQA-portrait.jpg
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
  -d file:///sdcard/Movies/OmniQA-video.mp4

found=0
for attempt in $(seq 1 25); do
  if adb shell content query --uri content://media/external/file \
      --projection _id:_display_name 2>/dev/null | grep -q "OmniQA-video.mp4" &&
     adb shell content query --uri content://media/external/file \
      --projection _id:_display_name 2>/dev/null | grep -q "OmniQA-portrait.jpg"; then
    found=1
    break
  fi
  sleep 2
done

if [[ "$found" != "1" ]]; then
  echo "[Gallery QA] generated media were not indexed by MediaStore" >&2
  exit 1
fi
echo "[Gallery QA] MediaStore found deterministic portrait JPG and MP4 fixtures"
