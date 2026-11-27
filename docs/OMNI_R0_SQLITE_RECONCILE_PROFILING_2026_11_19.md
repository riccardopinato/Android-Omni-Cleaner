# Omni Cleaner — R0 profiling scanner versus SQLite (2026-11-19)

Status: **SOURCE EXPERIMENT / NO DEVICE TIMINGS YET**. This is a read-only performance diagnostic; not a Rust migration, production release, or evidence that 18 seconds is all SQLite.

## Background
Step 9.7 physical evidence on Xiaomi M2101K6G, 87,136 files:
- cold total 61,064 ms; scanner-only 43,167 ms
- difference 17,897 ms **includes reconciliation and other overhead**, not necessarily SQL alone
- persistent warm 2,599 ms; hot RAM 1 ms.
This was measured on a prior APK; it is **not current-main performance**.

## Implementation
The existing debug-only Performance Lab now collects optional phase timings during a **forced cold scan**:
- `cold_scan_ms`: full request, includes scanner, SQLite and overhead
- `cold_scanner_only_ms`: recursive filesystem traversal (existing field)
- `cold_index_reconcile_ms`: entire SQLite reconciliation and index marker
- `cold_index_file_rows_ms`: per-file SQLite update/upsert pass
- `cold_index_directory_rows_ms`: per-directory SQLite writes
- `cold_index_finalize_ms`: prune old rows and metadata writes before commit
- `cold_index_commit_ms`: transaction commit and clearing dirty flag
- `cold_index_rows_reused`: file revisions matched
- `cold_index_rows_replaced`: newly inserted or changed file revisions
- `cold_index_directories_written`: directory entries written
- `cold_other_ms`: difference not attributed to scanner or reconciliation; includes setup, lock, scheduling, rounding. **It is not assumed to be SQLite.**
Keys are additive to `OMNI_PERF_V1` and optional in other consumers. No paths, filenames, source content or personal media are added to the report.

Only the native SQLite index in the **debug** benchmark emits these counters; no SQL schema, deletion, provider/permission, persistence or signing changes are intended.

## Procedure on physical Android
1. Install the exact-head **debug** APK from the PR Actions/release; debug applicationId has `.debug` suffix, so it **does not update official release**.
2. Open **Clean → Advanced → Advanced maintenance → Performance Lab**. Allow Deep Cleaning only if you are comfortable granting that privilege to the diagnostic test app and if it is already part of your existing testing; do not turn on full access casually.
3. Ensure device is cool and battery comfortably charged. Avoid downloads or new file creation while testing; the experiment reads local files and writes **only its own SQLite index**.
4. Tap **Run performance benchmark** and then **Copy report**. Run once, then twice more if practical under comparable conditions (the forced cold scan intentionally repeats work). Share only the `OMNI_PERF_V1` diagnostics; inspect any device identifiers before sharing.
5. The report includes both a warm index hit flag and file-count delta. If warm reuse is a miss or counts change, assess correctness before speeding up the index.
6. Compare medians of three runs, scanner/reconcile share, `rows_reused` versus `rows_replaced`, thermal and elapsed time. Repeat on a second lower-end Android device before generalizing.

## Decision gates
- If `cold_index_file_rows_ms` dominates and reused rows are abundant, prototype parameterized/prepared SQL statement reuse or batch upsert (keep schema and strong revision protections intact).
- If `cold_index_directory_rows_ms` dominates, optimize the directory write pass and avoid unnecessary unchanged rows only with identical fingerprint correctness.
- If `cold_scanner_only_ms` dominates, profile `File.listFiles`, `Os.lstat`, canonical paths and heap/GC; **do not start Rust before native workload profiling**.
- If `cold_other_ms` is material, profile scheduler/locking/initialization rather than attributing it to SQLite.
- Compare on the same ABI/device/files/conditions with no regression in cancellation, crash consistency, cold/warm file-count, hash revision binding, Safe Trash, or deletion revalidation.
- **R1 optimization begins only after R0 runtime evidence**, with a separate benchmarked PR. This branch contains instrumentation only.

Factory: Core v27 / Index v29, baseline `63c3ecba07dafdf46e27e175bd973ee05107fecb`; relevant Golden Incremental Persistent Indexing and Bounded Parallel Hashing still DRAFT. App source baseline `3b652d57c433d23025869b8daea7e7998e9003a2`.
