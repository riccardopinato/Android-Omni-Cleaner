# Android Omni Cleaner — Performance Budgets

Status: **INSTRUMENTED / RUNTIME EVIDENCE DEFERRED**

These budgets make Step 9 measurable instead of relying on qualitative statements.

## Storage traversal budget

A shared `StorageInventoryRepository` is authoritative for read-only storage intelligence during a short interactive session.

Contract:
- in-memory cache TTL: **120,000 ms**;
- persistent warm-index reuse TTL: **6 hours maximum**;
- Overview, Large Files, Duplicates and Smart Categories reuse the same inventory when valid;
- warm reuse re-stats known files and checks MediaStore generation where available;
- cleanup completion marks both in-memory and persistent reuse stale immediately;
- manual full-index refresh is available;
- destructive actions never trust the cache/index alone and retain per-file live revalidation.

Acceptance target:
- **<= 1 full storage inventory traversal per 120 s interactive session** unless explicitly refreshed/invalidated;
- a normal warm app restart SHOULD reuse the persistent index rather than recursively walk the full tree when freshness checks pass;
- inventory traversal should perform at most one canonical-path resolution per visited directory in the normal path.

Recorded snapshot metrics:
- file count;
- unreadable directory count;
- excluded directory count;
- inventory elapsed milliseconds;
- cache hit/miss.

## Duplicate budget

Current optimizations:
- 64 KiB prefix/suffix candidate fingerprint;
- **256 KiB sequential IO buffer** for full SHA-256 and byte verification;
- optional extension filter applied before expensive duplicate stages;
- physical alias collapse before duplicate grouping;
- CPU-aware bounded hashing: **1–4 workers**, never task-per-file fanout;
- revision-bound persistent sample/full-hash reuse;
- throttled progress publication (not one UI event per file);
- correctness remains SHA-256 + final byte-for-byte verification.


- same-size grouping first;
- prefix + suffix fingerprint: **64 KiB per edge**;
- full SHA-256 only for sample collisions;
- byte-for-byte comparison remains the final equality proof;
- all long reads remain cancellable.

## Gallery memory budget

- page size: **120**;
- prefetch distance: **12**;
- undo history: **50**;
- reviewed-item compaction threshold: **240**;
- reviewed items retained after compaction: **60**.

## Runtime fixture

Deterministic Step 9 fixture: **1,253 files**.

The next executable FULL/physical performance evidence must record:
- cold inventory elapsed time;
- warm persistent-index latency;
- known-path restat count;
- duplicate-stage candidate counts;
- hash-cache hits/misses;
- bytes hashed;
- 1/2/4-worker elapsed time;
- workers peak/in-flight peak;
- peak memory / OOM outcome;
- cancellation response;
- process-death/restart outcome;
- thermal/battery note on low/mid device.

## Certification boundary

No wall-clock PASS threshold is fabricated before representative physical measurements exist. Measured baselines must be captured from the exact APK/device and then promoted into release budgets.

Current state:
- architecture and measurement hooks: implemented;
- exact-head runtime measurements: deferred;
- Store certification: not claimed.
