# Android Omni Cleaner

Recovery and productization of the lost-source Android cleaner APK.

## Current state

**MAXI STEP 2 — Full APK Functional Reconstruction: source-level complete**  
**MAXI STEP 3 — APK Parity Audit: next**

The repository is no longer a bootstrap shell. It now contains reconstructed functional implementations for the main donor APK capabilities: storage scanning, large files, duplicates, safe cleanup, Gallery Cleaner, EXIF privacy, thumbnail/residue analysis, Pro batch review, Billing and consent-aware advertising.

The remaining boundary is runtime parity: build/static evidence does not yet prove the reconstructed APK behaves identically on a real device.

## Product direction

- Android native Kotlin + Jetpack Compose
- local-first cleaning
- safe/reversible cleanup before maximum deletion
- monetizable Free/Pro architecture
- one-time donor product id: `premium_unlock`
- UI redesign planned after parity/safety validation
- no fake booster/antivirus claims

## Current recovery version

- applicationId: `com.riccardopinato.omnicleaner`
- version: `0.4.0-recovery` / code 4
- minSdk 24
- targetSdk 36
- compileSdk 37
- JDK 17
- Gradle 9.6
- AGP 9.4

## Quality lanes

### Android CI
Runs lint, unit tests, debug build and release build.

### AppLab Intelligence
Pinned to AppLab `d3681e4d2b92631ce96fcfc80349a079cbed40bb` and provides deterministic Product/UX/Architecture evidence.

### AppLab trusted runtime
Currently manual/BLOCKED by GitHub Actions artifact quota. This is an evidence-infrastructure limitation, not a runtime PASS.

## Key documents

- `PRODUCT_BIBLE.md`
- `DONOR_PARITY_MATRIX.md`
- `RECOVERY_AUDIT.md`
- `PROJECT_BASELINE.yaml`
- `ROADMAP.md`
- `APPLAB.md`
- `RELEASE_SIGNING.md`
- `AGENTS.md`

## Core rule

> Safe cleaning first, maximum cleaning second.
