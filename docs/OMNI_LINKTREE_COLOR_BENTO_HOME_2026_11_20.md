# Omni Cleaner — Color Bento Home v1 (evaluation only)

**User approval:** 2026-11-20. **Status:** SOURCE_IMPLEMENTED_CANDIDATE; visual acceptance, exact-head CI, usability and physical QA required. Do not merge without owner acceptance.

## Source of truth for visual design
- Six user-provided Linktree screenshot references in ChatGPT conversation 2026-11-20. Visual reference ONLY; no vendor photographs, logo, JS or other asset copied.
- Palette research: https://uicolours.com/sites/linktree (captured 2025-12-09; seven dominant HEX values #D2E823, #254F1A, #780016, #E9C0E9, #2665D6, #E8EFD6, #1E2330).
- Supplemental user palette: #F3F3F1 off-white, #061492 cobalt, #502274 purple, #E8B838 ochre, #13A8C9 cyan. Olive #70774F and magenta #D20CDF come from the screenshot design, not claimed as official primary brand tokens.
- Typography: Space Grotesk Variable (display) + Manrope Variable (body), exact source blobs from google/fonts on 2026-11-20 and complete SIL OFL license text in docs/fonts. Typeface assets stored locally in res/font. No runtime font downloads.
- This is an **original design interpretation**, not a Linktree clone or derivative branding.

## Design measurements and anti-slop requirements
- Off-white chalk page (#F3F3F1), cobalt/olive/lilac/magenta tile planes, bordeaux only for the quick-tool panel. No tinted global burgundy overlay in light mode.
- 17dp horizontal inset (13dp on smaller phones), 10dp card gutter, 26dp tile radius, 14dp vertical rhythm, 48dp settings target. Header display 25sp, numerical displays 26–33sp, bodies 11–12sp in Manrope; no unnecessary shadows, fake chrome or rainbow gradient.
- First row: storage + loaded media, weights 1.75:1; second row: verified duplicates + Gallery, weights 1:1.75; third row: large files; fourth row: touch-to-expand original stacked cards; last section: actual Clean, Privacy, Pro actions and explicit safe cleaning/capability information.
- Motion: tool deck morphs between compact angled stack and accessible, selectable cards using native Compose 420ms finite tween. Cards launch real existing destinations, and never create trash requests. No automatic, infinite or decorative motion on the media review surface.
- Respect dark mode, small screen, font scaling, insets, live data, and localization; all five existing tabs remain intact. Skip fake screenshot media thumbnails and synthetic metrics.

## Real data and behavior
- Storage used/total from CleanerUiState.storageInfo, total percent only for positive total. Original waveform is a decorative brand motif, **not** a storage trend.
- Verified duplicates only when duplicatesScanned=true; otherwise "—", not zero.
- Large files total only when bigFilesScanned=true; otherwise "—", not a claim of savings or automatically deletable bytes.
- Gallery loadedSourceCount is explicitly **loaded media only**, NEVER total device library, and observer is unsubscribed in DisposableEffect.
- Existing callbacks onOpenClean, onOpenGallery, onFindDuplicates, onOpenLargeFiles, onOpenTrashQueue, onOpenPrivacy, onOpenPro, onRefresh, onEnableDeepClean are the sole behavior changes. No new permissions, deletion actions, Billing or database migration.
- Gallery Tinder, review queue, Android system Trash, media playback, Undo and consent flows are untouched.

## Mandatory evidence before merge
- [ ] Exact-head Android CI PASS including release/build + unit tests and locales EN, IT, ES, FR, PT.
- [ ] Android visual screenshot workflow PASS on exact PR head.
- [ ] **Human visual review of the *actual* Android screenshots vs the user-approved reference**: palette, font glyphs, line breaks, density, icon alignment, proportions, no clipping, no generic AI/UI plasticity. Screenshot workflow PASS alone proves capture, **not fidelity**.
- [ ] 360dp, 393dp, 200% font, dark mode, TalkBack, reduced motion, Android gesture navigation and Ads/safe areas.
- [ ] Validate empty storage, ungranted access, no scans, zero actual duplicates, non-empty gallery, slow media loading.
- [ ] Verify quick-tool card tap expands/collapses and launches only its existing non-destructive destination.
- [ ] Owner aesthetic acceptance before merge to main. No release, no APK claimed from this PR.

Implementation is deliberately isolated to a design PR. Preserve main signed APK path unaffected.
