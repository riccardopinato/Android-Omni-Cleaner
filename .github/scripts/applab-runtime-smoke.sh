#!/usr/bin/env bash
set -Eeuo pipefail

: "${APK_PATH:?APK_PATH is required}"
: "${PACKAGE_ID:?PACKAGE_ID is required}"
: "${APPLAB_PLAN:?APPLAB_PLAN is required}"
: "${GITHUB_WORKSPACE:?GITHUB_WORKSPACE is required}"

SOURCE_BRANCH="${OMNI_SOURCE_BRANCH:-${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-}}}"

# Source build only proves androidTest compiles. Execute the existing safety,
# storage, localization and entitlement instrumentation on this live emulator.
# This is emulator evidence, never physical-device or Play certification.
if [[ "$SOURCE_BRANCH" == "audit/omni-safety-ux-360-20261115" ]]; then
  echo "[Omni/AppLab] running device-backed connectedDebugAndroidTest..."
  (
    cd "$GITHUB_WORKSPACE/target"
    ./gradlew connectedDebugAndroidTest --stacktrace
  )
fi

adb install -r -t "$APK_PATH"
adb shell appops set "$PACKAGE_ID" MANAGE_EXTERNAL_STORAGE allow || true

bash "$GITHUB_WORKSPACE/target/.github/scripts/prepare-parity-fixture.sh"

if [[ "$SOURCE_BRANCH" == "step-9-performance-deep-qa" ]]; then
  echo "[Omni/AppLab] preparing Step 9 large-library fixture..."
  bash "$GITHUB_WORKSPACE/target/.github/scripts/prepare-performance-fixture.sh"
fi

export REPORT_DIR="$GITHUB_WORKSPACE/applab-runtime-smoke"
export APPLAB_VERSION="4.5.0-pinned-smoke"
if [[ "$SOURCE_BRANCH" == "step-9-performance-deep-qa" ]]; then
  export APPLAB_ANALYSIS_MODE="full"
else
  export APPLAB_ANALYSIS_MODE="fast"
fi
export APPLAB_ANALYSIS_PLAN_JSON="$APPLAB_PLAN"
export APPLAB_TARGET_ROOT="$GITHUB_WORKSPACE/target"
export APPLAB_PROJECT_ROOT="$GITHUB_WORKSPACE/target"
export RUN_MAESTRO="true"
export MAESTRO_FLOW="$GITHUB_WORKSPACE/target/.maestro/ui-redesign.yaml"

cd "$GITHUB_WORKSPACE/.applab"

run_verifier() {
  rm -rf "$REPORT_DIR"
  mkdir -p "$REPORT_DIR"

  set +e
  bash ./scripts/verify_apk.sh "$APK_PATH" "$PACKAGE_ID"
  local status=$?
  set -e
  return "$status"
}

is_transient_pid_flake() {
  local interaction_json="$REPORT_DIR/interaction-crawl.json"
  local journey_json="$REPORT_DIR/journey-crawl.json"
  local app_logcat="$REPORT_DIR/app-logcat.txt"
  local system_logcat="$REPORT_DIR/logcat.txt"

  local transient
  transient="$(
    python3 - "$interaction_json" "$journey_json" <<'PY'
import json
import sys
from pathlib import Path

def contains_pid_loss(value):
    if isinstance(value, dict):
        if value.get("reason") == "application process died":
            return True
        return any(contains_pid_loss(item) for item in value.values())
    if isinstance(value, list):
        return any(contains_pid_loss(item) for item in value)
    return False

found = False
for raw in sys.argv[1:]:
    path = Path(raw)
    if not path.is_file():
        continue
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        continue
    if payload.get("result") == "FAIL" and contains_pid_loss(payload):
        found = True
        break

print("yes" if found else "no")
PY
  )"

  # The system lab intentionally force-stops/restarts the app; a stale PID
  # sampled immediately afterward can be transient when the lab itself passed.
  # Never waive a real fatal signal or a repeated failed verification.
  local system_restart_pid_loss="no"
  if [[ -s "$REPORT_DIR/summary.md" ]] &&
     grep -Fq "Application process died during system-lab." "$REPORT_DIR/summary.md" &&
     [[ -s "$REPORT_DIR/system-lab.md" ]] &&
     grep -Fq "Force-stop restart: PASS" "$REPORT_DIR/system-lab.md"; then
    system_restart_pid_loss="yes"
  fi
  [[ "$transient" == "yes" || "$system_restart_pid_loss" == "yes" ]] || return 1

  for log_file in "$app_logcat" "$system_logcat"; do
    if [[ -s "$log_file" ]] &&
       grep -Eiq 'FATAL EXCEPTION|ANR in |OutOfMemoryError|SIG(SEGV|ABRT|BUS|ILL)' "$log_file"; then
      return 1
    fi
  done

  return 0
}

