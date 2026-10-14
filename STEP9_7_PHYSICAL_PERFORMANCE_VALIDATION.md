# MAXI STEP 9.7 — Physical Performance Validation

Status: **HARNESS IMPLEMENTED / REAL-DEVICE EVIDENCE PENDING**

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

Pending:
- real-device report from the exact Step 9.7 APK;
- analysis of measured worker choice;
- any optimization justified by those measurements;
- second-device evidence for promotion of the two DRAFT Goldens.

PHYSICAL BENCHMARK HARNESS IMPLEMENTED != PHYSICAL PERFORMANCE VALIDATED.
