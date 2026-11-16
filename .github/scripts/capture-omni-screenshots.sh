#!/usr/bin/env bash
set -Eeuo pipefail
: "${APK_PATH:?APK_PATH must point to the exact-head debug APK}"
: "${GITHUB_WORKSPACE:?GITHUB_WORKSPACE is required}"

PKG="com.riccardopinato.omnicleaner.debug"
OUTPUT="$GITHUB_WORKSPACE/omni-design-screenshots"
mkdir -p "$OUTPUT"/maestro

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
    clearState: true
- tapOn:
    id: "omni_nav_home"
- assertVisible: "Smart cleaning"
- takeScreenshot: 01-home
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
for name in 01-home 02-clean 03-gallery 04-privacy 05-pro; do
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
DETAIL_FLOW
mkdir -p "$OUTPUT/maestro-details"
maestro test --test-output-dir "$OUTPUT/maestro-details" "$OUTPUT/detail-capture.yaml"
for name in 06-clean-duplicates 07-clean-filters 08-privacy-quick 09-privacy-batch 10-pro-manage; do
  candidate="$(find "$OUTPUT/maestro-details" -type f -name "$name.png" -print -quit)"
  if [[ -z "$candidate" ]]; then
    echo "::error::Missing detail screenshot $name.png"
    exit 1
  fi
  cp "$candidate" "$OUTPUT/$name.png"
  test -s "$OUTPUT/$name.png"
done

# Optional emulator media proof: build a synthetic clip (not real user data),
# index it via MediaStore, and exercise the native play control. This must not
# mark the five/dedicated screen capture PASS if real video never ran.
if command -v ffmpeg >/dev/null 2>&1; then
  if ffmpeg -hide_banner -loglevel error -y \
      -f lavfi -i "testsrc=size=640x360:rate=12" -t 4 \
      -c:v mpeg4 -q:v 5 -pix_fmt yuv420p "$OUTPUT/omni-qa-landscape.mp4"; then
    adb shell mkdir -p /sdcard/Movies/OmniQA
    adb push "$OUTPUT/omni-qa-landscape.mp4" /sdcard/Movies/OmniQA/omni-qa-landscape.mp4
    adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
       -d file:///sdcard/Movies/OmniQA/omni-qa-landscape.mp4 || true
    adb shell pm grant "$PKG" android.permission.READ_MEDIA_IMAGES || true
    adb shell pm grant "$PKG" android.permission.READ_MEDIA_VIDEO || true
    adb shell am force-stop "$PKG" || true
    sleep 5
    if adb shell content query --uri content://media/external/video/media \
       --projection _display_name | grep -q "omni-qa-landscape.mp4"; then
      cat > "$OUTPUT/video-capture.yaml" <<'VIDEO_FLOW'
appId: com.riccardopinato.omnicleaner.debug
---
- launchApp:
    clearState: false
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
- takeScreenshot: 11-video-playing
VIDEO_FLOW
      mkdir -p "$OUTPUT/maestro-video"
      if maestro test --test-output-dir "$OUTPUT/maestro-video" "$OUTPUT/video-capture.yaml"; then
        screenshot="$(find "$OUTPUT/maestro-video" -type f -name '11-video-playing.png' -print -quit)"
        if [[ -n "$screenshot" ]]; then
          cp "$screenshot" "$OUTPUT/11-video-playing.png"
          echo "VIDEO_PLAYBACK_SMOKE_PASS: synthetic original-aspect MP4"
        fi
      else
        echo "::warning::VIDEO_PLAYBACK_NOT_VERIFIED: media UI flow failed"
      fi
    else
      echo "::warning::VIDEO_PLAYBACK_NOT_VERIFIED: synthetic video not indexed by MediaStore"
    fi
  else
    echo "::warning::VIDEO_PLAYBACK_NOT_VERIFIED: ffmpeg could not generate synthetic video"
  fi
else
  echo "::warning::VIDEO_PLAYBACK_NOT_VERIFIED: ffmpeg unavailable"
fi

cat > "$OUTPUT/README.txt" <<EOF
Android Omni Cleaner: five genuine Android emulator screenshots.
Git commit: ${GITHUB_SHA:-unknown}
Source: debug APK assembled from this commit on the workflow runner.
Device: Google APIs x86_64, Pixel 7 Pro, Android API 35
Theme: system light; font scale: 1.0; no personal user data.
Screens: 01-home, 02-clean, 03-gallery, 04-privacy, 05-pro.
Evidence: UI only. No real physical device or Play certification.
EOF
echo "Ten navigation screenshots captured; video playback optional if indexed."
