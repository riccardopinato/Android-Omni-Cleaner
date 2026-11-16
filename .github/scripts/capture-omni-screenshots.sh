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

cat > "$OUTPUT/README.txt" <<EOF
Android Omni Cleaner: five genuine Android emulator screenshots.
Git commit: ${GITHUB_SHA:-unknown}
Source: debug APK assembled from this commit on the workflow runner.
Device: Google APIs x86_64, Pixel 7 Pro, Android API 35
Theme: system light; font scale: 1.0; no personal user data.
Screens: 01-home, 02-clean, 03-gallery, 04-privacy, 05-pro.
Evidence: UI only. No real physical device or Play certification.
EOF
echo "Five genuine UI screenshots captured successfully."
