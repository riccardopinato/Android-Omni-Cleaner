#!/usr/bin/env bash
set -Eeuo pipefail
: "${APK_PATH:?APK_PATH must point to the exact-head debug APK}"
: "${GITHUB_WORKSPACE:?GITHUB_WORKSPACE is required}"

PKG="com.riccardopinato.omnicleaner.debug"
OUTPUT="$GITHUB_WORKSPACE/omni-design-screenshots"
mkdir -p "$OUTPUT"/maestro

# Keep actionable evidence if Maestro/device startup fails.
capture_failure() {
  adb shell screencap -p /sdcard/omni-visual-failure.png >/dev/null 2>&1 || true
  adb pull /sdcard/omni-visual-failure.png "$OUTPUT/failure-screen.png" >/dev/null 2>&1 || true
  adb logcat -d -t 350 > "$OUTPUT/failure-logcat.txt" 2>/dev/null || true
  adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1 || true
  adb pull /sdcard/window.xml "$OUTPUT/failure-window.xml" >/dev/null 2>&1 || true
}
trap capture_failure ERR

# Reproducible empty emulator, English interface, light theme, normal font.
adb wait-for-device
adb shell settings put system font_scale 1.0
adb shell cmd uimode night no || true
adb install -r -t "$APK_PATH"
adb shell appops set "$PKG" MANAGE_EXTERNAL_STORAGE allow || true

cat > "$OUTPUT/visual-capture.yaml" <<'FLOW'
appId: com.riccardopinato.omnicleaner.debug
---
- launchApp:
    clearState: false
# Some Android API35 emulator images display an *external* Pixel Launcher ANR
# over the running Omni Cleaner. Dismiss only that launcher modal.
- tapOn:
    text: "Close app"
    optional: true
# Fresh install, do not revoke app-ops after granting storage access.
# Default startup route IS Home. Do not tap a tab before Compose is ready.
- extendedWaitUntil:
    visible: "Omni Cleaner"
    timeout: 90000
- takeScreenshot: 01-home
# Color Bento 2.0: capture real editor mode and return without changing layout.
- tapOn:
    id: "omni_bento_v2_editor_toggle"
- assertVisible:
    id: "omni_bento_v2_editor_toggle"
- takeScreenshot: 23-home-deck-expanded
- tapOn:
    id: "omni_bento_v2_editor_toggle"
- tapOn:
    id: "omni_nav_clean"
- assertVisible: "Deep Cleaning"
- takeScreenshot: 02-clean
- tapOn:
    id: "omni_nav_gallery"
- assertVisible: "Gallery cleaner"
- takeScreenshot: 03-gallery
- tapOn:
    id: "omni_nav_privacy"
- assertVisible: "EXIF privacy"
- takeScreenshot: 04-privacy
- tapOn:
    id: "omni_nav_pro"
- assertVisible: "Omni Cleaner Pro"
- takeScreenshot: 05-pro
FLOW

maestro test --test-output-dir "$OUTPUT/maestro" "$OUTPUT/visual-capture.yaml"

# Maestro stores takeScreenshot images in a nested per-flow artifact folder.
# Copy those actual pixels into a stable, user-friendly artifact layout.
for name in 01-home 23-home-deck-expanded 02-clean 03-gallery 04-privacy 05-pro; do
  candidate="$(find "$OUTPUT/maestro" -type f -name "$name.png" -print -quit)"
  if [[ -z "$candidate" ]]; then
    echo "::error::Missing Maestro screenshot $name.png"
    find "$OUTPUT/maestro" -type f \( -name "*.png" -o -name "*.jpg" \) -print || true
    exit 1
  fi
  cp "$candidate" "$OUTPUT/$name.png"
  test -s "$OUTPUT/$name.png"
  file "$OUTPUT/$name.png"
done