status=0
run_verifier || status=$?
if [[ "$status" -ne 0 ]]; then
  if is_transient_pid_flake; then
    echo "[Omni/AppLab] transient PID loss after crawler/system restart without crash evidence; retrying once..."
    sleep 3
    if run_verifier; then
      status=0
    else
      status=$?
    fi
  fi
fi

if [[ "$status" -eq 0 ]]; then
  echo "[Omni/AppLab] running retained critical storage parity journey..."
  if maestro test "$GITHUB_WORKSPACE/target/.maestro/parity-storage.yaml"; then
    echo "[Omni/AppLab] critical storage parity journey passed."
  else
    status=$?
    echo "[Omni/AppLab] critical storage parity journey failed with status $status."
  fi
fi

# This is separate from AppLab's UIAutomator hierarchy warnings. It proves
# the app can restart and navigate after AppLab's storage-recovery lab, rather
# than incorrectly converting a missing hierarchy snapshot into PASS.
if [[ "$status" -eq 0 ]]; then
  echo "[Omni/AppLab] running two non-destructive restart/recovery journeys..."
  for cycle in 1 2; do
    adb shell am force-stop "$PACKAGE_ID" || true
    if maestro test "$GITHUB_WORKSPACE/target/.maestro/restart-recovery.yaml"; then
      echo "[Omni/AppLab] restart/recovery cycle ${cycle}/2 PASSED."
    else
      status=$?
      echo "[Omni/AppLab] restart/recovery cycle ${cycle}/2 FAILED (status $status)."
      echo "[Omni/AppLab] AppLab persistence/storage warnings remain independent evidence."
      adb shell dumpsys activity activities | tail -n 90 || true
      break
    fi
  done
fi

if [[ "$status" -eq 0 ]]; then
  echo "[Omni/AppLab] running accessibility journeys at 1.3x, 1.5x, and 2.0x font scale..."
  for scale in 1.3 1.5 2.0; do
    adb shell settings put system font_scale "$scale" || true
    adb shell am force-stop "$PACKAGE_ID" || true

    if maestro test "$GITHUB_WORKSPACE/target/.maestro/localization-accessibility.yaml"; then
      echo "[Omni/AppLab] localization/accessibility journey PASSED at ${scale}x."
    else
      status=$?
      echo "[Omni/AppLab] localization/accessibility journey FAILED at ${scale}x with status $status."
      echo "[Omni/AppLab] capturing bounded current hierarchy..."
      timeout 20s adb shell uiautomator dump /sdcard/omni-a11y-failure.xml || true
      timeout 10s adb shell cat /sdcard/omni-a11y-failure.xml | head -c 20000 || true
      echo
      break
    fi
  done

  adb shell settings put system font_scale 1.0 || true
  adb shell am force-stop "$PACKAGE_ID" || true
fi

for evidence in \
  summary.md \
  result.json \
  interaction-crawl.md \
  interaction-crawl.json \
  visual-qa.md \
  system-lab.md \
  persistence-lab.md \
  configuration-lab.md \
  resource-pressure-lab.md \
  background-lab.md \
  storage-lab.md \
  performance-lab.md \
  app-logcat.txt; do
  path="$REPORT_DIR/$evidence"
  if [[ -s "$path" ]]; then
    echo "===== $evidence ====="
    cat "$path"
  fi
done

exit "$status"
