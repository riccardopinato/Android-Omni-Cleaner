# Omni Cleaner — MAXI STEP A–D execution ledger (27 November 2026)

**Status: RC2 integration candidate / Draft PR #77 / NO PRODUCTION RELEASE.**

Baseline: App-Factory-Core `CURRENT.yaml` selects Master v27, Golden Index v31, Registry v2. Relevant contracts: Android Signing/Release Identity (draft); Release Reality/Physical Validation (draft); Immutable Build/Test Artifact (copy-ready); Entity Lifecycle/Trash (draft). These are process references, not an assertion that Android native runtime was certified.

## A — Integration and exact-head CI

- [x] Integration branch: `integration/omni-v11-colors-gallery-qa-20261121`, PR #77.
- [x] Canonical #77 includes the #73/#75 Home + Quick Look lineage, #76 palette lineage, #39 R1 code, #49 video gate, SD Maid documentation-only research. Do not copy GPL source.
- [x] Previously observed PASS on `eaa314e6486cc8665657de2950514d40926b2bea`: Android CI, R1, synthetic-safety, and 14 API35 instrumentation tests (9 individually executed classes).
- [ ] **Re-run on new exact head** after RC2 version/identity changes. The old PASS cannot certify newer commits.
- [ ] Visual screenshot/video gate PASS on exact new head. The earlier `45988593530` attempt was cancelled; do not silently substitute an older PASS.
- [ ] Compare and disposition the separate open PRs #18/#39/#40/#41/#49/#72/#73/#74/#75/#76; close only once merged/superseded, retaining evidence and not force-pushing their heads.
- [ ] Visually approve current **Soft Pastel Cleaner**, not historical Pop Fresh wording. Validate at 360dp, 200% font, light/dark, landscape, TalkBack.

## B — File integrity and on-device performance

- [x] Android media trash has per-URI read-after-write gate in #77, with partial success retention; consent alone does not count as deletion.
- [x] Background indexing uses WorkManager and version/generation detection, historic SQLite summary and explicit manual full refresh.
- [ ] Real-device SAF/MediaStore consent, deny, revoke, mixed result and process-death tests — use only consented synthetic fixture files, not personal photos.
- [ ] Three matched R0/R1 performance runs on representative physical inventory (target ~88k files): median/p95, memory, thermal, battery, cold/warm timing and index consistency.
- [ ] True per-file incremental index reconciliation is **not implemented**. Current change detection performs a full reconcile; non-media changes can stay cached until the 24-hour audit/manual refresh.
- [ ] Verify duplicate-keeper, 50 MB Safe Trash, restore collision, failure/cancellation, selected-item revision and permanent purge confirmation on hardware.

Physical evidence schema per case: `{sourceSha, apkSha256, deviceModel, androidApi, scenarioId, dataSet, beforeHash, afterHash, observed, result, logs, screenshots}`. **A green emulator test is not a hardware PASS**.

## C — Signing and upgrade

- [x] This candidate raises `versionCode` from **14 to 15** and `versionName` from `1.0.0-rc1` to `1.0.0-rc2`. Package remains `com.riccardopinato.omnicleaner`; debug package remains separate.
- [x] Release-candidate workflow now rejects unexpected package IDs and APK version codes <=14, on top of release keystore certificate matching for both APK and AAB.
- [ ] Configure/verify existing protected key material and `OMNI_EXPECTED_CERT_SHA256` through GitHub Actions (never add private keys/passwords to source or chat).
- [ ] Run `.github/workflows/release-candidate.yml` at the selected immutable exact-head commit; record source SHA, run ID, APK/AAB SHA-256, certificate fingerprint and package/version. Workflow Dispatch alone does not imply signed success.
- [ ] On physical Android: install previously signed RC1, populate benign fixture data; install RC2 **without clearing data**; check preservation and restored trash/index state; reject downgrades.
- [ ] Verify APK and AAB from the same signed run. Do not deliver a debug-signed APK as a production update.

**Release guard:** signing identity + source/build/artifact manifest + physical update PASS must all hold before promoting an RC.

## D — Google Play and monetization

- [ ] `MANAGE_EXTERNAL_STORAGE` policy declaration + evidence of core file-management functionality and Standard mode without broad access.
- [ ] Update and confirm `PRIVACY_POLICY.md` and `DATA_SAFETY.md` against actual runtime data collection/permissions; publish privacy URL.
- [ ] Google Play `premium_unlock` internal license-test matrix: purchase, cancel, pending, restore, refund/revoke, already-owned, offline grace. No live paid tests.
- [ ] AdMob test-unit QA; production app/banner/interstitial IDs; UMP consent, refusal and privacy options; `app-ads.txt` verification; no premium ad requests.
- [ ] Generate screenshots/metadata from reviewed exact-head build, upload approved AAB to internal testing, then complete applicable Play testing and policy review.
- [ ] Production rollout only after safety, signing, privacy, monetization and store gates are explicitly PASS.

## Decision

**No merge yet.** PR #77 is the proposed canonical integration. The code changes in this checkpoint improve future upgrade eligibility and signed artifact identity checks, but they do **not** substitute for exact-head CI, on-device storage safety, physical update validation or Play provider configuration.

References: `docs/OMNI_PR_CONSOLIDATION_2026_11_21.md`, `docs/OMNI_MAXI_STEP_11_PHYSICAL_QA_2026_11_27.md`, `PLAY_CONSOLE_SETUP.md`, `.github/workflows/release-candidate.yml`.
