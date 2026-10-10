# Android Omni Cleaner

Recovery and productization of the lost-source Android cleaner APK.

## Current state

**MAXI STEP 0 — Forensic Recovery: COMPLETE**  
**MAXI STEP 1 — Native Project Bootstrap: COMPLETE**  
**MAXI STEP 2 — Full APK Functional Reconstruction: COMPLETE**  
**MAXI STEP 3 — APK Parity Audit: COMPLETE**  
**MAXI STEP 4 — Safety & Storage Hardening: COMPLETE**  
**MAXI STEP 5 — Monetization Productization: IN PROGRESS**

The donor functionality is reconstructed and storage hardening is complete within the current evidence boundary. The active work is Play Billing, Ads/UMP productization and provider-side release validation before the planned UI redesign.

## Product direction

- Android native Kotlin + Jetpack Compose
- local-first cleaning
- Standard Cleaning usable without All files access
- optional Deep Cleaning for full-device analysis where Android supports it
- safe/reversible cleanup before maximum deletion
- monetizable Free/Pro architecture
- one-time donor product id: `premium_unlock`
- UI redesign planned after parity/safety validation
- no fake booster/antivirus claims

## Current recovery version

- applicationId: `com.riccardopinato.omnicleaner`
- version: `0.6.0-recovery` / code 6
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
