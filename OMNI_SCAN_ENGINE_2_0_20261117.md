# Omni Scan Engine 2.0 — Batch Hash I/O + actionable progress

Status: IMPLEMENTED IN FEATURE BRANCH; source/CI/AppLab/physical gates still required.
Parent: `integration/omni-v1-1-software-verified-20261116` (not frozen `main`).

## Why
A large file library can make "Find duplicates" look frozen. Step 9.6 already provides a shared SQLite inventory, 64-KiB edge samples, bounded 1–4 hash workers, and full SHA-256/byte verification. Scan 2.0 avoids repeated per-file SQLite read roundtrips and per-file autocommit writes.

## Changes
- **Single-use per-scan cache sessions**: explicitly prepare eligible same-size candidate hashes from SQLite with 250-path bounded IN batches. Check exact path, size, mtime, device/inode, and high-resolution ctime; no cache reuse without strong revision.
- **64-write transaction batches**: sample/full SHA-256 writes persist with bounded transactions. The final pending batch is flushed even on cooperative cancellation; on abrupt process death, at most the pending batch can be lost. The next scan recomputes missing hashes.
- **No schema migration and no new dependency**: existing private SQLite index stays authoritative for *accelerating reads only*, not for cleanup decisions.
- **UI**: Duplicates screen shows candidate sample progress, and a separate indeterminate verification stage after preliminary samples complete. Progress does not claim scan completion while final byte comparisons are running.
- **Correctness preserved**: original alias collapse, SHA-256, final byte-for-byte comparison, keeper checking and deletion revalidation remain untouched. One scan session does not share uncommitted hashes with a cancelled session.

## Limits
- This change is not a semantic/AI search model.
- It does not implement a durable cursor checkpoint or interruption-resume of the entire scan; already committed hashes are the bounded reusable work.
- The full filesystem inventory can still be cold when the directory fingerprint or MediaStore generation changed; 120-second RAM cache, 6-hour indexed warm reuse and manual refresh are unchanged.
- Physical speedup is **not measured yet**. Do not claim this fixes every long-running scan until exact-APK device benchmarks exist.

## Acceptance
- [ ] Exact-head Android CI + JVM tests + localization parity PASS.
- [ ] AppLab Duplicates route and cancel/restart flow PASS.
- [ ] Instrumentation test for session persistence and revision invalidation executed.
- [ ] Physical run on large library: cold vs warm; candidate count, sample/full hash elapsed time, cache hits, peak workers, battery/thermal.
- [ ] Physical no-data-loss review + deletion/restore testing; production signing/Play remain separate gates.
