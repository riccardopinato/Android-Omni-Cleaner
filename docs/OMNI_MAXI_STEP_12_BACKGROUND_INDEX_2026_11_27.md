# MAXI STEP 12 — Instant Scan & Background Indexing (2026-11-27)

Source: PR #77, layered on exact-head 9e22481. No merge before safety gates.

## Scope
- On resume, schedule unique WorkManager jobs. A 6-hour periodic request and a throttled one-off opening check run off the UI thread with battery-not-low/storage-not-low constraints.
- Inspect MediaStore version and generation for inexpensive multimedia invalidation. When unchanged and the last complete pass is under 24 hours old, skip filesystem traversal. A forced full audit at 24 hours detects non-media edits MediaStore does not report.
- When changed or stale, rebuild the existing read-only filesystem inventory and revision-checked SQLite index using the repository mutex; no files are removed, modified, or uploaded.
- Persist aggregate file count/bytes/timestamps only after a completed scan. Home and Storage Intelligence show the previous summary immediately, explicitly marked as potentially stale. Storage Intelligence can hydrate the persisted index without triggering a cold traversal; manual Analyze deliberately forces full refresh.
- Settings offer an on/off switch. Turning off cancels unique scheduled jobs. No boot-triggered dataSync foreground service.
- Preserve existing destructive path SHA/physical revision and Safe Trash validation. App-side invalidation marks the summary dirty.
- The opening scheduler runs on an IO dispatcher, and the one-time/periodic workers share a mutex so a first launch cannot trigger two simultaneous full traversals.
- Cancellation and MediaStore-generation races never publish a completed result. WorkManager may defer or stop work under Android background limits.

## Deliberate limits
- A changed media generation currently triggers a **background full reconciliation**; this is not yet a per-file MediaStore delta updater. We do not advertise a numerical speedup without matched physical samples.
- Non-media modifications from other apps may remain visible in the cached snapshot until the 24-hour audit or explicit refresh.
- Cached **counts** are not revalidated duplicate groups, freed-space estimates, or cleanup authorization. Duplicate hashing and cleanup remain explicit verified operations.
- The previous visual emulator video-motion gate remains unresolved independently of this step.

## Acceptance checks
1. Clean install first launch: UI opens instantly, work starts off main thread only when permissions/constraints permit, and no fabricated scan results.
2. Relaunch without changes: WorkManager records a quick check and does not request forceRefresh.
3. New media changes getGeneration: full background reconciliation occurs; no obsolete summary marked complete while scanning.
4. Non-media modifications: 24-hour full pass or manual Analyze discovers them even with unchanged MediaStore generation.
5. Permission revoked / low battery / background stop: no false completion; no automatic deletion.
6. Settings disable: periodic and one-off work canceled; previously saved summary remains labeled historical.
7. Existing tests: Android CI lint, release and debug builds, SQLite R1 and synthetic safety tests. Record physical median cold/warm runs on an Android device separately.
