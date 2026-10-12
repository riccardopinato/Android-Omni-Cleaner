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
- perceptual/similar-photo hashing moved explicitly to Product Expansion 2 / v1.1;
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
- exact 297-key parity across all five locales;
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
**Status: IMPLEMENTATION COMPLETE — VALIDATION DEFERRED (FAST DEVELOPMENT MODE)**

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
- Step 9 engine baseline: version `0.10.0-recovery` / code 10;
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

## MAXI STEP 9.5 — Post-Audit P1/P2/P3 Hardening
**Status: SOURCE COMPLETE — VALIDATION DEFERRED**

Resolved in source:
- Safe Trash moved from app-private storage to persistent hidden shared storage with verified legacy-entry migration;
- Safe Trash app-managed directory protected from normal cleaner traversal;
- lifetime Premium receives a bounded 30-day offline grace only after prior Play verification;
- cached FREE/unknown state never authorizes ads;
- intentional adaptive/monochrome launcher branding installed;
- shared 120-second storage inventory prevents repeated full scans across core modules;
- Cleaner, Gallery and Pro Batch controllers retained across configuration changes in an Activity ViewModel;
- raw runtime exception text removed from user-facing Cleaner/Gallery/Smart Categories/Pro failure states;
- AdView lifecycle now destroys banner views on composition disposal;
- Android instrumentation contract suite added;
- privacy policy, Data Safety, app-ads/AdMob readiness and quantitative performance documents added;
- redesigned app shell split so Home/Clean implementation is no longer concentrated in one god file;
- domain enums no longer carry Italian presentation labels;
- version `0.11.0-recovery` / code 11.

Evidence boundary:
- source audit: complete;
- exact-head compile/tests/AppLab: deferred while GitHub-hosted runners are unavailable;
- physical/storage/provider/store validation: Step 10.

See `STEP9_5_AUDIT_HARDENING.md`.

## MAXI STEP 10 — Store & Release Certification

Persistent signing, exact artifact identity, physical evidence, AAB, Internal Testing and final
`CERTIFIED / NOT CERTIFIED / BLOCKED` verdict.


## FAST DEVELOPMENT MODE

Temporarily enabled to prioritize implementation and APK delivery while GitHub Actions hosted-runner usage is unavailable.

Rules:
- Step 9 validation is deferred, not converted to PASS.
- AppLab Intelligence and AppLab Runtime are manual-only.
- feature-branch Android CI is deferred; main CI remains available.
- `Fast APK` is the only automatic feature-branch build lane.
- Fast APK performs `assembleDebug` only, publishes a SHA-bound prerelease APK and checksum, and does not claim certification.
- full Step 9 and Store/Release certification gates must be rerun before any production/store claim.
