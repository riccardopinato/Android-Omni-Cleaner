# Android Omni Cleaner

Recovery and productization of the lost-source Android cleaner APK.

## Current state

**MAXI STEP 0 — Forensic Recovery: COMPLETE**  
**MAXI STEP 1 — Native Project Bootstrap: COMPLETE**  
**MAXI STEP 2 — Full APK Functional Reconstruction: COMPLETE**  
**MAXI STEP 3 — APK Parity Audit: COMPLETE**  
**MAXI STEP 4 — Safety & Storage Hardening: COMPLETE**  
**MAXI STEP 5 — Monetization Productization: COMPLETE (provider validation pending)**  
**MAXI STEP 6 — UI/UX Redesign: COMPLETE**  
**MAXI STEP 7 — Product Expansion: COMPLETE**  
**MAXI STEP 8 — Localization & Accessibility: COMPLETE**  
**MAXI STEP 9 — Performance & Deep QA: IMPLEMENTATION COMPLETE (validation deferred)**  
**MAXI STEP 9.5 — Post-Audit P1/P2/P3 Hardening: SOURCE COMPLETE**  
**MAXI STEP 9.6 — Persistent Indexing & Bounded Hashing: IMPLEMENTED (fast exact-head CI PASS)**

The recovered cleaner, safety model, monetization architecture, redesigned shell, Smart Categories and localization/accessibility layer are complete through Step 8. Play/AdMob provider validation and physical-device certification remain separate release gates.

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
- version: `0.12.0-recovery` / code 12
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

See `MONETIZATION_CONTRACT.md`, `PLAY_CONSOLE_SETUP.md`, `PRIVACY_POLICY.md`, `DATA_SAFETY.md` and `APP_ADS_SETUP.md`.

## UI/UX shell

The recovery-era single long screen has been replaced by five destinations:

- Home — storage hero, metrics, access health and quick actions;
- Clean — scans, thresholds, findings, cleanup, Safe Trash and advanced maintenance;
- Gallery — focused media review;
- Privacy — focused EXIF safe-copy workflow;
- Pro — purchase/restore, privacy options and Pro batch tools.

The visual system uses a fixed colorful identity, soft surfaces and dark mode without adding a heavy navigation/icon dependency. Cleaner, Gallery, EXIF and monetization engines remain independent from the shell.

See `STEP6_UI_REDESIGN.md`.

## Step 7 product expansion

Smart Categories adds review-only discovery for:
- downloads older than 90 days;
- installer packages (APK/APKS/XAPK/APKM);
- archives;
- screenshots.

The scan uses the shared local inventory, never auto-selects files, and reuses the existing snapshot revalidation + Android Trash / Safe Trash safety model. Perceptual similar-photo detection is explicitly deferred to Product Expansion 2 / v1.1.

See `STEP7_PRODUCT_EXPANSION.md`.

## Localization & accessibility

The interface is fully resource-driven across English, Italian, Spanish, French and Portuguese, with exact resource-key and format-placeholder parity. A manual System / EN / IT / ES / FR / PT selector is available in Pro settings.

Accessibility hardening includes TalkBack-friendly selection rows, explicit non-gesture alternatives for Gallery actions, progress semantics, decorative-glyph suppression and large-font layout resilience.

See `STEP8_LOCALIZATION_ACCESSIBILITY.md`.

## Performance & deep QA

Step 9 hardens large-library behavior with duplicate candidate prefiltering, 256 KiB hashing/comparison buffers, reduced canonical-path filesystem overhead, cancellable hashing/MediaStore queries, a bounded Gallery review window, source-revalidated EXIF export and a deterministic large-library runtime fixture. The Clean workspace also includes an extension dropdown that can restrict expensive duplicate hashing before it starts.

Step 9.6 adds a persistent SQLite storage index and bounded CPU-aware hashing workers. Persisted sample/full hashes are bound to strong file revisions on supported Android versions, physical aliases are collapsed before duplicate grouping, and destructive cleanup still revalidates live files. A manual full-index refresh remains available.

See `STEP9_PERFORMANCE_DEEP_QA.md`, `STEP9_5_AUDIT_HARDENING.md`, `STEP9_6_PERSISTENT_INDEXING_BOUNDED_HASHING.md` and `PERFORMANCE_BUDGETS.md`.

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
- Safe Trash stores integrity metadata in hidden shared storage, survives uninstall/reinstall, and supports verified restore without overwrite;
- Android 14+ partial photo/video access is supported without pretending it is full access.

See `STORAGE_PERMISSION_POLICY.md`.

## Quality lanes

### Android CI
Pull requests run exact-head locale parity, JVM tests, instrumentation-source compilation and debug APK assembly. Full lint/debug/release verification remains available on main/manual lanes.

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


### Fast APK

During fast-development mode, pull requests use one exact-head lane:
- locale parity + JVM tests + debug APK assembly;
- the same successful job publishes the SHA-bound prerelease APK + SHA-256;
- no duplicate automatic Fast APK runner;
- AppLab/full release certification remains deferred;
- development/testing artifact only, not Store Ready.


### Duplicate scan UX

- duplicate search now reports indexed/candidate progress instead of showing only an indefinite spinner;
- completed scans explicitly report when no duplicate groups were found;
- the file-type dropdown includes common extensions such as `.apk`, `.pdf`, archives, images, video, audio and office files;
- when an extension is selected, duplicate candidates are filtered **before** sample/SHA-256/byte verification;
- large-file scans apply the same extension filter before sorting and the 500-item result cap.


### Persistent indexing & bounded hashing

- app-private SQLite index survives process/app restart;
- warm reuse re-stats known files instead of blindly trusting persisted rows;
- MediaStore generation drift invalidates reuse where available;
- app-owned mutations mark the persistent index stale;
- sample/full hashes are reused only under trustworthy revision identity;
- duplicate hashing uses a bounded 1–4 worker policy instead of task-per-file fanout;
- device/inode aliases are collapsed before duplicate grouping;
- final duplicate equality still requires byte-for-byte verification;
- manual full index refresh is exposed in Clean.

Exact-head development evidence for the first implementation: Android CI run `38484281905` PASS on `aa5f6e9951237fcfafaa1c361c4aa643c439ad63`.
