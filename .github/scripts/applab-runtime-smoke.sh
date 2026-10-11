#!/usr/bin/env bash
set -Eeuo pipefail

: "${APK_PATH:?APK_PATH is required}"
: "${PACKAGE_ID:?PACKAGE_ID is required}"
: "${APPLAB_PLAN:?APPLAB_PLAN is required}"
: "${GITHUB_WORKSPACE:?GITHUB_WORKSPACE is required}"

adb install -r -t "$APK_PATH"
adb shell appops set "$PACKAGE_ID" MANAGE_EXTERNAL_STORAGE allow || true

bash "$GITHUB_WORKSPACE/target/.github/scripts/prepare-parity-fixture.sh"

export REPORT_DIR="$GITHUB_WORKSPACE/applab-runtime-smoke"
export APPLAB_VERSION="4.5.0-pinned-smoke"
export APPLAB_ANALYSIS_MODE="fast"
export APPLAB_ANALYSIS_PLAN_JSON="$APPLAB_PLAN"
export APPLAB_TARGET_ROOT="$GITHUB_WORKSPACE/target"
export APPLAB_PROJECT_ROOT="$GITHUB_WORKSPACE/target"
export RUN_MAESTRO="true"
export MAESTRO_FLOW="$GITHUB_WORKSPACE/target/.maestro/monetization-smoke.yaml"

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
  local crawl_json="$REPORT_DIR/interaction-crawl.json"
  local app_logcat="$REPORT_DIR/app-logcat.txt"

  [[ -s "$crawl_json" ]] || return 1

  local reason
  reason="$(
    python3 - "$crawl_json" <<'PY'
import json
import sys
from pathlib import Path

payload = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
print(payload.get("reason") or "")
PY
  )"

  [[ "$reason" == "application process died" ]] || return 1

  if [[ -s "$app_logcat" ]] && grep -Eiq 'FATAL EXCEPTION|ANR in |OutOfMemoryError|SIG(SEGV|ABRT|BUS|ILL)' "$app_logcat"; then
    return 1
  fi

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

for evidence in   summary.md   result.json   interaction-crawl.md   interaction-crawl.json   visual-qa.md   system-lab.md   app-logcat.txt; do
  path="$REPORT_DIR/$evidence"
  if [[ -s "$path" ]]; then
    echo "===== $evidence ====="
    cat "$path"
  fi
done

exit "$status"
