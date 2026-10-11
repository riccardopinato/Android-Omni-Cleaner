# Android Omni Cleaner

Recovery and productization of the lost-source Android cleaner APK.

## Current state

**MAXI STEP 0 — Forensic Recovery: COMPLETE**  
**MAXI STEP 1 — Native Project Bootstrap: COMPLETE**  
**MAXI STEP 2 — Full APK Functional Reconstruction: COMPLETE**  
**MAXI STEP 3 — APK Parity Audit: COMPLETE**  
**MAXI STEP 4 — Safety & Storage Hardening: COMPLETE**  
**MAXI STEP 5 — Monetization Productization: COMPLETE (provider validation pending)**  
**MAXI STEP 6 — UI/UX Redesign: IN PROGRESS**

The donor functionality, storage hardening and monetization architecture are reconstructed. The active work is the new multi-surface UI/UX shell; Play/AdMob provider validation remains a separate release gate.

## Product direction

- Android native Kotlin + Jetpack Compose
- local-first cleaning
- Standard Cleaning usable without All files access
- optional Deep Cleaning for full-device analysis where Android supports it
- safe/reversible cleanup before maximum deletion
- monetizable Free/Pro architecture
- one-time donor product id: `premium_unlock`
- colorful five-surface UI shell with later visual refinement supported
- no fake booster/antivirus claims

## Current recovery version

- applicationId: `com.riccardopinato.omnicleaner`
- version: `0.7.0-recovery` / code 7
- minSdk 24
- targetSdk 36
- compileSdk 37
- JDK 17
- Gradle 9.6
- AGP 9.4

## Monetization model

- Free keeps the core cleaner and safety features.
- Premium is the one-time Google Play product `premium_unlock`.
- Premium unlocks Pro combined/batch tools and blocks all ad requests.
- Pricing is supplied by Google Play, never hardcoded.
- UMP must allow ads before the Free tier can request them.
- Debug builds use Google demo ad units.
- Release ads stay disabled until production AdMob IDs are configured.

See `MONETIZATION_CONTRACT.md` and `PLAY_CONSOLE_SETUP.md`.

## UI/UX shell

The recovery-era single long screen has been replaced by five destinations:

- Home — storage hero, metrics, access health and quick actions;
- Clean — scans, thresholds, findings, cleanup, Safe Trash and advanced maintenance;
- Gallery — focused media review;
- Privacy — focused EXIF safe-copy workflow;
- Pro — purchase/restore, privacy options and Pro batch tools.

The visual system uses a fixed colorful identity, soft surfaces and dark mode without adding a heavy navigation/icon dependency. Cleaner, Gallery, EXIF and monetization engines remain independent from the shell.

See `STEP6_UI_REDESIGN.md`.

## Storage safety model

- Gallery/EXIF and scoped flows do not require All files access.
- Deep Cleaning is opt-in and never requested at first launch.
- Android 10 is not falsely represented as full-filesystem capable under scoped storage.
- scan results are revalidated before cleanup;
- duplicate deletion requires a surviving unchanged keeper;
- media use Android system trash;
- small non-media files may use Safe Trash;
- large non-media files have no permanent-delete fallback;
- moving to a trash location is not reported as bytes already freed;
- Safe Trash stores integrity metadata and supports verified restore without overwrite;
- Android 14+ partial photo/video access is supported without pretending it is full access.

See `STORAGE_PERMISSION_POLICY.md`.

## Quality lanes

### Android CI
Lint, unit tests, debug build and release build.

### AppLab Intelligence
Pinned to AppLab v4.5 at
`dc826264c31dd79fc7617ab7a4a8aee61f9b5aba`.

### AppLab runtime
Bounded emulator evidence is available. Physical storage / SAF / Billing evidence
remains a separate gate and is never inferred from emulator PASS.

## Key documents

- `PRODUCT_BIBLE.md`
- `DONOR_PARITY_MATRIX.md`
- `STORAGE_PERMISSION_POLICY.md`
- `RECOVERY_AUDIT.md`
- `PROJECT_BASELINE.yaml`
- `ROADMAP.md`
- `APPLAB.md`
- `RELEASE_SIGNING.md`
- `AGENTS.md`

## Core rule

> Safe cleaning first, maximum cleaning second.
