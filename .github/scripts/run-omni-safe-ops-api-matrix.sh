#!/usr/bin/env bash
set -Eeuo pipefail
# No personal media, accounts or real user files: test fixtures only.
API="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
APP="app/build/outputs/apk/debug/app-debug.apk"
TEST="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
test -s "$APP" && test -s "$TEST"
adb install -r -t "$APP"
adb install -r -t "$TEST"
run_class() {
  local classname="$1" expected="$2" output
  echo "BEGIN_SYNTHETIC_CLASS api=$API class=$classname expected=$expected"
  output="$(adb shell am instrument -w -e class "$classname" \
      com.riccardopinato.omnicleaner.debug.test/androidx.test.runner.AndroidJUnitRunner 2>&1)"
  printf '%s\n' "$output"
  if ! printf '%s\n' "$output" | grep -Eq "OK \\($expected tests?\\)"; then
    echo "FAIL_SYNTHETIC_CLASS api=$API class=$classname expected=$expected"
    exit 1
  fi
  echo "PASS_SYNTHETIC_CLASS api=$API class=$classname methods=$expected"
}
run_class com.riccardopinato.omnicleaner.cleaner.SafeOperationsJournalInstrumentedTest 2
if (( API >= 30 )); then
  run_class com.riccardopinato.omnicleaner.gallery.MediaTrashReadbackInstrumentedTest 2
  run_class com.riccardopinato.omnicleaner.cleaner.SafeTrashStorageInstrumentedTest 2
fi
echo "OMNI_SYNTHETIC_API_"$API"_PASS"
