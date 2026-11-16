# Omni Cleaner — Five-step implementation and release gates (2026-11-16)

This is an evidence register, not a production/Play certification.

## Branch ancestry
- PR #22 `design/omni-bento-navigation-v1-1` → `main` (Draft)
- PR #23 `audit/omni-safety-ux-360-20261115` → PR #22 (Draft, safety/Gallery/media)
- PR #24 `feature/omni-storage-intelligence-v1-20261116` → PR #23 (Draft, storage search/map)
- PR #25 `feature/omni-gallery-pro-v1-20261116` → PR #24 (Draft, video and thumbnail similarity)
- New follow-up branch `feature/omni-qa-gallery-review-v1-20261116` → PR #25; do not merge before all inherited gates pass.

## Step 1 — unblock tests and harden startup
- AppLab #43891645556 attempt 2: **PASS** for Gallery feature baseline, 0 lab warnings, 2 restart/recovery journeys, font scale 130/150/200%, Gallery Pro route.
- Storage AppLab #43891418779 attempt 2: FAILED on English header visibility after transition to Advanced, while the underlying AppLab, parity, persistence, storage, two restarts all PASS.
- Added stable `omni_clean_detail_<tool>` root semantics and Compose `key(activeDetail)` to reset lazy-list state when transitioning between detail pages.
- Replaced fragile English UI assertion with `omni_clean_detail_advanced` visibility.
- Visual #43891645498 attempt 2: FAILED at `Smart cleaning` Home after `launchApp(clearState=true)` and previously granted storage app-op. Changed to `clearState=false` on a freshly installed emulator, with bounded explicit wait and failure screenshot + UI hierarchy capture.
- New exact-commit evidence required; these fixes are **not PASS until verified**.

## Step 2 — complete Gallery Pro comparison
- Adds a manual pair-by-pair compare control to potential similarity groups.
- Previews are decoded *from original MediaStore URIs* with the existing capped 2160px original-aspect decoder, never cropped or modified. This is NOT a full-resolution export.
- Photo comparison stays read-only and cannot call Trash or set a keep/delete choice.
- MediaStore access is checked by existing Gallery; grouping remains limited to the loaded page and max 160 thumbnails, with no cloud processing.

## Step 3 — performance and state safety
- Query uses shared StorageInventoryRepository; aggregate on Dispatchers.Default; search exposes at most 200 rows even when 3000 items match.
- JVM stress contract covers 3,000 synthetic files across 30 folders, search cap and precise counts. UI media comparisons hold only one pair of bounded previews.
- Full physical performance, low-memory devices, TalkBack and large genuine MediaStore libraries are **pending**.

## Step 4 — centralized cross-module review
- Adds a dedicated Clean → Review Center, with distinct local file/folder and Android MediaStore queue totals.
- Local duplicates and large files are de-duplicated by path to avoid inflating byte totals. MediaStore queue is reported separately; never claim guaranteed savings.
- Adds open-review navigation to existing Big Files, Duplicates, Folders and Gallery, preserving current permission-aware confirmation mechanics.
- Only extra action is clearing non-destructive local selections. No new delete, restore, trash or permission API.
- Stale selections from prior scans are flagged, not considered validated.

## Step 5 — integration/readiness (deliberate gate)
- [ ] New follow-up Android CI PASS (lint, JVM, Kotlin debug/release).
- [ ] New follow-up AppLab PASS with Storage + Gallery + Review journeys, persistence and storage.
- [ ] Screenshots for Home, Gallery Pro, Storage and Review Center; capture actual frame pixels, not placeholder.
- [ ] Exact-head APK debug Release asset with SHA-256 verified.
- [ ] Merge staged PRs in dependency order #22 → #23 → #24 → #25 → follow-up **only after all technical gates and a review**.
- [ ] Physical Android MediaStore/SAF/Trash restore and no-data-loss tests (multiple API levels/OEMs); no external certification inferred.
- [ ] Production signing, Play policy, AdMob/UMP/Billing and versionCode verification before release.

Do not merge Draft PRs automatically while the integration criteria are still incomplete.
