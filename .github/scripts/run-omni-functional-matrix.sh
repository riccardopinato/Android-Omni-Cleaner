#!/usr/bin/env bash
set -Eeuo pipefail

# Every test runs on a disposable Android 35 emulator with synthetic assets.
# Zero executed, missing, failed, or unexpected counts never count as PASS.

APP_APK="app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
[[ -s "$APP_APK" && -s "$TEST_APK" ]] || { echo "TEST_APK_MISSING"; exit 2; }
adb install -r -t "$APP_APK"
adb install -r -t "$TEST_APK"
LOG="${RUNNER_TEMP:-/tmp}/omni-functional-matrix.txt"
: > "$LOG"

CASES=(
  'com.riccardopinato.omnicleaner.AppContractInstrumentedTest|4'
  'com.riccardopinato.omnicleaner.LocalizationInstrumentedTest|1'
  'com.riccardopinato.omnicleaner.ManifestHardeningInstrumentedTest|1'
  'com.riccardopinato.omnicleaner.cleaner.FactoryPythonFixtureInstrumentedTest|1'
  'com.riccardopinato.omnicleaner.cleaner.SafeTrashStorageInstrumentedTest|2'
  'com.riccardopinato.omnicleaner.gallery.NativeVideoPlaybackInstrumentedTest|1'
  'com.riccardopinato.omnicleaner.monetization.PremiumCacheInstrumentedTest|1'
  'com.riccardopinato.omnicleaner.cleaner.BackgroundIndexRuntimeInstrumentedTest|2'
  'com.riccardopinato.omnicleaner.gallery.MediaTrashReadbackInstrumentedTest|1'
)

for spec in "${CASES[@]}"; do
  classname="${spec%|*}"
  expected="${spec##*|}"
  printf 'RUN_ANDROID_CLASS: %s expected=%s\n' "$classname" "$expected"
  if output="$(adb shell am instrument -w -e class "$classname" com.riccardopinato.omnicleaner.debug.test/androidx.test.runner.AndroidJUnitRunner 2>&1)"; then
    printf '%s\n' "$output"
  else
    printf '%s\n' "$output"
    printf 'FAIL_ANDROID_CLASS: %s (instrumentation failed)\n' "$classname" | tee -a "$LOG"
    exit 1
  fi
  if printf '%s\n' "$output" | grep -Fq "OK ($expected test"; then
    printf 'PASS_ANDROID_CLASS: %s tests=%s\n' "$classname" "$expected" | tee -a "$LOG"
  else
    printf 'FAIL_ANDROID_CLASS: %s (missing or unexpected count)\n' "$classname" | tee -a "$LOG"
    exit 1
  fi
done

echo "ALL_ANDROID_CLASSES_EXECUTED_AND_VERIFIED" | tee -a "$LOG"
