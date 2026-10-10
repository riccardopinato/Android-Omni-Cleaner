# Android Omni Cleaner — Recovery & Product Roadmap

Factory baseline: `a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03`

## MAXI STEP 0 — Forensic Recovery
**Status: COMPLETE**

- donor APK inventoried;
- native Kotlin/Compose stack confirmed;
- package, permissions, signing and release debt identified;
- recoverable class/method/resource surface documented.

## MAXI STEP 1 — Native Project Bootstrap
**Status: COMPLETE**

- clean Kotlin/Compose project;
- canonical package `com.riccardopinato.omnicleaner`;
- Gradle/JDK/AGP baseline;
- debug/release separation;
- R8/resource shrinking;
- Android CI.

## MAXI STEP 2 — Full APK Functional Reconstruction
**Status: SOURCE-LEVEL COMPLETE / RUNTIME PARITY PENDING**

Recovered from donor evidence:
- storage overview and inventory;
- folders, threshold and search;
- large files and filters;
- duplicate SHA-256 engine;
- keep-oldest / keep-newest;
- guarded review and cleanup;
- Safe Trash;
- MediaStore system trash;
- thumbnail/cache scanning and verified cleanup;
- accessible app-residue scanning;
- manual maintenance;
- Gallery Cleaner with paging, prefetch, visual review, Keep/Trash and Undo;
- EXIF privacy cleaning;
- exact five-category Pro scan contract;
- Pro DataStore selection;
- Pro candidate review and guarded resolve flow;
- donor one-time Billing SKU `premium_unlock`;
- UMP consent;
- donor ad cadence: every 3 completed cleanups with minimum 180 seconds;
- Premium blocks ad requests;
- debug/test and production ad configuration separated.

Intentional recovery differences:
- app residues are review-only where package visibility makes uninstall state ambiguous;
- irreversible large non-media deletion is not used as a fallback;
- stale donor UI wording such as MD5 is not preserved when actual donor algorithm is SHA-256;
- visual parity is not a gate because a redesign is planned.

Exit still pending:
- trusted runtime / physical-device parity.

See `DONOR_PARITY_MATRIX.md`.

## MAXI STEP 3 — APK Parity Audit
**Status: NEXT**

Compare donor APK and reconstructed artifact using the same representative fixture/device state.

Required checks:
- storage counts and sizes;
- folder classifications;
- large-file results;
- duplicate grouping/keeper behavior;
- thumbnail results;
- Gallery paging/swipe/undo/trash;
- EXIF safe-copy behavior;
- Pro scan/review/resolve;
- permissions and degraded states;
- cleanup result truth.

Evidence states:
`RECOVERED / IMPROVED / INTENTIONALLY_CHANGED / MISSING / BLOCKED`.

Current external blocker:
AppLab trusted runtime requires GitHub Actions artifacts; account artifact quota is saturated. Static AppLab Intelligence remains operational but cannot substitute for runtime evidence.

## MAXI STEP 4 — Safety & Storage Hardening

- capability/permission health model;
- scoped-storage-first lanes;
- explicit Deep Cleaning lane if all-files access remains justified;
- destructive-flow revalidation;
- policy review for `MANAGE_EXTERNAL_STORAGE`;
- app-residue strategy decision.

## MAXI STEP 5 — Monetization Productization

Implementation baseline already recovered during Step 2; this step productizes it:
- configure Play Console one-time product;
- Billing test purchases/restore/acknowledge;
- production AdMob IDs;
- UMP/privacy validation;
- decide final Free/Pro feature boundary;
- decide final price;
- verify Premium generates zero ad requests.

## MAXI STEP 6 — UI/UX Redesign

Use the user's later design ideas without rewriting the recovered engines.

Targets:
- Home/storage visualization;
- navigation and information architecture;
- result/review screens;
- Gallery Cleaner presentation;
- Premium screen;
- motion/feedback;
- polished light/dark themes.

## MAXI STEP 7 — Product Expansion

Evaluate, do not blindly add:
- screenshots;
- old downloads;
- large videos;
- similar/burst media;
- storage history/health;
- smarter recommendations.

No fake RAM/CPU/battery/antivirus claims.

## MAXI STEP 8 — Localization & Accessibility

- en/it/es/fr/pt;
- device language + manual selector;
- centralized strings;
- TalkBack/content semantics;
- font scaling/touch targets;
- responsive phones/tablets.

## MAXI STEP 9 — Performance & Deep QA

Stress:
- large file trees;
- 50k+ media;
- storage almost full;
- cancellation/background/process death;
- revoked permissions;
- stale files during scan;
- low-end hardware;
- memory/I/O/ANR monitoring.

## MAXI STEP 10 — Store & Release Certification

- persistent INTERNAL signing;
- exact-artifact hashes;
- physical-device testing;
- update-in-place;
- AAB;
- Play Internal Testing;
- Billing/Ads/UMP;
- storage-policy review;
- final verdict: `CERTIFIED / NOT CERTIFIED / BLOCKED`.

## Certification rule

Build success is evidence, not certification.

`IMPLEMENTED -> STATICALLY CHECKED -> TESTED -> CI GREEN -> ARTIFACT BUILT -> TRUSTED RUNTIME VERIFIED -> PHYSICAL DEVICE VERIFIED -> DISTRIBUTION VERIFIED -> STORE READY`
