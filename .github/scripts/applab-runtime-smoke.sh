#!/usr/bin/env bash
set -Eeuo pipefail

: "${APK_PATH:?APK_PATH is required}"
: "${PACKAGE_ID:?PACKAGE_ID is required}"
: "${APPLAB_PLAN:?APPLAB_PLAN is required}"
: "${GITHUB_WORKSPACE:?GITHUB_WORKSPACE is required}"

adb install -r -t "$APK_PATH"
adb shell appops set "$PACKAGE_ID" MANAGE_EXTERNAL_STORAGE allow || true

bash "$GITHUB_WORKSPACE/target/.github/scripts/prepare-parity-fixture.sh"

if [[ "${GITHUB_REF:-}" == "refs/heads/step-9-performance-deep-qa" ]]; then
  echo "[Omni/AppLab] preparing Step 9 large-library fixture..."
  bash "$GITHUB_WORKSPACE/target/.github/scripts/prepare-performance-fixture.sh"
fi

export REPORT_DIR="$GITHUB_WORKSPACE/applab-runtime-smoke"
export APPLAB_VERSION="4.5.0-pinned-smoke"
if [[ "${GITHUB_REF:-}" == "refs/heads/step-9-performance-deep-qa" ]]; then
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

  [[ "$transient" == "yes" ]] || return 1

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
    echo "[Omni/AppLab] transient crawler PID loss without crash evidence; retrying once..."
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

if [[ "$status" -eq 0 ]]; then
  echo "[Omni/AppLab] running localization/accessibility journey at 1.3x font scale..."
  adb shell settings put system font_scale 1.3 || true
  adb shell am force-stop "$PACKAGE_ID" || true

  if maestro test "$GITHUB_WORKSPACE/target/.maestro/localization-accessibility.yaml"; then
    echo "[Omni/AppLab] localization/accessibility journey passed."
  else
    status=$?
    echo "[Omni/AppLab] localization/accessibility journey failed with status $status."
  fi

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
