# MAXI STEP 9.6 — Persistent Indexing & Bounded Parallel Hashing

Status: **IMPLEMENTED — FAST EXACT-HEAD CI PASS / PHYSICAL PERFORMANCE EVIDENCE PENDING**

Version: `0.12.0-recovery` / code 12

## Goal

Reduce repeated storage traversal and duplicate-scan latency without weakening
the cleaner safety model.

This step pilots two App Factory Golden candidates:
- Incremental Persistent Indexing;
- Bounded Parallel Hashing.

App Factory candidate PR: `riccardopinato/App-Factory-Core#7`.

## Persistent storage index

The app now maintains an app-private SQLite index for read-only storage
intelligence.

Persisted per file:
- path;
- size;
- modified timestamp;
- extension;
- device/inode physical identity where available;
- high-resolution ctime revision on Android API 27+;
- sample hash;
- full SHA-256;
- scan generation / last verification.

Persisted global metadata:
- indexed root;
- last complete scan;
- scan generation;
- MediaStore generation when available;
- unreadable/excluded counters.

Warm-session behavior:
- in-memory 120 s cache remains the first layer;
- persisted snapshots may be reused across process/app restart for up to 6 h;
- every known persisted path is re-statted before reuse;
- MediaStore generation drift rejects persisted reuse when available;
- app-owned cleanup invalidates both in-memory and persistent reuse;
- user can explicitly trigger a full storage-index refresh.

Important boundary:
the persistent index is an accelerator, never destructive authority. Cleanup
continues to revalidate live files before mutation.

## Strong revision binding

`path + size + mtime` is not trusted as sufficient evidence for reusable
derived hashes.

On Android API 27+:
- physical identity: `deviceId + inode`;
- strong change revision: high-resolution `ctime`;
- persisted sample/full hashes are reused only when these still match.

On older Android versions:
- device/inode can still collapse physical aliases;
- persistent derived hash reuse is deliberately disabled when a trustworthy
  high-resolution revision token is unavailable.

## Duplicate pipeline

The duplicate path is now:

```text
physical-alias collapse
-> group by size
-> extension prefilter when selected
-> 64 KiB prefix/suffix sample
-> bounded sample workers
-> full SHA-256 only for sample collisions
-> bounded full-hash workers
-> deterministic grouping
-> byte-for-byte final verification
```

Small files whose sample covers the entire file can reuse that SHA-256 as the
full digest.

## Bounded concurrency

Worker count is CPU-aware and capped at 4.

Current policy:
- 1 CPU -> 1 worker;
- 2–5 CPUs -> 2 workers;
- 6–7 CPUs -> 3 workers;
- 8+ CPUs -> 4 workers.

The implementation launches only the bounded worker count and workers pull
indices from a shared atomic cursor. It does not create one coroutine/task per
file.

Progress remains throttled to avoid a UI update for every candidate.

## Alias safety

Hard links/provider-equivalent physical aliases with the same
`deviceId + inode` are collapsed before duplicate grouping.

This prevents the app from reporting two paths to the same physical object as
reclaimable duplicate bytes.

## Tests / evidence

Exact-head source evidence:
- Android CI run `38484281905`: **PASS**;
- resource parity: **305 keys** in EN/IT/ES/FR/PT;
- JVM unit tests: PASS;
- instrumentation sources compile in the PR lane;
- debug APK assembly: PASS;
- exact-head prerelease: `fast-apk-aa5f6e995123`.

Regression coverage includes:
- duplicate correctness;
- sample collision followed by full verification;
- extension filtering before hashing;
- bounded worker policy;
- no task-per-file producer fanout;
- physical alias collapse;
- revision-bound hash-cache reuse;
- persistent-index restart/hash invalidation instrumentation source.

## Remaining evidence

Not yet claimed:
- Android instrumentation execution on a real device;
- cold-vs-warm timing on a representative large real library;
- 1/2/4-worker benchmark on low/mid/high devices;
- thermal/battery impact;
- physical validation of new/external file discovery during the persistence TTL;
- production/store certification.

Therefore this step is implemented and build-verified, but not yet a
Factory-CERTIFIED performance pattern.
