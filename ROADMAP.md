# Android Omni Cleaner — Recovery & Product Roadmap

Factory baseline: `fa3c0679de393f1cb13c2fed2be314d1a1269353`

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
**Status: COMPLETE — PHYSICAL OEM/STORAGE GATES REMAIN**

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

Step 4 evidence:
- Android CI PASS;
- AppLab Intelligence PASS;
- AppLab Runtime push PASS on the final source head;
- physical OEM/storage/SAF behavior remains a separate validation gate.

## MAXI STEP 5 — Monetization Productization
**Status: COMPLETE — PROVIDER/PLAY VALIDATION BLOCKED**

Implemented in source:
- Billing Library 9.1.0;
- one app-level BillingClient lifecycle;
- automatic service reconnection;
- donor one-time product `premium_unlock`;
- ownership query at startup and explicit Restore purchase;
- PENDING never unlocks Premium;
- PURCHASED unlocks Premium and is acknowledged;
- Free core cleaner / Premium Pro batch boundary;
- Premium produces zero ad requests;
- UMP 4.0.0 before ad eligibility;
- inline adaptive banner;
- donor interstitial cadence retained;
- debug demo ads only;
- release ads disabled until real AdMob IDs are configured;
- production monetization fail-fast guard available.

External blockers:
- Play Console product activation;
- signed AAB/Internal Testing;
- purchase/restore/pending/refund validation;
- production AdMob IDs and UMP message;
- app-ads.txt.

## MAXI STEP 6 — UI/UX Redesign
**Status: COMPLETE**

Implemented:
- new colorful design system with fixed light/dark identity;
- recovery mega-scroll replaced by five stable destinations: Home, Clean, Gallery, Privacy, Pro;
- storage hero with capacity progress;
- horizontal metrics and quick-action cards;
- bottom navigation with persistent destination state;
- dedicated cleaning workspace for scans, filters and results;
- Gallery / EXIF / Pro moved to focused surfaces;
- old recovery shell removed after replacement;
- no new heavy icon/navigation dependency;
- version `0.7.0-recovery` / code 7;
- dedicated Step 6 Maestro journey;
- AppLab custom visual journey for all five primary destinations.

Current external blocker:
- GitHub-hosted jobs are currently rejected before runner assignment because the account Actions allowance/budget gate is exhausted; this is BLOCKED evidence, not an application test failure.

Exit gate:
- Android CI PASS;
- AppLab Intelligence PASS;
- AppLab runtime + Maestro PASS;
- multi-screen visual journey PASS/WARN with no high-confidence visual failure;
- Step 6 debug APK prerelease generated.

The design system is intentionally tokenized so later user-specific visual ideas can change colors, card language and composition without rewriting cleaner engines.

## MAXI STEP 7 — Product Expansion
**Status: COMPLETE**

Scope selected after REUSE-FIRST comparison:
- Smart Categories based on one local inventory pass;
- Old Downloads (90+ days);
- installer packages (APK/APKS/XAPK/APKM);
- archives (ZIP/RAR/7Z/TAR/GZ and related formats);
- screenshots by folder/name signal;
- no automatic selection or automatic deletion;
- scan-bound snapshot revalidation before any mutation;
- media remain mediated by Android Trash;
- small non-media can use verified Safe Trash;
- unsupported irreversible deletion remains blocked.

Explicitly deferred:
- perceptual/similar-photo hashing until performance work is ready;
- Shizuku/root expansion;
- scheduled/background deletion;
- RAM/CPU/battery/antivirus claims.

Exit gate:
- focused JVM classification tests PASS;
- Android CI PASS;
- AppLab Intelligence PASS;
- AppLab runtime + Maestro PASS/WARN with no high-confidence failure;
- Step 7 exact-head APK prerelease generated.

See `STEP7_PRODUCT_EXPANSION.md`.

## MAXI STEP 8 — Localization & Accessibility
**Status: COMPLETE**

Implemented:
- complete EN / IT / ES / FR / PT resource bundles;
- exact 284-key parity across all five locales;
- exact format-placeholder parity;
- manual System / EN / IT / ES / FR / PT language selector;
- lightweight persisted locale override applied at Activity context boundary;
- Android localeConfig for all supported locales;
- TalkBack-friendly toggleable selection rows;
- decorative glyphs removed from accessibility semantics;
- progress semantics for the storage hero;
- large-font resilience for action/filter rows and selection content;
- version `0.9.0-recovery` / code 9.

Exit gate:
- resource key/placeholder parity PASS;
- Android CI PASS;
- AppLab Intelligence PASS;
- AppLab runtime + Maestro PASS;
- multi-screen visual journey PASS/WARN with no high-confidence visual failure;
- Step 8 exact-head debug APK prerelease generated.

See `STEP8_LOCALIZATION_ACCESSIBILITY.md`.

## MAXI STEP 9 — Performance & Deep QA
**Status: IN PROGRESS — VALIDATION BLOCKED BY GITHUB ACTIONS QUOTA**

Implemented:
- duplicate 64 KiB prefix/suffix prefilter before full SHA-256;
- full duplicate hashing remains cancellable and byte-verified;
- MediaStore queries cancel with their coroutine;
- stale Gallery queries are cancelled on filter/sort changes;
- Gallery review memory and undo history are bounded;
- EXIF export reduces redundant I/O while retaining end-to-end digest verification;
- EXIF cancellation is propagated and temp-file failures are contained per item;
- thumbnail hashing/cleanup traversal is cancellation-aware;
- deterministic 1,253-file runtime performance fixture;
- AppLab FULL plan enables performance, lifecycle, resource-pressure/process-death, background and storage labs;
- version `0.10.0-recovery` / code 10;
- Step 9 branch detection works on pull-request and manual runs, so FULL cannot silently degrade to FAST;
- CI checks out the immutable PR head instead of the synthetic merge ref;
- feature-branch push + pull-request duplicate workflow runs have been removed;
- parity thumbnail fixture is isolated from the recursively pushed storage tree;
- EXIF temp export independently revalidates the source after copying before any cleaned output is trusted.

Exit gate:
- Android CI PASS;
- focused performance/correctness tests PASS;
- AppLab Intelligence PASS;
- AppLab FULL runtime + retained parity + accessibility journeys PASS;
- deep labs execute with no blocking error, crash, ANR or OOM;
- Step 9 exact-head APK prerelease generated.

See `STEP9_PERFORMANCE_DEEP_QA.md`.

## MAXI STEP 10 — Store & Release Certification

Persistent signing, exact artifact identity, physical evidence, AAB, Internal Testing and final
`CERTIFIED / NOT CERTIFIED / BLOCKED` verdict.
