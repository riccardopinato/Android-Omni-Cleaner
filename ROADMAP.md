# Android Omni Cleaner — Recovery & Product Roadmap

Factory baseline: `03406ef5e511dfc580b67967c67a11ab58e20141` (Master v26.3 / Golden Index v15)

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
- exact 319-key parity across all five locales;
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
- first-pass inventory traversal avoids redundant canonical-path resolution per directory;
- duplicate hashing uses larger 256 KiB IO buffers while retaining SHA-256 + byte verification;
- duplicate scans expose real candidate progress instead of an opaque spinner;
- file-extension dropdown (.apk, .pdf, archives, media and common documents) filters large-file results and duplicate candidates before expensive work;
- Cleaner, Gallery and Pro Batch controllers retained across configuration changes in an Activity ViewModel;
- raw runtime exception text removed from user-facing Cleaner/Gallery/Smart Categories/Pro failure states;
- AdView lifecycle now destroys banner views on composition disposal;
- Android instrumentation contract suite added;
- privacy policy, Data Safety, app-ads/AdMob readiness and quantitative performance documents added;
- redesigned app shell split so Home/Clean implementation is no longer concentrated in one god file;
- domain enums no longer carry Italian presentation labels;
- version `0.11.0-recovery` / code 11.

Final review hardening:
- stale in-flight inventory scans cannot repopulate cache after cleanup invalidation;
- Gallery persists a newly exposed resume anchor immediately after paging;
- retained controllers resolve UI copy through the currently selected locale without retaining Activity instances;
- Pro Android Trash requests live in retained controller state and are launched by the current Activity registry;
- one automatic exact-head PR verification gate is restored and also publishes the development APK when green.

Evidence boundary:
- source audit: complete;
- exact-head fast compile/tests: PASS; full AppLab remains deferred;
- physical/storage/provider/store validation: Step 10.

See `STEP9_5_AUDIT_HARDENING.md`.

## MAXI STEP 9.6 — Persistent Indexing & Bounded Parallel Hashing
**Status: IMPLEMENTED — FAST EXACT-HEAD CI PASS / PRIMARY PHYSICAL PERFORMANCE PASS**

Implemented:
- app-private SQLite persistent file index across process/app restart;
- 120 s in-memory cache remains first-level, persistent reuse extends warm-start behavior;
- warm reuse validates directory fingerprints / MediaStore generation and hydrates persisted file metadata from SQLite without a per-file filesystem stat;
- MediaStore generation mismatch rejects stale persistent snapshots where available;
- manual full-index refresh exposed in Clean;
- directory fingerprints reject stale warm snapshots and force reconciliation after tree changes;
- app-owned cleanup invalidates persistent reuse immediately;
- sample/full hashes persisted only against trustworthy strong revision identity;
- Android API 27+ uses device/inode + high-resolution ctime for hash-cache binding;
- older Android keeps alias identity but refuses persistent derived-hash reuse without a strong revision;
- physical aliases/hard links collapsed before duplicate grouping;
- bounded CPU-aware hashing workers capped at 4;
- no coroutine/task-per-file fanout;
- deterministic result order and throttled progress;
- final byte-for-byte duplicate verification retained;
- version `0.12.0-recovery` / code 12.

Evidence:
- exact-head Android CI run `38484281905`: PASS;
- locale parity: 319 keys in EN/IT/ES/FR/PT;
- JVM regression suite: PASS;
- Android instrumentation sources compile in PR lane;
- debug APK generated at exact head `aa5f6e9951237fcfafaa1c361c4aa643c439ad63`;
- App Factory Golden candidates prepared in Core PR #9 (Golden Index v15 candidate).

Physical evidence:
- exact Step 9.7 APK on Xiaomi M2101K6G / SDK 33 / 8 processors;
- 87,136-file library;
- cold inventory 61,064 ms;
- warm persistent HIT 2,599 ms, 23.50x speedup, file-count delta 0;
- hot-memory HIT 1 ms;
- hashing: 1 worker 573.99 MiB/s, 2 workers 920.86 MiB/s, 4 workers 1,267.33 MiB/s;
- thermal status 0 -> 0, battery temperature 35.3 C -> 37.2 C;
- bounded peak in-flight respected exactly.

