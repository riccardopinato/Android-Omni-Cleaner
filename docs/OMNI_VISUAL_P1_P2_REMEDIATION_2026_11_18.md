# Android Omni Cleaner — P1/P2 visual remediation (2026-11-18)

Status: **SOURCE IMPLEMENTED / CI AND VISUAL REVIEW PENDING**.
Source base: `main` b2bd4381ddd6e7e880eebb3a25b6bee03d9aafec (merged PR #35).
Audit evidence: 20 emulator screenshots, PR #35 run 44309534289 and written 20-screen audit. Reuse Design-App references, **adapted in Kotlin/Compose**, not third-party source copied.

## P1 source changes
- **Gallery 150%/360dp**: three filters now use FlowRow, selected checked label and semantics, inactive neutral branded controls. Loading the library is the primary action; Gallery Pro is a secondary destination. Exact tags: `omni_gallery_filter_all/images/videos`.
- **Threshold units / search filters**: Bytes/KB/MB selected has checkmark plus distinct brand surface, unselected a neutral surface, TalkBack `selected` and stable test tags. Big-file filter follows the same distinction.
- **Action hierarchy**: `OmniActionButton` retains branded primary; `OmniSecondaryButton` and `OmniTextButton` are first-party Compose styling wrappers, preserving existing callbacks. Explicit Gallery Trash/Keep buttonColors are now honored rather than silently overridden.
- **Manage Pro provider error**: readable error container and separate secondary Restore button, no invented price, no new provider requests or entitlement rules.
- **Video evidence**: synthetic fixture captures actual Android Settings pixels instead of a near-black idle emulator; stores `video-fixture-reference.png`, tests native video play/fullscreen/Close, plus `21-gallery-360dp-large.png` at forced 360dp and font 1.5. Source changes do **not** certify video playback until new screenshots are reviewed.

## P2 source changes
- Home tiles accommodate larger text without single-line clipping and compact vertical rhythm at 150%.
- Clean hub: storage overview is full-width priority tile when space allows, nine original routes and stable test IDs preserved; selected filters distinguish true state; Cancel is secondary.
- Privacy/Pro hub: compact guidance after access cards instead of three equally prominent instructional steps. Detail flows remain functional.
- One-photo EXIF picker primary; Batch EXIF scan disabled reason displayed when no source folder.
- Review Center: one primary scan discovery and two lower-emphasis secondary discovery routes; real empty-state copy retained.
- Gallery Pro: contextual empty state with a route to media permission/review, wrapped video filters, quieter Review action.
- Storage Intelligence: bars explicitly display the actual computed percentage of **indexed bytes**, never full storage capacity or scan progress.

## Design-App mapping
| Reference | Mode | Reason |
|---|---|---|
| UIX-STYLE-006 / DCL-002 | ADATTATA | Priority-based wide storage tile / compact responsive Bento; no asset copied |
| UIX-STYLE-009 / DCL-007 | ADATTATA | Semantic primary/secondary/selected controls and quiet guidance |
| DCL-005 | ADATTATA | Truthful proportional indexed-data map |
| DCL-006 / WML-003 | ADATTATA CONCEPT | Existing native VideoView/MediaController and original-ratio media retained; new fixture only |
| DCL-011 | ADATTATA | Contextual no-library and no-folder state |
| Third-party Uiverse art / unrelated 3D | NON ADATTA | Not required; would add size/performance risk |

## QA and release boundary
- Run exact-head Android CI, locales EN/IT/ES/FR/PT, JVM, lint, debug, androidTest APK, release APK and AAB compilation. **PASS may only be recorded after workflow success.**
- Run exact-head Android emulator Maestro on primary/detail/video/dark-large **and** forced 360dp/font 150%; review rendered pixel artifacts against PR #35 with identical theme/locale/device where possible.
- 200% font, physical OEM codec playback, photos portrait/landscape, real MediaStore Trash/Undo, billing, destructive permissions, physical accessibility, signed production APK/AAB remain **NOT VERIFIED** until separate evidence.
- No changes to destructive cleanup engines, DB, permissions, Billing provider code, signing identity, package, release version or Play Console.
- Provenance **does not automatically promote Design-App catalog** from PROPOSED_ONLY; record consumer source proof only after exact-head success and approved merge.

Related: [PR #35](https://github.com/riccardopinato/Android-Omni-Cleaner/pull/35), [issue #36](https://github.com/riccardopinato/Android-Omni-Cleaner/issues/36).