# Dedicated detail screens are rendered independently, with Android Back.
cat > "$OUTPUT/detail-capture.yaml" <<'DETAIL_FLOW'
appId: com.riccardopinato.omnicleaner.debug
---
- launchApp:
    clearState: false
- tapOn:
    id: "omni_nav_clean"
- tapOn:
    id: "omni_clean_tool_duplicates"
- assertVisible: "Find duplicates"
- takeScreenshot: 06-clean-duplicates
- pressKey: Back
- scrollUntilVisible:
    element:
      id: "omni_clean_tool_filters"
    direction: DOWN
    timeout: 30000
- tapOn:
    id: "omni_clean_tool_filters"
- assertVisible: "Filters & thresholds"
- takeScreenshot: 07-clean-filters
- pressKey: Back
- tapOn:
    id: "omni_nav_privacy"
- tapOn:
    id: "omni_privacy_quick"
- assertVisible: "Choose one JPEG photo"
- takeScreenshot: 08-privacy-quick
- pressKey: Back
- tapOn:
    id: "omni_privacy_batch"
- assertVisible: "Choose photo folder"
- takeScreenshot: 09-privacy-batch
- pressKey: Back
- tapOn:
    id: "omni_nav_pro"
- tapOn:
    id: "omni_pro_manage"
- assertVisible: "Restore purchase"
- takeScreenshot: 10-pro-manage
- pressKey: Back
- tapOn:
    id: "omni_nav_clean"
- tapOn:
    id: "omni_clean_tool_intelligence"
- assertVisible: "Storage Intelligence"
- tapOn:
    id: "omni_storage_intelligence_scan"
- extendedWaitUntil:
    visible: "Storage map"
    timeout: 120000
- takeScreenshot: 12-storage-intelligence
- pressKey: Back
- tapOn:
    id: "omni_nav_gallery"
- tapOn:
    id: "omni_gallery_open_pro"
- assertVisible: "Gallery Pro"
- takeScreenshot: 13-gallery-pro
- pressKey: Back
- tapOn:
    id: "omni_nav_clean"
- tapOn:
    id: "omni_clean_tool_review"
- assertVisible:
    id: "omni_review_center"
- takeScreenshot: 14-review-center
DETAIL_FLOW
# Synthetic, disposable emulator fixture to verify a *nonempty* visual
# storage map. The first five screenshots are captured before this is created.
# These paths belong to the short-lived GitHub runner, never a user's device.
adb shell mkdir -p /sdcard/Download/OmniStorageQA /sdcard/Documents/OmniStorageQA /sdcard/Music/OmniStorageQA
adb shell "dd if=/dev/zero of=/sdcard/Download/OmniStorageQA/installer.apk bs=1024 count=256 >/dev/null 2>&1"
adb shell "dd if=/dev/zero of=/sdcard/Documents/OmniStorageQA/guide.pdf bs=1024 count=384 >/dev/null 2>&1"
adb shell "dd if=/dev/zero of=/sdcard/Music/OmniStorageQA/archive.zip bs=1024 count=128 >/dev/null 2>&1"
mkdir -p "$OUTPUT/maestro-details"
maestro test --test-output-dir "$OUTPUT/maestro-details" "$OUTPUT/detail-capture.yaml"
for name in 06-clean-duplicates 07-clean-filters 08-privacy-quick 09-privacy-batch 10-pro-manage 12-storage-intelligence 13-gallery-pro 14-review-center; do
  candidate="$(find "$OUTPUT/maestro-details" -type f -name "$name.png" -print -quit)"
  if [[ -z "$candidate" ]]; then
    echo "::error::Missing detail screenshot $name.png"
    exit 1
  fi
  cp "$candidate" "$OUTPUT/$name.png"
  test -s "$OUTPUT/$name.png"
done

