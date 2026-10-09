# Android Omni Cleaner

Recovery project for the lost-source Android cleaner APK.

## Status

**MAXI STEP 0 — Project Recovery & Forensic Reconstruction**

The supplied APK has been audited and confirmed to be a native **Kotlin + Jetpack Compose** application. This repository is being rebuilt from forensic evidence while preserving safe behavior and removing prototype/release debt.

No reconstructed source code is claimed complete yet.

## Canonical project documents

- `PRODUCT_BIBLE.md` — product truth
- `ROADMAP.md` — execution truth
- `PROJECT_BASELINE.yaml` — machine-readable project baseline
- `RECOVERY_AUDIT.md` — APK forensic findings and evidence boundary
- `APP_FACTORY_BASELINE.md` — fixed App Factory baseline
- `AGENTS.md` — concise development/recovery rules

## Factory baseline

- App-Factory-Core: `a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03`
- Master Prompt: **v25.1**
- Golden Index: **v10**
- Golden Registry: **v2**

## Recovery direction

Preserve:
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
- application identity
- signing
- release build configuration
- CI/tests
- billing integration
- AdMob production integration
- permission/capability health
- Play policy compliance for broad storage access

## Core product rule

> Safe cleaning first, maximum cleaning second.
