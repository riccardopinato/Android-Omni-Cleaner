# REA CLI — Independent Android Omni Cleaner APK research pilot

**2026-11-19 · RESEARCH_ONLY · NOT A RELEASE / NOT DEVICE CERTIFIED**

This file documents an **independent consumer/harness** for the App Factory
Golden [REA CLI Reverse Intelligence v1](https://github.com/riccardopinato/App-Factory-Core/blob/main/golden/governance/GOLDEN_REA_CLI_REVERSE_INTELLIGENCE_v1.txt) (Index v30, `COPY-READY PROCESS`). Source analysis remains authoritative for features. The legacy recovery APK and protected RC1 signing identity must NOT be substituted for this separately built debug APK.

## Design and scope

- Workflow: `.github/workflows/rea-independent-apk-research.yml`, runs on the exact Omni PR head and produces a fresh local `app-debug.apk` with the existing Kotlin/Compose Gradle build.
- Runs JDK 17 for the app's Gradle build, then isolated JDK 21 for REA's headless JADX inspection; Node 24, `rea-agents@6.9.0` MIT installed in ephemeral runner only.
- Checks out the **pinned AppLab research adapter** into a separate `.applab-research` workspace; no source dependency or runtime module inserted into the Android app.
- REA CLI `inspect-android-package` captures manifest/component and static package metadata via the bundled JADX engine. Large full Evidence is stored **locally only** in the runner; uploaded artifact contains only limited receipts and hashes, never APK or decoded code.
- REA `identify-flutter-build` on this **native Kotlin app** is a true negative control, not evidence of Flutter usage.
- Additional negative test verifies the adapter refuses a wrong expected SHA-256 **before executing the analysis**.
- Output is an evidentiary annex to AppLab, **not an alternative trusted verifier**, Play Console artifact, signed release, UI acceptance, malware clearance, user-data scan or permission audit at runtime.

## Acceptance and interpretation

A valid independent pilot must have: exact source SHA, new APK SHA-256, REA 6.9.0 and Java toolchain, successful real CLI operation, hashed raw report, negative Flutter control and negative mismatched-hash refusal, and all required repository PR exact-head checks green. **A workflow file or a generated APK is not evidence that the engine actually ran**: record the GitHub run URL, exit statuses and result fields after a successful run.

If successful, this is a **second project** and different artifact family compared to the Deskland public Windows PE/NSIS/.NET pilot. It supports a future *scoped* `CERTIFIED PROCESS` evaluation, subject to independent source/binary gain review and explicit Core admission; no automated E5/CERTIFIED promotion.

If failed/unavailable, record `NOT_VERIFIED` and the blocking reason; preserve the App Factory Golden at `COPY-READY`, preserve the original app code, and do not weaken checks or run REA continuously.

## Privacy and IP

Only the locally built APK from this owned repo is analyzed. The workflow is read-only, has `contents: read`, does not request device, Google Play, storage-media or emulator permissions, does not send personal content to GitHub artifacts, does not alter `PRODUCT_BIBLE.md`, RC1 freeze, cleaning logic, APK signature, feature flags or provider settings. No third-party app decompilation or protected asset copying.

## Live evidence

**Pending exact-head workflow run:** results to be completed after the pull request's checks, not fabricated in this document.

Proven previous source research: AppLab public Deskland experiment [#44615407360](https://github.com/riccardopinato/AppLab/actions/runs/44615407360). This prior run is **not** the independent Omni consumer evidence.
