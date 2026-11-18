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

# The Files table can publish a name before MediaScanner has classified
# a newly pushed asset as an image or video. Wait for the typed collections,
# which is what the actual Gallery MediaStore query relies on.
found=0
for attempt in $(seq 1 40); do
  if adb shell content query --uri content://media/external/video/media \
      --projection _id:_display_name 2>/dev/null | grep -q "OmniQA-video.mp4" &&
     adb shell content query --uri content://media/external/images/media \
      --projection _id:_display_name 2>/dev/null | grep -q "OmniQA-portrait.jpg"; then
    found=1
    break
  fi
  sleep 2
done

if [[ "$found" != "1" ]]; then
  echo "[Gallery QA] MediaStore typed image/video collections not ready" >&2
  exit 1
fi
# The provider can be visible to the shell before app-side permission
# and MediaStore caches settle on a freshly provisioned emulator.
sleep 4
echo "[Gallery QA] typed MediaStore collections contain both QA fixtures"
