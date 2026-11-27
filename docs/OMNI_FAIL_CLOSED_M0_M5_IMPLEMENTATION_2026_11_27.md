# Android Omni Cleaner — fail-closed M0–M5 integration checkpoint
**Date:** 2026-11-27
**Branch / PR:** `integration/omni-v11-colors-gallery-qa-20261121` / #77 (DRAFT)
**Factory baseline:** `riccardopinato/App-Factory-Core@9bec235b319963bc05f3c3c4d95d0f6b267bb309` — CURRENT Master v27, Golden Index v31, Registry v2.
**Scope:** M0–M5 ONLY. **M6 (APK delivery, signed release, physical OEM device QA) excluded.** Never merge or release solely because this document exists.
**Input:** `Audit_Android_Omni_Cleaner_Fail_Closed_2026-11-27.md`, audit of original `46163207d8f5652ec4131b2e86516e176d5be637`.

## M0 — CI and prior failing tests

- Fixed mismatched restore policy for verified `Android/media/<app>/.thumbnails` cache only, without opening ordinary `Android/media` paths.
- Invalid legacy SHA metadata is quarantined: source retained and no new valid-looking record published.
- The functional test script expects **2** MediaStore readback tests, not 1.
- Visual Maestro uses the stable `omni_gallery_filter_videos` test tag, not translated text.
- An unrelated Gradle plugin-resolution failure was observed on a cold hosted runner; Kotlin Compose plugin `2.4.10` is published, so do not silently change toolchains to hide network errors. Retry only with evidence and report exact run outcomes.
- The **final exact-head test status must be read from GitHub Actions**. Previous FAIL/PASS belong to a different commit and may not be transferred.

## M1 — Destructive routes and hard guards

- Removed non-atomic cross-volume `copy -> File.delete(original)` fallback; unsupported cross-volume move refuses without unlinking original.
- Removed unsafe rollback `payload.renameTo(original)` (possible TOCTOU overwrite). Retain payload+metadata for manual recovery.
- `SafeTrashManager.purgeAll()`, `emptyTrash()` and `permanentlyDelete(file)` now deny; raw `purgeEntry(id)` requires an exact internally reviewed record.
- `SafeTrashManager.planPurge(reviewed)` freezes entries, paths, SHA and size with a fingerprint; late-arriving or changed entries make bulk purge abort before mutation.
- Purge UI requires the original reviewed set plus two separate confirmations; unknown/corrupt entries stop bulk approval.
- Dedicated `.github/scripts/check-omni-destructive-policies.py` enforces no recursive app-source deletion, direct MediaStore delete, dynamic purge from UI, or direct thumbnail unlink. Wired to Android CI.
- Verified thumbnail-cache cleanup exclusively uses recoverable Safe Trash. Root and scanned SHA validated.
- `SafeTrashManager(context)` rechecks authorized broad access before each data mutation; testing constructor uses synthetic-scoped roots.

## M2 — Safe Operations Core and duplicate survivor invariants

- New `cleaner/SafeOperationsCore.kt`: `SafeOperationsJournal` and `DeletionCoordinator`.
- Local selections: live SHA/size/mtime/symlink/path revalidation, reviewed target manifest digest, duplicate keeper check, one coordinator lock.
- A file that is required as a keeper **cannot also appear in the mutation set**, even through a different category.
- Local move and empty-folder `rmdir` operations go through the coordinator; thumbnail recovery and restore/purge perform their own staged journal writes.
- All actions are explicit user selections, not triggered by scanning or WorkManager.
- Weakness: external writers on shared storage cannot be cryptographically locked by a precomputed SHA. Filesystems without a supported safe move remain unsupported, not force-deleted.

## M3 — MediaStore approval, volume version and restart

- MediaStore callbacks retain per-URI read-after-write evidence `IS_TRASHED` (three bounded reads). `RESULT_OK` is user consent only.
- `StorageCleanupEngine` journals the exact URI, size, name and scanned SHA before OS consent; `GalleryController` also journals its selection.
- Read-only process-resume reconciliation inspects old `MEDIA_TRASH` records. **It never issues another delete/trash request.**
- `GalleryMediaItem` now captures URI, `DATE_MODIFIED`, `GENERATION_MODIFIED`, physical `VOLUME_NAME`, and the opaque `MediaStore.getVersion(context, volume)`. Any missing/changed volume epoch blocks the stale selection and requires rescan.
- `StorageCleanupEngine.prepareSnapshots` ties normal media requests to a source SHA; pre-consent checks include current source snapshot and provider URI/name/size/Trash state.
- Partial/denied/unknown OS results do not count as success, do not claim reclaimed disk bytes, and must leave unverified selections open.
- No arbitrary SAF `deleteDocument` or `MediaStore.createDeleteRequest` is added.
- Android system media Trash may expire according to provider policy, often ~30 days; UI discloses this and does not promise permanent recovery.

