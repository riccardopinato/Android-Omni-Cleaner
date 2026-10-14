# Android Omni Cleaner — Performance Budgets

Status: **IN-APP PHYSICAL HARNESS IMPLEMENTED / DEVICE EVIDENCE PENDING**

These budgets make Step 9 measurable instead of relying on qualitative statements.

## Storage traversal budget

A shared `StorageInventoryRepository` is authoritative for read-only storage intelligence during a short interactive session.

Contract:
- in-memory cache TTL: **120,000 ms**;
- persistent warm-index reuse TTL: **6 hours maximum**;
- Overview, Large Files, Duplicates and Smart Categories reuse the same inventory when valid;
- warm reuse re-stats known files, verifies persisted directory fingerprints and checks MediaStore generation where available;
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


## Step 9.7 on-device benchmark

The production code now contains a read-only Performance Lab.

One run captures:
- cold wall-clock storage traversal + index reconciliation;
- scanner-only cold traversal time;
- warm persistent-index request after memory-cache eviction;
- hot in-memory request;
- total file count;
- deterministic real-file hash sample;
- 1/2/4-worker elapsed time and MiB/s;
- peak in-flight worker count;
- device/SDK/CPU-thread metadata;
- battery %, battery temperature and Android thermal status when available;
- observed app heap across benchmark phase boundaries.

Hash sample bounds:
- max 200 files;
- max 128 MiB total;
- each file 4 KiB–32 MiB;
- persistent hash cache bypassed for worker comparison fairness.

Decision rules:
- do not assume 4 workers is best;
- prefer the lowest worker count whose throughput is effectively tied with the best result when it reduces heat;
- investigate any warm persistent MISS or unexpected cold/warm file-count delta;
- peak in-flight MUST NOT exceed configured workers.

No benchmark result is accepted until copied from an exact APK running on a physical device.
