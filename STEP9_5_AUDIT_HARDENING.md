# MAXI STEP 9.5 — Post-Audit Hardening

Factory baseline: `fa3c0679de393f1cb13c2fed2be314d1a1269353`  
Version: `0.11.0-recovery` / code 11  
Status: **SOURCE COMPLETE — EXECUTABLE VALIDATION DEFERRED**

This step resolves the P1/P2/P3 findings from the 2026-10-12 source audit without pretending blocked GitHub-hosted validation has passed.

## P1 — user data / commercial reliability / product identity

### Safe Trash uninstall safety

Resolved in source:
- new Safe Trash location: hidden app-managed shared storage under `.OmniCleaner/SafeTrash`;
- new reversible payloads no longer depend on `context.filesDir`;
- Safe Trash is excluded/protected from general cleaner traversal;
- old app-private recovery entries are migrated only after metadata parsing plus source/target payload SHA-256 and size verification;
- failed migration leaves the legacy source untouched;
- restore/permanent-delete operations fail closed if the persistent trash root is unavailable or unsafe;
- UI discloses that Safe Trash survives uninstall and still occupies storage.

Physical permission/OEM validation remains required.

### Premium offline reliability

Resolved in source:
- provider-neutral entitlement state includes source + verification/grace timestamps;
- last PLAY-verified lifetime Premium entitlement is cached locally;
- bounded offline Premium grace: 30 days;
- cached FREE state never authorizes ad requests;
- expired/unknown state returns to unresolved/no-ads rather than silently classifying the user Free;
- a verified Play response immediately overrides stale cache;
- UI discloses when Premium is running from offline cache.

Play purchase/refund/restore validation remains a Store gate.

### Product launcher identity

Resolved in source:
- intentional Omni Cleaner shield/storage-cleanup mark;
- legacy vector launcher resource;
- Android adaptive icon;
- Android 13+ monochrome icon;
- manifest icon + roundIcon wired;
- `BRANDING_NOTES.md` added.

Launcher/store-size physical review remains a release gate.

## P2 — performance / lifecycle / i18n / monetization / QA / Store prep

### Shared storage inventory

Resolved in source:
- new `StorageInventoryRepository`;
- 120 s interactive cache TTL;
- Overview, Folders, Large Files, Duplicates and Smart Categories reuse the same read-only inventory;
- Pro Batch benefits from the same shared snapshot;
- cleanup completion invalidates the snapshot;
- destructive cleanup still performs independent mutation-time snapshot/hash validation;
- snapshot records elapsed scan time and cache-hit state.

See `PERFORMANCE_BUDGETS.md`.

### Configuration-change resilience

Resolved in source:
- `OmniCleanerViewModel` retains Cleaner, Gallery and Pro Batch controllers across Activity recreation;
- controller jobs use a ViewModel-owned supervisor scope;
- Gallery durable session persistence remains DataStore-backed;
- process death does not fake freshness: filesystem scans are rebuilt rather than restoring stale file snapshots.

### Localization failure paths

Resolved in source:
- UI no longer exposes raw `Throwable.message` for Cleaner, Gallery, Smart Categories or Pro Batch failures;
- failure copy is routed through localized resource bundles;
- Gallery / EXIF / Pro domain enums no longer carry Italian UI labels;
- user-facing labels remain in EN/IT/ES/FR/PT resources;
- hardened locale bundles retain exact **297-key** and format-placeholder parity.

### AdView lifecycle

Resolved in source:
- inline adaptive `AdView` is remembered per width/unit;
- `DisposableEffect` destroys the view when the slot leaves composition;
- Premium/unresolved/consent rules remain unchanged.

### Android instrumentation baseline

Added:
- launcher identity contract;
- MainActivity exported contract;
- storage/media manifest permission contract;
- Safe Trash non-private-storage contract.

This does not replace AppLab/physical tests.

### Privacy / Data Safety / AdMob readiness

Added:
- `PRIVACY_POLICY.md`;
- `DATA_SAFETY.md`;
- `APP_ADS_SETUP.md`;
- `PERFORMANCE_BUDGETS.md`.

External URLs, publisher ID, AdMob IDs and final Play declarations remain intentionally `TODO_EXTERNAL`; no value is invented.

## P3 — maintainability

Resolved in source:
- Home/Clean UI extracted from the navigation shell into `CleanerScreens.kt`;
- root shell reduced substantially;
- obsolete formatter removed;
- localized labels removed from domain enums and kept at UI/resource boundary.

## Explicit v1 deferral

Similar/perceptual-photo detection is **not** part of v1.0 release scope. It is moved to Product Expansion 2 / v1.1 because it adds false-positive, CPU, cache and UX review surfaces that are not required to ship the safe cleaner core.

## Evidence boundary

Current source state can be audited statically, but exact-head build/runtime gates are still blocked by the GitHub Actions account allowance/budget state.

Therefore:

```text
IMPLEMENTED != VALIDATED
VALIDATION DEFERRED != PASS
FAST APK != STORE CERTIFIED
```

When a runner becomes available, rerun:
1. locale parity;
2. JVM tests;
3. `androidTest`;
4. debug + release build;
5. AppLab Intelligence;
6. AppLab FULL;
7. exact-head APK + SHA-256;
8. physical/Play Store gates.


## Final Codex review closure

The exact-head review after the initial P1/P2/P3 pass raised five additional findings. They are resolved in source:

1. automatic PR verification restored as a single exact-head lane;
2. inventory cache uses an atomic invalidation generation so pre-cleanup in-flight scans cannot reinstall stale snapshots;
3. Gallery persists the new current media anchor immediately after a page append;
4. retained controllers receive an application-scoped context configured from the current Activity locale, and stale transient copy is cleared only when the locale changes;
5. Pro Android Trash no longer captures an Activity launcher callback inside a retained job: the PendingIntent is retained as controller state and consumed by the currently registered launcher.

The automatic PR lane is deliberately lightweight during FAST DEVELOPMENT MODE (locale parity + JVM tests + debug assembly) and publishes the same exact-head debug APK on success. Full lint/release/AppLab remain deferred certification gates.
