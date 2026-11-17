# Omni Cleaner — 5 maxi-step parallel implementation, evidence register
Date: 2026-11-17
App: riccardopinato/Android-Omni-Cleaner
Baseline: App-Factory-Core@c5adc147046beb7024629466d10eeb799c3c2619 (Master 27, Index 29)
Target: feature/omni-scan-engine-2-0-20261117 → draft PR #27 → integration branch
Main RC1 frozen; no auto-merge, signing or Play release.

## Step 1 — Trash reliability / data safety
- Source: SafeTrashManager inspect() returns one bounded directory enumeration, valid entries, physical payload bytes including unknown entries, unknown count and list readability.
- UI exposes unknown/partial entries as a warning instead of pretending the trash is empty.
- Unknown/corrupt entries are **never automatically purged**. The safe next path is recovery/manual inspection; damaged payload data is not discarded to make an empty state green.
- JVM regression for valid+orphan+unknown inventory, plus existing 50-MiB APK move/restore/purge and interrupted migration tests.
- Runtime: synthetic 50-MiB app-owned Android fixture for move → restore → purge. Physical shared-storage validation outstanding.

## Step 2 — Gallery, MediaStore and batch limits
- MediaStore.createTrashRequest capped at 2,000 URI. No request can exceed this platform limit.
- Gallery submits only first 2,000 selected IDs to the system confirmation and preserves the remainder for further review/approval.
- Normal and Pro cleanup preparation caps URI collection; overflow candidates remain untouched and reported as blocked (not silently claimed as processed).
- JVM policy test for 2,501 selected entries.
- Pending: true user-device system Trash confirmation, gallery permission transitions, retry/restart and URI MediaStore reconciliation under OEM providers.

## Step 3 — Scan Engine 2.1
- Candidate groups ordered by highest potential duplicate space to get large verified previews sooner, without dropping candidates.
- Worker count bounded at 1–4 even if a caller requests more.
- Cleanup preparation skips duplicate full SHA validation when already validated in same loop; outer move path relies on SafeTrashManager immediate validation before mutation rather than repeating it.
- JVM test for large groups first. This is I/O reduction, NOT a measured speedup until physical cold/warm benchmarks.

## Step 4 — UI & testing
- Safe Trash exposes and refreshes partial/corrupt states.
- AppLab runtime job executes connectedDebugAndroidTest for this feature branch.
- Failed visual journeys print their full Markdown report and bounded JSON detail into GitHub Actions logs to diagnose the previously observed failure without concealing it.
- Outstanding: inspect fresh exact-head UI screenshots (10+ screens), fix any concrete high-confidence issue; small screens/dark/TalkBack/physical UI require independent validation.

## Step 5 — release readiness
- Feature CI now runs lintDebug, lintRelease, JVM tests, debug APK, androidTest APK, assembleRelease, bundleRelease on same immutable source SHA.
- The generated debug APK may be shared for testing only. Production APK/AAB signing identity, release cert pin, Play Internal Testing, real Billing/AdMob/UMP, declaration and physical device evidence are mandatory external gates.
- No production signing material or provider credentials are generated, copied, or requested in chat.

## Acceptance matrix (must be run on the exact final APK)
| Scenario | Evidence scope | Current status |
| --- | --- | --- |
| 2 pairs of ~50MB identical APK: keep 1 each, move 2 into private Safe Trash | real device shared storage | NOT VERIFIED |
| 50MB Safe Trash item → restore → purge | Android synthetic test | RUN REQUIRED |
| Interrupted move / space full / permission revoked, no data loss | Android OEM physical | NOT VERIFIED |
| 2501 gallery media picks: two independent Android dialogs, none silently dropped | real MediaStore | NOT VERIFIED |
| MediaStore cancel, partial permissions and queue persistence after force-stop | Android OEM | NOT VERIFIED |
| Repeated cold/warm scans, duplicate SHA correctness, worker peak 1–4 | Android benchmark | NOT VERIFIED |
| UI Home/Clean/Gallery/Privacy/Pro and detailed screens incl. responsive | Emulator screenshot + physical | NOT VERIFIED |
| Exact-head debug/release lint/build/test pipeline | GitHub Actions | RUN REQUIRED |
| Signed AAB, Play entitlement/Ads/UMP, privacy + storage declarations | provider/Play | BLOCKED EXTERNAL |

The documentation records implementation and evidence separately; never promote without an observed gate.
