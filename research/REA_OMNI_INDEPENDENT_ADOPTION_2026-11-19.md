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

## Live execution evidence — verified

**Second independent real REA CLI + JADX pilot: [run #44626294217 PASS](https://github.com/riccardopinato/Android-Omni-Cleaner/actions/runs/44626294217)**

- Source head at run: `riccardopinato/Android-Omni-Cleaner@b594adc670bce482d0768638648196d92a3b8eaf`.
- Exact APK built from that head: `app-debug.apk`, **19,324,653 bytes**, SHA-256 `b2cb75cd332f1261ba5bfee69c47c994fd8acc0d64df88886be81c32fa14e977`; 263 ZIP entries.
- Engine: `rea-agents@6.9.0` / Node 24.22.2; Java 21 plus separately SHA-256-verified `jadx-headless-mcp-0.7.1-all.jar` v0.7.1 (`6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`). This external component is a prerequisite, **not part of the APK**.
- Actual `inspect-android-package` exit 0: compact report 28,517 bytes, SHA-256 `6423982f6146121acec9accc87cd521b786c8d4c65238019e8c94c3e0b282fa7`; full evidence locally retained at 60,054 bytes, SHA-256 `1bdd889aa718c48b5c3a923c6cdb2076ab63c84fe564e8b30d42a8c5a58b7677`.
- Actual `identify-flutter-build` on Kotlin native APK exit 0; **no** `libapp.so` AOT entries; negative test on deliberately wrong expected SHA also **PASS (reject)**.
- Earlier [run #44625530368](https://github.com/riccardopinato/Android-Omni-Cleaner/actions/runs/44625530368) failed after successful APK build because the external JADX JAR prerequisite was absent. Corrected by explicitly fetching and verifying the official JAR, then **successful independent rerun**. Failure retained in history; no falsified success.
- Saved GitHub artifact `omni-rea-independent-research-receipts` contains **only restricted receipt/version JSON**; full report and APK were not uploaded. This repository's product code, storage behavior, permissions, signing identity, releases and cleanups remain untouched.

**Evidence boundary:** the CLI completed static inspection, not a behavioral Android lifecycle or user's cleanup/safety acceptance. This satisfies a genuine separate consumer **execution pilot** but does not by itself promote the Core Golden to `CERTIFIED PROCESS`; that decision requires separate admission and assessment of incremental findings.


Proven previous source research: AppLab public Deskland experiment [#44615407360](https://github.com/riccardopinato/AppLab/actions/runs/44615407360). This prior run is **not** the independent Omni consumer evidence.
