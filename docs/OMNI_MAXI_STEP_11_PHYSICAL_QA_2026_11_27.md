# Omni Cleaner — MAXI STEP 11 / Physical Android acceptance

Status: **PENDING DEVICE TESTS / DO NOT CERTIFY RELEASE** (27 November 2026).

## Build and identity
- [ ] Obtain **exact-head** APK; record Git source SHA, APK SHA-256, versionName, versionCode and installed package ID.
- [ ] Debug preview must install as a separate `.debug` package; never interpret that installation as a signed production upgrade.
- [ ] Verify actual production-signing certificate and reinstall/upgrade path with a release-candidate built using approved signing configuration before claiming data-preserving update.
- [ ] Verify there is no user-data loss on in-place update using backup/restore test data, without clearing the app before the test.

## Destructive action gates — synthetic / consented fixtures only
- [ ] Photo, video and audio review: long-press opens read-only preview; only explicit Play starts audio; Fit keeps natural aspect ratio and is not cropped.
- [ ] Checkbox selection changes local review state only. Dismiss without confirmation; verify byte-for-byte identical source files.
- [ ] MediaStore Trash on Android 11+: explicit selection, OS consent, then **independent read-after-write verification** of `IS_TRASHED`/visibility for every selected URI, including cancellation and partial rejection. `RESULT_OK` alone is not proof of Trash state.
- [ ] SAF-granted folders: permission loss/revocation, missing files and restricted Android/data paths must fail safely.
- [ ] Safe Trash: verified move, 50 MB fixture, restore without overwriting existing target, invalid checksum rejection, partial failure and explicit permanent-purge confirmation.
- [ ] Duplicates: verified byte-identical samples, retaining at least one keeper copy, changed file after scan, incomplete scan and unreadable files.
- [ ] App residues and thumbnail cleanup: package visibility ambiguity and snapshot/sha mismatch must block unverified deletion.
- [ ] Runtime permissions for Standard vs Deep access: deny, grant, revoke, app restart. Basic scanning must remain usable without broad access.

## UX and performance gates
- [ ] Test Home and feature pages at 360dp, system font 200%, dark mode, landscape and TalkBack. Confirm all actions are reachable by scrolling and labels are announced.
- [ ] Test Bento drag/reorder/resize and persistence across process death, with no overlapping or unintentionally activated controls.
- [ ] Test localized Quick Look and Trash review with app locale set independently to English, Italian, Spanish, French and Portuguese.
- [ ] Record 3 matched R0/R1 physical SQLite benchmarks on comparable file inventories, report median/p95 and disk footprint, and check no index corruption. Synthetic instrumentation PASS is **not** a physical speedup claim.
- [ ] Compare scroll responsiveness in gallery and large-file lists, memory growth, thumbnail loading, low-memory resume, app cold start and APK download/install experience.
- [ ] Confirm Google Play Billing entitlement/restoration and consented AdMob behavior using approved test accounts. No production purchases or ad requests in QA.

## Evidence to record per test
Device model and Android version; build source SHA; reproducible steps; timestamp; test fixture size/count; expected vs observed; screenshots/logcat without personal file names; PASS/FAIL/BLOCKED; defect link; tester acceptance. No fixture or personal file may be deleted solely to make QA pass. Never publish Play release or merge Draft PR #77 before these gates and visual owner acceptance.
