# Android Omni Cleaner — PR consolidation and release gates (2026-11-21)

Status: **INTEGRATION CANDIDATE / NOT MERGED / NOT APK READY**
Factory baseline: `riccardopinato/App-Factory-Core@4df94ac3b3ea22a2323634fe83c3c7770fe3b835`, Master v27, Golden Index v31.
Integration source baseline: PR #75 `c8e785f89c4dc852799171718d380e97d2040966`. All later edits are in this separate integration branch; never force-push #75 or overwrite independent work.

## Five requested workstreams

1. **#73 + #76 + #75**: #75 is already a descendant of #73 (the 73-to-75 compare is 39 ahead / 0 behind). Color-only PR #76 has been transplanted with exact nine color HEX values, navy/white contrast handling, JVM palette tests, and original provenance document. Original feature branch #75 is untouched; no automatic merge.
2. **Gallery Pro + visual journeys**: correct the screenshot journey so the "Gallery Pro" tile is tapped by its visible text, then assert actual `omni_gallery_pro` content. Use Maestro's documented `centerElement: true` on Bento tiles because a visible semantics tag at a viewport edge can fail a real tap. Re-run emulator evidence; no fabricated PASS.
3. **#38 / #39 / #49**: #38 is an ancestor of #39. Main/PR75 lacks R0/R1 diagnostic and compiled SQLite UPDATE. Exactly port #39's three source files, JVM and instrumented tests, stress fixture, R0/R1 documentation and scoped workflow. Latest PR75 already has genuine 360dp/200% Gallery screenshots from #49; bring only #49's missing temporal motion-check and screenshot assets, not its superseded navigation script. R1 physical benchmark remains **NOT CERTIFIED** until matched real-device measurements and zero data-loss validation.
4. **Historical PR triage**:
   - #27: PR head is an ancestor of current main (main 31 commits ahead and zero behind); the scan/SafeTrash source and Scan 2.0 dossier are already byte-identical. Close as already integrated via later lineage after final check; do not merge again.
   - #33: earlier v1.1 realignment candidate diverged from main after the selected source became available in main; its older design/galleries must not overwrite newer main. Close obsolete candidate with canonical lineage preserved; do not merge.
   - #41: apply only the two optional-clean header callsites and nullable eyebrow component, not old visual paths; keep original PR as historical until this branch passes.
   - #40: earlier quiet-utility visual pilot has not passed user approval and modifies the old shell; **HOLD / review individually** instead of wholesale merge.
   - #18: historical signed RC1 evidence is retained as `RELEASE_EVIDENCE_RC1.md` with a source-specific warning; do not claim new v1.1 signed release from old source SHA. Keep release gate separate.
   - #72: evaluation-only release PR must **never be merged**; its workflow trigger is a temporary evaluation lane.
   - #74: donor audit imported as DOCUMENTATION ONLY (SD Maid GPL code not copied).
   - #38 / #49 are not auto-merged or deleted while QA on combined code is pending.
5. **Final verification**: exact-head build and locale/JVM gate; Android native screenshots, dark mode, 360dp / 200% font, real video frames, read-only scanner paths and R1 sqlite instrumentation. Physical tests with user-owned files remain separate, including duplicate keeper, Safe Trash 50 MB, restore/no-overwrite, Android MediaStore confirmations, SAF permissions, storage sources and actual premium purchases. New signed APK/AAB must use existing production identity and checksum proof. No device data is removed automatically to make tests pass. No APK or Play release now.

## Evidence boundaries
- This document records source changes and reconciliation decisions; it is **not** independent test evidence.
- 2026-11-21 previous #75 visual workflow `44843405311` failed on `omni_clean_detail_filters`. Earlier `44767316818` failed on "Gallery Pro"; these exact failures motivated the Maestro fixes.
- The existing source #75 CI and synthetic-safety successes do NOT apply to this new SHA.
- PR #39 emulator R1 PASS on its head is not a physical 88k-file performance speedup, and a combined branch requires its own exact-head execution.
- Screenshots must be manually inspected, including all palettes and loading/empty/non-empty states.
- The final merge target is only the reviewed, test-passing integration of the single Home and distinct feature screens. Never auto-promote a Draft branch to main solely because compilation passes.

## MAXI STEP 11 — integration checkpoint (27 November 2026)

- The previous `d7fb405` exact-head Android CI, synthetic safety, SQLite instrumentation, and visual workflow completed successfully. A genuine QA archive was inspected. **Those results do not certify any newer commit.**
- Verified discrepancy: the current Soft Pastel palette differs from the older Pop Fresh description above; current behavior is governed by `OmniColorBentoV2Palette`.
- The Quick Look and file-trash review surfaces have been moved from hard-coded Italian labels to English, Italian, Spanish, French and Portuguese Android resources. Only Safe Trash movement can now emit the completed-cleanup event; Android media-trash consent alone is not counted as completed.
- The canonical `Android CI` workflow already publishes exact-head **debug** APK prereleases for in-repository PRs (source SHA and SHA-256). The separate fast-preview job is reserved for its original feature branches and uses `setup-gradle` to avoid cold-cache dependency lookup failures. `Android CI` release output is still pending a successful run. Debug APK is not a production update or release certification.
- The two upstream `main` commits modify only the App Factory context workflow files, which are copied unchanged into the integration tree with `main` as the second merge parent. Do not merge this Draft PR into `main` until the owner accepts the screenshots, hardware tests and release-signing gates.
- Physical-device cases and evidence requirements: `docs/OMNI_MAXI_STEP_11_PHYSICAL_QA_2026_11_27.md`.
