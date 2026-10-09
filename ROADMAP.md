# Android Omni Cleaner — Recovery Roadmap

Factory baseline: App-Factory-Core a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03

## MAXI STEP 0 — Project Recovery & Forensic Reconstruction
Status: COMPLETE

Completed:
- repository governance established;
- supplied APK forensic findings frozen;
- Product Bible and machine-readable baseline created;
- native Kotlin/Compose stack preserved;
- release blockers documented;
- no unsupported claim of full source recovery.

Checkpoint:
- merged to main via PR #1.

## MAXI STEP 1 — Native Project Bootstrap
Status: COMPLETE

Completed:
- reproducible Android/Kotlin/Compose project created;
- canonical applicationId selected: `com.riccardopinato.omnicleaner`;
- debug and release build lanes separated;
- release build is non-debuggable;
- R8/resource shrinking enabled for release;
- JDK 17 / Gradle 9.6 / AGP 9.4 baseline established;
- Gradle Wrapper committed;
- Android CI added;
- bootstrap unit tests added;
- minSdk 24 preserved;
- dangerous storage permissions intentionally NOT added during bootstrap;
- INTERNAL signing policy documented without committing key material.

Evidence:
- application source checkpoint: `be6ff12559b3039b25f21481fdebc948b21e8aab`
- GitHub Actions run: `37959830963`
- result: SUCCESS
- verified command:
  `./gradlew lintDebug testDebugUnitTest assembleDebug assembleRelease --stacktrace`

Certification boundary:
- build/test/CI evidence exists;
- no physical-device validation yet;
- no distribution validation yet;
- no recovered cleaner capability is claimed active yet.

## MAXI STEP 2 — Core Storage Domain Recovery
Status: NEXT

Goals:
- reconstruct scanner contracts and models;
- storage overview;
- folder inventory;
- large files;
- path safety;
- typed scan/result states;
- deterministic cleanup plan.

Exit:
- core scanner tests;
- no destructive action required for basic validation.

## MAXI STEP 3 — Safe Cleanup Engine
Goals:
- unified Scan -> Finding -> Review -> Cleanup Plan -> Execute -> Verify pipeline;
- Safe Trash;
- failure reporting;
- rollback/recovery where practical;
- no optimistic "cleaned" state before verification.

## MAXI STEP 4 — Duplicate Engine
Goals:
- candidate grouping;
- staged hashing/digest;
- keeper rules;
- progressive scan;
- cancellation;
- large-library performance tests.

## MAXI STEP 5 — Gallery Cleaner
Goals:
- MediaStore paging;
- swipe keep/delete;
- undo;
- system trash request;
- state restoration;
- prefetch/memory discipline.

## MAXI STEP 6 — Privacy / EXIF
Goals:
- metadata inspection;
- JPEG EXIF cleanup;
- safe cleaned-copy/export;
- original preservation by default.

## MAXI STEP 7 — Capability & Permission Health
Goals:
- just-in-time permissions;
- scoped-storage-first paths;
- explicit deep-clean/all-files capability if retained;
- settings/repair flow;
- distinguish permission from actual operational health.

## MAXI STEP 8 — Monetization Reconstruction
Goals:
- provider-independent entitlement contract;
- current Play Billing adapter;
- Free/Premium/Lifetime product decision;
- AdMob test/production separation;
- UMP/privacy;
- Premium means zero ad requests.

## MAXI STEP 9 — UX / Localization / Accessibility
Goals:
- modern Compose shell;
- light/dark/system;
- device language + manual override;
- centralized strings;
- loading/empty/error/degraded states;
- small/large phone and tablet checks where supported.

## MAXI STEP 10 — QA, Signing & Release Reality
Goals:
- persistent INTERNAL signing identity;
- source/artifact hashes;
- exact-artifact physical testing;
- destructive-flow test matrix;
- install/update-in-place validation;
- AAB release readiness;
- Play policy review for broad storage access.

## Certification rule

Build green != feature verified.

Final status may only progress through:
IMPLEMENTED -> STATICALLY CHECKED -> TESTED -> CI GREEN -> ARTIFACT BUILT -> TRUSTED RUNTIME VERIFIED -> PHYSICAL DEVICE VERIFIED -> DISTRIBUTION VERIFIED -> STORE READY.
