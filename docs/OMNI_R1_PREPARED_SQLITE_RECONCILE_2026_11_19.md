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


## App Factory Python performance evidence gate — 19/11/2026

The reusable **Python Factory Performance QA v1** engine is now in
[App-Factory-Core@cac009697970490e45f9aaa9e7e7634016607273](https://github.com/riccardopinato/App-Factory-Core/commit/cac009697970490e45f9aaa9e7e7634016607273).
Use the **merged, immutable** commit for real-device R0/R1 evidence comparison; do not vendor Python into the Android APK.

Example once BOTH R0 and R1 have at least three same-library, same-Xiaomi, same-API, same-mode, same-count and same-integrity rows:

```bash
python3 reference_engines/performance_qa/factory_performance_qa.py compare \
  --baseline omni-r0-cold-reused.jsonl --candidate omni-r1-cold-reused.jsonl \
  --metric file_rows_ms --min-runs 3 --max-regression-pct 5 \
  --report omni-r1-sqlite-evidence.json
```

Each JSONL row requires `schema_version=1`, `fixture_id`, exact full `source_sha` (40 hex),
`device`, `environment=physical`, `api_level`, `mode=cold`, `file_count`,
`rows_reused`, `rows_replaced`, `integrity_digest=sha256:<64hex>`,
and `file_rows_ms`. These must be measured, **not filled with example values**.

- Current historical R0 provides just **one reused-row physical sample at 9,840 ms**. The Python comparator will intentionally refuse this input until additional matched R0 samples exist.
- New Core comparison protocol returns exit 2 for missing/mixed/incomparable evidence and exit 3 for a policy regression. Even a non-regressing result remains `NOT_CERTIFIED` without physical safety, thermal and OEM gates.
- Do not use 1,200-file emulator timings (116/66/52 ms) as a numerical baseline for the Xiaomi's ~88k-file physical scan.
- Always preserve the APK's signing identity and data; do not uninstall a populated app to run benchmarks.


## Machine-readable physical report conversion — controlled gate

New reusable local-only adapter:
`.github/scripts/omni-perf-report-to-jsonl.py` (unit tested by
`omni-r1-physical-evidence-contract.yml`).

It consumes real **copied** `OMNI_PERF_V1` reports and writes JSONL accepted
by App-Factory-Core's pinned Python comparator. Sample command with
**placeholders to replace by real measured values**:

```bash
python3 .github/scripts/omni-perf-report-to-jsonl.py \
  --input xiaomi-r1-1.txt --input xiaomi-r1-2.txt --input xiaomi-r1-3.txt \
  --source-sha FULL_40_HEX_SOURCE_COMMIT \
  --fixture-id VERIFIED_UNCHANGED_LIBRARY_ID \
  --integrity-sha256 VERIFIED_64_HEX_INVENTORY_SHA256 \
  --output xiaomi-r1-cold-reused.jsonl
```

**Never substitute the example ID or invent the integrity SHA.** It must be
computed independently over the same canonical file inventory/snapshot under
a documented protocol. If no trustworthy unchanged-library digest exists,
**STOP / evidence unavailable**, rather than derive it from a count or claim
the R0 and R1 datasets are identical. The tool rejects missing SQLite phase,
emulator reports, non-read-only operations, inconsistent counts, duplicated
run timestamps, mixed device metadata or reused/replaced rows, and refuses to
overwrite pre-existing output. Only it can normalize the fields, not certify
their physical provenance. The Core comparator still demands ≥3 matched
samples per candidate, a real same-device baseline and independent QA.