## M4 — Safe Trash, explicit legacy recovery, reversible restore

- `readEntry` is pure: it does not rewrite `original.properties`. Invalid or missing SHA is not a valid entry.
- Legacy private Trash import is explicit from UI and journaled; wrong original SHA is rejected before copy. Migration staging keeps original until new payload and metadata pass verification.
- Separate manual, journaled `repairLegacyEntryApproved(id)` for old metadata with **no** stored checksum; a wrong existing checksum is never overwritten. Backup and staging files remain for recovery if interrupted.
- `restore` accepts only reviewed thumbnail subtrees under `DCIM/.thumbnails`, `Pictures/.thumbnails`, `Android/media/<app>/.thumbnails`, with symlink/root guards.
- No-overwrite destination uses `Os.link`, else `O_CREAT | O_EXCL | O_NOFOLLOW` on Android API24+; source Trash copy is preserved on ambiguous failure.
- Before a file moves/restores/purges, a durable operation event is written; corrupt/unexpected journal files prevent new mutations. Unknown states are visible for review and never auto-replayed.
- Explicit single/bulk purge records are written before any unlink. Explicit purge may be irreversible by definition; nothing discovered by background scanning may trigger it.

## M5 — Synthetic verification gates

Added or updated:
- `SafeOperationsCoreTest`: restart after journal phase, duplicate keeper also targeted, source same-size changed bytes, corrupt journal, successful reversible move and restore.
- `SafeTrashManagerTest`: exact-approved purge, late-arriving unreviewed item cannot be purged, payload tamper, legacy quarantine/migration, no raw-ID purge.
- `ThumbnailFileCleanupTest`: verified JPG moved to recoverable Trash, ordinary Android/media preserved, unsupported folder stays untouched.
- `MediaTrashReadbackInstrumentedTest`: 2 synthetic provider tests, readback true/false/null and URI unavailable.
- `SafeOperationsJournalInstrumentedTest`: synthetic API independent process-phase inspection and denied/unknown media; never real user data.
- `.github/workflows/omni-safe-ops-api-matrix.yml`: bounded parallel emulator fixture matrix API **24, 29, 30, 33, 35, 36**, with per-class count assertions and exact-source checkout.
- `.github/scripts/run-omni-functional-matrix.sh` expects both MediaStore tests.
- `.github/scripts/capture-omni-screenshots.sh` tests stable video tab identifier.

**CRITICAL:** Test sources present and a run in progress are NOT evidence of PASS. This document **does not claim full JVM/CI/Android matrix completion**. Read exact-commit run results and logs before changing any checklist item to verified.

## Residual limitations and no-go rules

- Zero-loss is not a mathematical guarantee against malicious concurrent writers, hardware/media failure, Android OS provider purge or external cleaners. Fail closed where an atomic safe mutation cannot be proven.
- No app may auto-trigger permanent purge. Unknown/partial URI, unreadable file, missing permission or SHA mismatch always blocks the affected action and must never increase reclaimed-space counters.
- The journal is an append-only fsynced evidence log, not a fully database-backed WAL; an interrupted staging entry blocks additional mutation until explicit review. Directory fsync and multi-process file locking require more runtime evidence.
- New synthetic test suites, especially API24/29/30/33/35/36, may fail due hosted emulator infrastructure; a failure remains a blocker until classified, not an implied success.
- Android CI and integration tests must all be PASS at the **same final source SHA**, with no P0 safety findings, before integration promotion.
- M6 physical-device verification, signed APK, upgrade-in-place, Play/Billing/ad providers are outside this run and intentionally remain blocked.
- CI debug/androidTest APK **compilation** is allowed as a test requirement; **automatic APK publication for PR #77 is disabled**.

**Decision:** PR #77 remains Draft; no merge to `main`, no final APK, no signed release. A source change can only be called IMPLEMENTED, not CERTIFIED, until the evidence ladder proves it.