Pending:
- second representative lower-end/older physical device for Golden promotion;
- full AppLab/certification lane.

See `STEP9_6_PERSISTENT_INDEXING_BOUNDED_HASHING.md`.

## MAXI STEP 9.7 — Physical Performance Validation
**Status: PRIMARY PHYSICAL DEVICE PASS — SECOND-DEVICE GOLDEN PROMOTION EVIDENCE PENDING**

Implemented:
- in-app Performance Lab in Clean;
- one forced cold full inventory measurement;
- persistent warm reuse measured after dropping only the memory cache;
- immediate hot in-memory measurement;
- deterministic real-file SHA-256 benchmark with 1 / 2 / 4 workers;
- bounded sample: max 200 files / 128 MiB / 4 KiB–32 MiB per file;
- peak in-flight worker measurement;
- battery %, battery temperature and Android thermal-state capture when available;
- copyable `OMNI_PERF_V1` report for evidence;
- benchmark is read-only and never authorizes cleanup;
- version `0.13.0-recovery` / code 13.

Validated physical evidence:
- exact head `436e884886dbc7e034547b3a7b7299a9cd649f7b`;
- Android CI #370 PASS;
- Xiaomi M2101K6G / SDK 33 / 8 processors;
- 87,136 files;
- cold 61,064 ms;
- warm persistent 2,599 ms / HIT / 23.50x / file-count delta 0;
- hot memory 1 ms / explicit HIT;
- 4 workers fastest at 1,267.33 MiB/s with no thermal-status escalation;
- benchmark remained non-destructive.

Remaining:
- second representative lower-end/older device before promoting the two performance Goldens beyond DRAFT.

See `STEP9_7_PHYSICAL_PERFORMANCE_VALIDATION.md`.

## MAXI STEP 10 — Store & Release Certification
**Status: RELEASE PIPELINE PREPARED — EXTERNAL GATES BLOCKED**

Implemented in source:
- full CI compiles release APK + AAB;
- production signing configuration is fail-closed;
- dedicated manual `Android Release Candidate` workflow;
- persistent signing secrets required;
- expected public certificate SHA-256 required;
- signer mismatch fails the workflow;
- signed APK/AAB SHA-256 evidence generated;
- production AdMob IDs required when production monetization is enabled;
- no debug-signing fallback;
- no fabricated provider IDs.

External/user-action gates:
- generate and securely back up permanent release/upload keystore;
- configure GitHub signing secrets + expected certificate fingerprint;
- provision Play Console app and Play App Signing;
- upload exact signed AAB to Internal Testing;
- activate/test `premium_unlock`;
- provision production AdMob / UMP / app-ads.txt;
- publish final privacy-policy URL;
- complete physical storage / SAF / MediaStore / Billing validation;
- reconcile Data Safety and MANAGE_EXTERNAL_STORAGE declarations against the exact release artifact.

Current certification verdict:
**BLOCKED** until those external gates are evidenced.

See `STEP10_STORE_RELEASE_CERTIFICATION.md`.


## FAST DEVELOPMENT MODE

Temporarily enabled to prioritize rapid implementation and APK delivery. GitHub-hosted runners are available again; full AppLab/Store certification remains intentionally deferred until the implementation cycle stabilizes.

Rules:
- Step 9 validation is deferred, not converted to PASS.
- AppLab Intelligence and AppLab Runtime are manual-only.
- pull requests run one automatic exact-head fast gate: locale parity + JVM tests + `assembleDebug`;
- the same successful PR job publishes the SHA-bound development APK + SHA-256, avoiding a duplicate runner;
- full lint + debug/release builds remain on main/manual verification;
- `Fast APK` remains a manual fallback only and does not claim certification.
- full Step 9 and Store/Release certification gates must be rerun before any production/store claim.
