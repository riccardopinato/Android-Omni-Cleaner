# MAXI STEP 9.7 — Physical Performance Validation

Status: **PHYSICAL PERFORMANCE VALIDATED — PRIMARY DEVICE PASS / SECOND-DEVICE GOLDEN PROMOTION PENDING**

Version: `0.13.0-recovery` / code 13

Factory baseline:
- App-Factory-Core `03406ef5e511dfc580b67967c67a11ab58e20141`;
- Master v26.3;
- Golden Index v15;
- relevant DRAFT Goldens:
  - Incremental Persistent Indexing v1;
  - Bounded Parallel Hashing v1.

## Goal

Turn the Step 9.6 performance work into measurable device evidence instead of
qualitative impressions.

The benchmark is deliberately read-only and never authorizes cleanup.

## In-app Performance Lab

The Clean workspace now exposes **Performance Lab**.

One run measures:

1. **cold inventory**
   - forces a complete storage traversal;
   - includes persistent-index reconciliation wall time;
   - records the scanner-only elapsed time separately.

2. **warm persistent inventory**
   - drops only the in-memory cache;
   - preserves the persistent SQLite index;
   - re-runs the inventory request;
   - records whether the result was a real persistent-index HIT.

3. **hot in-memory inventory**
   - immediately requests the same snapshot again;
   - measures the short-session memory-cache path.

4. **bounded hashing benchmark**
   - chooses a deterministic, bounded real-file sample;
   - performs one untimed-for-worker-choice serial pre-warm pass and records its elapsed I/O cost separately;
   - compares 1 / 2 / 4 workers only after that pre-warm, reducing page-cache order bias;
   - individual file size: 4 KiB–32 MiB;
   - at most 200 files;
   - at most 128 MiB total;
   - hashes the same sample with 1, 2 and 4 workers;
   - records elapsed time, bytes, throughput and peak in-flight workers;
   - does not use the persistent hash cache so worker comparisons measure actual I/O/hash work.

5. **device state**
   - manufacturer/model;
   - Android SDK;
   - reported CPU thread count;
   - battery percentage before/after when available;
   - battery temperature before/after when available;
   - Android thermal status before/after when available;
   - observed app heap high-water sample across benchmark phases.

## Evidence format

The app produces a copyable machine-readable report beginning with:

```text
OMNI_PERF_V1
```

Key fields include:

```text
cold_scan_ms
cold_scanner_only_ms
warm_scan_ms
warm_persistent_hit
warm_speedup_x
hot_memory_scan_ms
hot_memory_hit
hash_prewarm_ms
hash_prewarm_bytes
hash_1_workers_ms
hash_2_workers_ms
hash_4_workers_ms
hash_*_workers_mib_s
hash_*_workers_peak_inflight
battery_*_pct
battery_temp_*_c
thermal_*
observed_heap_max_bytes
```

Paste the report into the project chat for analysis.

## Physical protocol

For a useful comparison:

- battery preferably >= 40%;
- avoid active downloads/copies during the run;
- let the phone return near normal temperature before the first run;
- grant the same Deep Cleaning access used in normal product testing;
- run the benchmark once;
- copy the report;
- wait for the device to cool if it became warm;
- repeat only if the first result is anomalous.

For Golden promotion evidence:
- at least one representative mid/high Android device;
- at least one lower-end/older Android device;
- compare 1/2/4-worker throughput rather than assuming 4 workers are best.

## Interpretation rules

No fabricated universal millisecond threshold is used.

Primary decisions:

- if warm persistent reuse is not materially faster than cold traversal, revisit the index design;
- if 4 workers are slower than 2 by a meaningful margin on representative devices, reduce the adaptive cap;
- if battery temperature/thermal state rises materially during the bounded sample, prefer fewer workers;
- if warm file count unexpectedly diverges from cold count, investigate stale-index discovery before certification;
- if peak in-flight exceeds configured workers, fail the bounded-concurrency contract.

## Current boundary

Implemented:
- benchmark harness;
- report generation;
- UI trigger and copy action;
- bounded real-file sample;
- non-destructive execution.

Validated on the exact Step 9.7 development APK:
- commit: `436e884886dbc7e034547b3a7b7299a9cd649f7b`;
- CI: Android CI #370 — PASS;
- device: Xiaomi M2101K6G / Android SDK 33 / 8 reported processors;
- real library: 87,136 files;
- cold inventory: 61,064 ms (scanner-only 43,167 ms);
- warm persistent inventory: 2,599 ms, persistent HIT, 23.50x faster than cold;
- warm file-count delta: 0;
- hot in-memory inventory: 1 ms, explicit memory HIT;
- SHA-256 sample: 128 MiB across 5 large real files;
- 1 worker: 573.99 MiB/s;
- 2 workers: 920.86 MiB/s;
- 4 workers: 1,267.33 MiB/s, peak in-flight = 4;
- battery: 7% -> 6%;
- battery temperature: 35.3 C -> 37.2 C;
- Android thermal status: 0 -> 0;
- observed heap high-water sample: 82,121,544 bytes;
- emulator signals: false;
- destructive actions: false.

Measured optimization loop:
- the first physical run exposed a warm-path regression: 240,050 ms warm and
  244,511 ms nominal hot-memory time on 87,131 files;
- root cause 1: a persisted snapshot installed in RAM inherited the old
  persistent-index timestamp, so the 120 s RAM TTL could already be expired
  immediately after a slow warm validation;
- root cause 2: warm reuse re-statted every persisted file, making warm reuse
  slower than a full traversal on this device;
- fixes start the RAM TTL when the snapshot is hydrated and validate directory
  freshness before hydrating persisted file metadata directly from SQLite;
- destructive/hash-sensitive paths still revalidate live file revision before
  acting, preserving the safety boundary.

Primary-device verdict: **PASS**.

The measured 8-processor device supports the existing 4-worker cap: 4 workers
delivered the highest throughput with no thermal-status escalation and bounded
peak in-flight exactly equal to the configured worker count.

Still pending:
- second representative lower-end/older physical device before promoting the
  Incremental Persistent Indexing and Bounded Parallel Hashing Goldens beyond
  DRAFT;
- Store/production certification remains Step 10 and is not implied by this
  performance PASS.

PRIMARY DEVICE PERFORMANCE VALIDATED != GOLDEN PROMOTED != STORE CERTIFIED.


## Directory freshness validation

The benchmark build also includes persistent-index schema v2. A warm snapshot is
invalidated when an indexed directory fingerprint changes, forcing a full
reconciliation. This closes the stale-index case where a new non-media file could
otherwise remain absent from a reused snapshot.

Reports include `emulator_signals_detected`. A benchmark with emulator signals
is useful for diagnostics but is **not** accepted as physical-device evidence.