# Generate a deterministic, moving and BRIGHT H.264 fixture with hundreds
# of actual video frames; Android screenrecord on a static screen produced ONE
# frame despite an 18s wall-clock recording (PR #37 visual audit).
# The fixture contains no personal media and the video stays read-only.
# This is exact-head QA only, not a production playback implementation.
adb shell mkdir -p /sdcard/Movies/OmniQA
if ! command -v ffmpeg >/dev/null || ! command -v ffprobe >/dev/null; then
  echo "::error::VIDEO_FIXTURE_TOOL_UNAVAILABLE: ffmpeg and ffprobe required for deterministic visual QA"
  exit 1
fi
ffmpeg -hide_banner -loglevel error -nostdin \
    -f lavfi -i 'testsrc2=size=640x360:rate=15' \
    -t 24 -an -c:v libx264 -preset ultrafast -pix_fmt yuv420p \
    -movflags +faststart -y "$OUTPUT/video-fixture.mp4"
video_codec="$(ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 "$OUTPUT/video-fixture.mp4")"
video_frames="$(ffprobe -v error -count_frames -select_streams v:0 -show_entries stream=nb_read_frames -of default=noprint_wrappers=1:nokey=1 "$OUTPUT/video-fixture.mp4")"
video_duration="$(ffprobe -v error -select_streams v:0 -show_entries stream=duration -of default=noprint_wrappers=1:nokey=1 "$OUTPUT/video-fixture.mp4")"
if [[ "$video_codec" != h264 || ! "$video_frames" =~ ^[0-9]+$ ]] || (( video_frames < 200 )); then
  echo "::error::VIDEO_FIXTURE_FRAMES_INVALID: codec=$video_codec frames=$video_frames duration=$video_duration"
  exit 1
fi
# Save one reference frame from the exact video fed to the native Android player.
ffmpeg -hide_banner -loglevel error -nostdin -ss 3 \
    -i "$OUTPUT/video-fixture.mp4" -frames:v 1 -y "$OUTPUT/video-fixture-reference.png"
test -s "$OUTPUT/video-fixture-reference.png"
echo "VIDEO_FIXTURE_FRAMES_OK: codec=$video_codec frames=$video_frames duration=$video_duration"
adb push "$OUTPUT/video-fixture.mp4" /sdcard/Movies/OmniQA/omni-qa-landscape.mp4 >/dev/null
video_bytes="$(adb shell stat -c %s /sdcard/Movies/OmniQA/omni-qa-landscape.mp4 2>/dev/null | tr -d '\r' || true)"
if [[ ! "$video_bytes" =~ ^[0-9]+$ ]] || (( video_bytes < 10000 )); then
  echo "::error::VIDEO_FIXTURE_TRANSFER_INVALID: bytes=$video_bytes"
  exit 1
fi
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
   -d file:///sdcard/Movies/OmniQA/omni-qa-landscape.mp4 || true
adb shell cmd media scan-file /sdcard/Movies/OmniQA/omni-qa-landscape.mp4 || true
adb shell pm grant "$PKG" android.permission.READ_MEDIA_IMAGES || true
adb shell pm grant "$PKG" android.permission.READ_MEDIA_VIDEO || true
adb shell am force-stop "$PKG" || true
sleep 5
if ! adb shell content query --uri content://media/external/video/media \
    --projection _display_name | grep -q "omni-qa-landscape.mp4"; then
  echo "::error::VIDEO_NOT_INDEXED: synthetic MP4 missing from Android MediaStore"
  exit 1
fi
cat > "$OUTPUT/video-capture.yaml" <<'VIDEO_FLOW'
appId: com.riccardopinato.omnicleaner.debug
---
- launchApp:
    clearState: false
# Dismiss only the external Pixel Launcher ANR modal, if present.
- tapOn:
    text: "Close app"
    optional: true
- tapOn:
    id: "omni_nav_gallery"
- tapOn: "Videos only"
- tapOn: "Load gallery"
- tapOn:
    id: "omni_gallery_open_review"
- assertVisible:
    id: "omni_gallery_media_full"
- tapOn:
    id: "omni_gallery_video_play"
