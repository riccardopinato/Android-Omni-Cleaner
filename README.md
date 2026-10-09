# Android Omni Cleaner

Recovery project for the lost-source Android cleaner APK.

## Status

**MAXI STEP 1 — Native Project Bootstrap: COMPLETE**  
**MAXI STEP 2 — Core Storage Domain Recovery: NEXT**

The supplied APK was audited and confirmed to be a native **Kotlin + Jetpack Compose** application. The repository now contains a clean, reproducible Android project and CI baseline; recovered cleaner capabilities are restored only after their contracts and tests exist.

## Current bootstrap

- applicationId: `com.riccardopinato.omnicleaner`
- minSdk: 24
- targetSdk: 36
- compileSdk: 37
- JDK: 17
- Gradle Wrapper: 9.6
- AGP: 9.4
- UI: Jetpack Compose + Material 3
- release build: non-debuggable, R8/resource shrinking enabled
- dangerous storage permissions: not enabled in bootstrap
- CI: lint + unit tests + debug/release builds
- latest bootstrap CI evidence: run `37959830963` — SUCCESS

## Canonical project documents

- `PRODUCT_BIBLE.md` — product truth
- `ROADMAP.md` — execution truth
- `PROJECT_BASELINE.yaml` — machine-readable project baseline
- `RECOVERY_AUDIT.md` — APK forensic findings and evidence boundary
- `APP_FACTORY_BASELINE.md` — fixed App Factory baseline
- `RELEASE_SIGNING.md` — signing policy
- `AGENTS.md` — concise development/recovery rules

## Factory baseline

- App-Factory-Core: `a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03`
- Master Prompt: **v25.1**
- Golden Index: **v10**
- Golden Registry: **v2**

## Recovery direction

Preserve and reconstruct:
- Kotlin
- Jetpack Compose
- Material 3
- MediaStore-aware cleanup
- Safe Trash
- duplicate engine concepts
- gallery swipe cleaner
- EXIF/privacy cleanup
- path safety

Rebuild before store release:
- recovered core scanner behavior;
- persistent INTERNAL signing identity;
- billing integration;
- AdMob production integration;
- permission/capability health;
- physical-device validation;
- Play policy compliance for broad storage access if retained.

## Core product rule

> Safe cleaning first, maximum cleaning second.
