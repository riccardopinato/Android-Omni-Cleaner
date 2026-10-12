# MAXI STEP 9 — Performance & Deep QA

Factory baseline: `fa3c0679de393f1cb13c2fed2be314d1a1269353`  
App baseline entering Step 9: `379cbae5e332bd44ce5f78833c61a56da3faa923`

## Goal

Harden Android Omni Cleaner for large libraries, cancellation, low-storage conditions,
process death, background/restart behavior and lower-end devices without weakening
the storage safety model.

## Performance changes

### Duplicate scan
- same-size files receive a 64 KiB prefix + 64 KiB suffix fingerprint first;
- only sample collisions advance to full SHA-256;
- full SHA-256 remains mandatory before duplicate grouping;
- byte-for-byte verification remains mandatory after SHA-256;
- full hashing now checks coroutine cancellation between chunks.

The sampled fingerprint is only a negative prefilter. It can never prove that files
are duplicates.

### Gallery
- MediaStore CancellationSignal is wired to coroutine cancellation;
- changing filter/sort cancels stale in-flight queries;
- paging uses a source offset independent from the bounded in-memory review window;
- reviewed KEEP items are compacted from memory after the bounded window;
- selected-trash items survive compaction separately;
- undo history is bounded to the most recent 50 actions.

### EXIF
- source digest is computed while copying to the temporary file;
- the source URI is independently re-opened and re-hashed after the copy to detect provider truncation/source mutation;
- the temporary file is independently hashed before EXIF mutation;
- cleaned digest is computed while writing the exported copy;
- the provider output is still re-opened and hashed after writing;
- cancellation is propagated rather than converted into a failed item;
- temporary-file creation failure is contained per item for low-storage resilience.

### Thumbnail cleanup
- per-file SHA-256 now checks cancellation between chunks;
- post-clean empty-directory traversal also checks cancellation.

## Deep-QA runtime dataset

The Step 9 runtime lane adds a deterministic synthetic library under
`/sdcard/Download/OmniPerf` with:
- 1,200 small inventory files;
- 48 same-size but different files;
- one three-file real duplicate group;
- archive and installer category candidates.

The fixture is intentionally modest in bytes but high enough in file count and
same-size candidates to exercise inventory, duplicate prefiltering and UI scans.

## CI / AppLab gate hardening

- feature work runs one PR gate instead of duplicate push + PR lanes;
- PR workflows check out `github.event.pull_request.head.sha`, not the synthetic merge ref;
- source-branch detection uses `github.head_ref` / explicit `OMNI_SOURCE_BRANCH`;
- Step 9 therefore selects FULL on pull-request runs as well as manual branch runs;
- GitHub Actions quota/budget exhaustion is classified as BLOCKED when a job terminates before runner assignment.

## AppLab Step 9 mode

Step 9 switches the pinned AppLab run from FAST smoke planning to FULL analysis and
explicitly enables:
- System UI;
- Performance;
- Persistence & Restart;
- Configuration & Lifecycle;
- Resource Pressure & Process Death;
- Background Execution / Doze / Recovery;
- Storage & Data Integrity.

Network and Upgrade remain outside this step.

## Version

- `0.10.0-recovery`
- versionCode `10`

## Exit gate

Step 9 is COMPLETE only when the exact-head artifact has:
1. focused JVM tests PASS, including duplicate sample-collision and gallery-window tests;
2. Android CI PASS (lint, tests, debug and release builds);
3. AppLab Intelligence PASS;
4. AppLab FULL runtime PASS;
5. retained parity journey PASS on the large-library fixture;
6. localization/accessibility journey PASS;
7. Performance / lifecycle / resource-pressure / background / storage labs executed with no blocking error;
8. runtime failures = 0 and no app crash / ANR / OOM evidence;
9. exact-head Step 9 APK prerelease + SHA-256 sidecar.

Physical low-end-device and OEM behavior remains a Step 10 certification requirement.

Current validation state: **BLOCKED** until GitHub-hosted runners can be assigned again. No pre-run quota/budget rejection may be reported as a test failure or PASS.