- waitForAnimationToEnd:
    timeout: 1000
- takeScreenshot: 11-video-playing
- tapOn:
    id: "omni_gallery_immersive_open"
- assertVisible: "Close"
- assertVisible:
    id: "omni_gallery_immersive_content"
- tapOn:
    id: "omni_gallery_video_play"
- waitForAnimationToEnd:
    timeout: 1000
- takeScreenshot: 15-video-immersive
- tapOn:
    id: "omni_gallery_immersive_close"
- assertVisible:
    id: "omni_gallery_media_full"
VIDEO_FLOW
mkdir -p "$OUTPUT/maestro-video"
if ! maestro test --test-output-dir "$OUTPUT/maestro-video" "$OUTPUT/video-capture.yaml"; then
  echo "::error::VIDEO_IMMERSIVE_QA_FAILED: native player or fullscreen review flow"
  exit 1
fi
for name in 11-video-playing 15-video-immersive; do
  found="$(find "$OUTPUT/maestro-video" -type f -name "$name.png" -print -quit)"
  if [[ -z "$found" ]]; then
    echo "::error::Missing required video screenshot: $name"
    exit 1
  fi
  cp "$found" "$OUTPUT/$name.png"
done
echo "VIDEO_IMMERSIVE_SMOKE_PASS: native MediaStore MP4, playback, fullscreen, Close"

# Visual regression coverage for the intended light/dark, large-font responsive
# shell. This is an independent emulator flow, not Play/physical certification.
adb shell settings put system font_scale 1.5
adb shell cmd uimode night yes || true
adb shell am force-stop "$PKG" || true
cat > "$OUTPUT/dark-large-capture.yaml" <<'DARK_LARGE'
appId: com.riccardopinato.omnicleaner.debug
---
- launchApp:
    clearState: false
# Some Android 35 emulator images show a system Pixel Launcher ANR on top
# of the correctly running app. Dismiss ONLY that launcher process, not
# a genuine Android Omni Cleaner crash/ANR, before asserting product UI.
- tapOn:
    text: "Close app"
    optional: true
- extendedWaitUntil:
    visible: "Omni Cleaner"
    timeout: 90000
- takeScreenshot: 16-home-dark-large
- tapOn:
    id: "omni_nav_clean"
- takeScreenshot: 17-clean-dark-large
- tapOn:
    id: "omni_nav_gallery"
- takeScreenshot: 18-gallery-dark-large
- tapOn:
    id: "omni_nav_privacy"
- takeScreenshot: 19-privacy-dark-large
- tapOn:
    id: "omni_nav_pro"
- takeScreenshot: 20-pro-dark-large
DARK_LARGE
mkdir -p "$OUTPUT/maestro-dark-large"
maestro test --test-output-dir "$OUTPUT/maestro-dark-large" "$OUTPUT/dark-large-capture.yaml"
for name in 16-home-dark-large 17-clean-dark-large 18-gallery-dark-large 19-privacy-dark-large 20-pro-dark-large; do
  screenshot="$(find "$OUTPUT/maestro-dark-large" -type f -name "$name.png" -print -quit)"
  if [[ -z "$screenshot" ]]; then
    echo "::error::Missing dark/large accessibility screenshot: $name"
    exit 1
  fi
  cp "$screenshot" "$OUTPUT/$name.png"
done
adb shell settings put system font_scale 1.0
adb shell cmd uimode night no || true

# Narrow layout + large text must keep all three Gallery media filters visible.
adb shell wm size 360x800
adb shell wm density 160
adb shell settings put system font_scale 1.5
cat > "$OUTPUT/gallery-narrow.yaml" <<'NARROW_GALLERY'
appId: com.riccardopinato.omnicleaner.debug
---
- launchApp:
    clearState: false
- tapOn:
    id: "omni_nav_gallery"
