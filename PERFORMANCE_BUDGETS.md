# Android Omni Cleaner — Performance Budgets

Status: **INSTRUMENTED / RUNTIME EVIDENCE DEFERRED**

These budgets make Step 9 measurable instead of relying on qualitative statements.

## Storage traversal budget

A shared `StorageInventoryRepository` is authoritative for read-only storage intelligence during a short interactive session.

Contract:
- cache TTL: **120,000 ms**;
- Overview, Large Files, Duplicates and Smart Categories reuse the same inventory when the root and TTL match;
- cleanup completion invalidates the cache immediately;
- destructive actions never trust the cache alone and retain per-file snapshot/hash revalidation.

Acceptance target:
- **<= 1 full storage inventory traversal per 120 s interactive session** unless explicitly refreshed/invalidated;
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
- cache-hit latency;
- duplicate-stage candidate counts;
- peak memory / OOM outcome;
- cancellation response;
- process-death/restart outcome.

## Certification boundary

No wall-clock PASS threshold is fabricated while GitHub runners are unavailable. Measured baselines must be captured from the exact APK/device and then promoted into release budgets.

Current state:
- architecture and measurement hooks: implemented;
- exact-head runtime measurements: deferred;
- Store certification: not claimed.
