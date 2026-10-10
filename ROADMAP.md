# Android Omni Cleaner — Recovery & Product Roadmap

Factory baseline: `a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03`

## MAXI STEP 0 — Forensic Recovery
**Status: COMPLETE**

## MAXI STEP 1 — Native Project Bootstrap
**Status: COMPLETE**

## MAXI STEP 2 — Full APK Functional Reconstruction
**Status: COMPLETE**

## MAXI STEP 3 — APK Parity Audit
**Status: COMPLETE — PHYSICAL/PLAY GATES BLOCKED**

Completed:
- machine-readable donor contract;
- deterministic JVM parity fixture;
- controlled emulator storage fixture;
- dedicated Maestro parity journey;
- AppLab v4.5 Intelligence + runtime PASS;
- exact test APK prerelease + SHA-256 sidecar;
- final parity matrix with **0 critical MISSING**.

Explicitly blocked rather than inferred:
- real-device storage cleanup;
- MediaStore system trash;
- Gallery real-media flow;
- SAF EXIF export;
- Play Billing purchase/restore;
- production Ads/UMP;
- trusted artifact-bound AppLab certification while artifact quota is saturated.

## MAXI STEP 4 — Safety & Storage Hardening
**Status: IN PROGRESS**

Implemented:
- explicit Standard Cleaning vs Deep Cleaning capability model;
- no false "broad access" claim on Android 10 scoped storage;
- just-in-time Deep Cleaning disclosure before special access;
- Android 6–9 read + write storage permission pair requested only for Deep Cleaning;
- `requestLegacyExternalStorage` removed;
- scan-result snapshots revalidated immediately before mutation;
- duplicate cleanup requires an unchanged verified keeper;
- well-known top-level user folders protected from empty-folder deletion;
- Safe Trash / Android trash explicitly not counted as already reclaimed space;
- thumbnail cleanup records bytes actually deleted;
- current Google Play All files access justification documented in `STORAGE_PERMISSION_POLICY.md`;
- Android 14+ Selected Photos Access modeled as FULL / PARTIAL / NONE;
- Gallery session invalidated when media authorization changes;
- Safe Trash now has integrity-verified restore, no-overwrite behavior and permanent-empty UI.

Remaining Step 4 gates:
- Android CI + AppLab on the hardened branch;
- inspect AppLab findings and fix actionable issues;
- bounded runtime smoke;
- OEM/device-only cases remain physical-validation work, not inferred PASS.

## MAXI STEP 5 — Monetization Productization

- configure Play Console one-time product;
- Billing purchase/restore/acknowledge tests;
- production AdMob IDs;
- UMP/privacy validation;
- final Free/Pro boundary;
- final pricing;
- verify Premium generates zero ad requests.

## MAXI STEP 6 — UI/UX Redesign

Apply the user's later design ideas without rewriting recovered engines.

## MAXI STEP 7 — Product Expansion

Evaluate new cleaner capabilities without fake RAM/CPU/battery/antivirus claims.

## MAXI STEP 8 — Localization & Accessibility

en/it/es/fr/pt, manual language selection, TalkBack, scaling and responsive layouts.

## MAXI STEP 9 — Performance & Deep QA

Large libraries, low storage, cancellation, background/process death and low-end devices.

## MAXI STEP 10 — Store & Release Certification

Persistent signing, exact artifact identity, physical evidence, AAB, Internal Testing and final
`CERTIFIED / NOT CERTIFIED / BLOCKED` verdict.