- scrollUntilVisible:
    element:
      id: "omni_gallery_filter_videos"
    direction: DOWN
    timeout: 30000
- assertVisible:
    id: "omni_gallery_filter_all"
- assertVisible:
    id: "omni_gallery_filter_images"
- assertVisible:
    id: "omni_gallery_filter_videos"
- takeScreenshot: 21-gallery-360dp-large
NARROW_GALLERY
maestro test --test-output-dir "$OUTPUT/maestro-narrow" "$OUTPUT/gallery-narrow.yaml"
test -s "$(find "$OUTPUT/maestro-narrow" -name 21-gallery-360dp-large.png -print -quit)"
cp "$(find "$OUTPUT/maestro-narrow" -name 21-gallery-360dp-large.png -print -quit)" "$OUTPUT/21-gallery-360dp-large.png"
# Accessibility: all Gallery filters must fit on a genuine 360dp screen
# even at 200% text scale. Separate from experimental video-motion QA.
adb shell settings put system font_scale 2.0
cat > "$OUTPUT/gallery-200pct.yaml" <<'NARROW_GALLERY_200'
appId: com.riccardopinato.omnicleaner.debug
---
- launchApp:
    clearState: false
- tapOn:
    id: "omni_nav_gallery"
- scrollUntilVisible:
    element:
      id: "omni_gallery_filter_videos"
    direction: DOWN
    timeout: 30000
- assertVisible:
    id: "omni_gallery_filter_all"
- assertVisible:
    id: "omni_gallery_filter_images"
- assertVisible:
    id: "omni_gallery_filter_videos"
- takeScreenshot: 22-gallery-360dp-200pct
NARROW_GALLERY_200
maestro test --test-output-dir "$OUTPUT/maestro-200pct" "$OUTPUT/gallery-200pct.yaml"
picture_200="$(find "$OUTPUT/maestro-200pct" -type f -name 22-gallery-360dp-200pct.png -print -quit)"
test -n "$picture_200" && test -s "$picture_200"
cp "$picture_200" "$OUTPUT/22-gallery-360dp-200pct.png"

# The new Bento Home must render at 360dp/200% without hiding core controls.
cat > "$OUTPUT/home-bento-200pct.yaml" <<'NARROW_BENTO_200'
appId: com.riccardopinato.omnicleaner.debug
---
- launchApp:
    clearState: false
- tapOn:
    id: "omni_nav_home"
- extendedWaitUntil:
    visible: "Omni Cleaner"
    timeout: 30000
- takeScreenshot: 24-home-360dp-200pct
NARROW_BENTO_200
mkdir -p "$OUTPUT/maestro-bento-200pct"
maestro test --test-output-dir "$OUTPUT/maestro-bento-200pct" "$OUTPUT/home-bento-200pct.yaml"
bento_200="$(find "$OUTPUT/maestro-bento-200pct" -type f -name 24-home-360dp-200pct.png -print -quit)"
test -n "$bento_200" && test -s "$bento_200"
cp "$bento_200" "$OUTPUT/24-home-360dp-200pct.png"

adb shell settings put system font_scale 1.0
adb shell wm size reset
adb shell wm density reset

cat > "$OUTPUT/README.txt" <<EOF
Android Omni Cleaner: five genuine Android emulator screenshots.
Git commit: ${OMNI_SOURCE_SHA:-${GITHUB_SHA:-unknown}}
Source: debug APK assembled from this commit on the workflow runner.
Device: Google APIs x86_64, Pixel 7 Pro, Android API 35
Theme: light normal and dark font-scale 1.5; no personal user data.
Screens: 01-home, 02-clean, 03-gallery, 04-privacy, 05-pro, dedicated tools, video, and Storage Intelligence.
The storage map contains synthetic QA files only; numbers are not real device claims.
Evidence: UI only. No real physical device or Play certification.
EOF
echo "Primary, detail, and dark/large screenshots captured; indexed video fullscreen is a hard QA gate."
