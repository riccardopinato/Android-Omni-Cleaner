# Android Omni Cleaner — Recovery & Product Roadmap

Factory baseline: `a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03`

## MAXI STEP 0 — Forensic Recovery
**Status: COMPLETE**

## MAXI STEP 1 — Native Project Bootstrap
**Status: COMPLETE**

## MAXI STEP 2 — Full APK Functional Reconstruction
**Status: COMPLETE**

## MAXI STEP 3 — APK Parity Audit
**Status: IN PROGRESS**

Implemented audit harness:
- machine-readable donor contract;
- deterministic JVM parity fixture;
- controlled emulator storage fixture;
- dedicated Maestro parity journey;
- AppLab v4.5 pin;
- physical validation required for storage/file-picker/billing.

Exit gate:
- Android CI green;
- AppLab Intelligence green;
- bounded AppLab runtime parity green;
- no critical `MISSING` parity items;
- test APK available outside GitHub Actions artifact quota;
- physical-only checks remain explicit `BLOCKED` until tested.

## MAXI STEP 4 — Safety & Storage Hardening
**Status: NEXT**

## MAXI STEP 5 — Monetization Productization
Play Billing test, production AdMob/UMP, final Free/Pro boundary and pricing.

## MAXI STEP 6 — UI/UX Redesign
Apply the user's design ideas without rewriting recovered engines.

## MAXI STEP 7 — Product Expansion

## MAXI STEP 8 — Localization & Accessibility

## MAXI STEP 9 — Performance & Deep QA

## MAXI STEP 10 — Store & Release Certification

Final release verdict: `CERTIFIED / NOT CERTIFIED / BLOCKED`.
