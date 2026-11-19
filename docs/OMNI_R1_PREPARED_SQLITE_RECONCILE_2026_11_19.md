# Omni Cleaner — R1 candidate: reusable SQLite UPDATE

**State:** experimental source change; no runtime speedup or release certification claimed.
**Dependencies:** R0 measurements/instrumentation PR #38 (`e02447d1e8279ef6884aedd39b9ed94b1f78d774`). This PR includes R0 source until #38 is merged.

## Real device R0 evidence (2026-11-19)
Xiaomi M2101K6G / Android API 33 / same debug APK, `OMNI_PERF_V1`:

| | First benchmark | Same-APK second benchmark |
|---|---:|---:|
| Files | 88,351 | 88,352 |
| Cold wall time | 56,803 ms | 104,699 ms |
| Filesystem scan only | 42,333 ms | 94,497 ms |
| SQLite reconcile | 14,466 ms | 10,197 ms |
| SQLite file rows | 14,005 ms | 9,840 ms |
| Reused rows | 0 | 88,351 |
| Replaced rows | 88,351 | 1 |
| Warm persistent HIT | 1,458 ms | 1,546 ms |
| Warm count delta | 0 | 0 |

**Findings:** persistent index revision binding and reuse work. Per-file SQLite loop consumed 9,840 ms even when 88,351 records were reused. Cold filesystem scan showed high variability unrelated to the measured SQLite work. Do not misinterpret second-run 67.72× cold/warm ratio as an app improvement: cold scan got much slower.

## Source change
Previously `PersistentStorageIndex.reconcileFull` built `ContentValues`, revision strings and `db.update` for every candidate unchanged file. It now:
- compiles a single revision-qualified `UPDATE indexed_files ... WHERE path/size/mtime/dev/inode/ctime` statement per transaction
- rebinds typed parameters per file and calls `executeUpdateDelete()`
- leaves new/changed entries on the same `CONFLICT_REPLACE` path, clearing their hashes
- preserves strong revision checks, single transaction, cancellation check every 128 entries, deterministic inventory generation and index invalidation
- closes the statement before finalizing the transaction

No schema version or persisted-data migration, permissions, delete rules, Safe Trash, UI or build-signing changes.

## Validation
- JVM/Android CI exact-head compile/lint/unit and debug APK required.
- Android instrumented fixture tests unchanged revisions retain a hash, changed revisions invalidate it, and new files still appear in index; these tests must be **executed on Android** before final certification, not just compiled.
- Physical experiment: attempt to update R0 debug APK **without clearing app data**, but **do not assume GitHub Actions debug APKs share the same signing certificate**. If Android reports a signature mismatch, **do not uninstall the old debug APK automatically**: it may contain app-private diagnostic state or Safe Trash. First preserve any important content and make a conscious migration/reset decision. A separate install/cert provisioning may be needed; no production app uninstall is ever required for this experiment.
- In any case, the **first R1 run may be a fresh-index initialization**. Run at least one complete warm-up scan and then **2–3 additional forced cold benchmarks** on the same APK with unchanged file library. Only compare reused-row passes where `cold_index_rows_reused` is close to total files, warm count delta is 0 and no unexpected cache invalidation occurred.
- Compare median `cold_index_file_rows_ms` to the R0 9,840 ms reused-row sample, acknowledging that R0 has only one comparable run. Collect more matched R0/R1 physical samples before robust percentage claims.
- Keep record of CPU scheduling, battery temperature, available battery, heavy background activity and scanner-only variance. Compare change in SQLite phase, **not just total cold time**.
- If R1 fails quality/safety or shows negligible speedup, close without merge.

## Safety
- Debug benchmark remains read-only for user media, and updates only the app-private SQLite index.
- No external data, media paths or content should be added to telemetry or reports.
- Keep RC1 release freeze; do not merge into main automatically.
- Rust is not included; this is Kotlin/SQLite optimization.

Factory baseline for this work: App-Factory-Core `63c3ecba07dafdf46e27e175bd973ee05107fecb` Master v27, Golden Index 29; relevant indexing/hashing Goldens are still draft.
