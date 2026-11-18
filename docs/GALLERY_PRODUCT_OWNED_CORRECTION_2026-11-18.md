# Omni Cleaner — Gallery & navigation visual correction (2026-11-18)

## Why
PR #31 completed media preview/player QA, but the screenshot still showed the legacy Gallery material Card embedded in ModuleScreen. This contradicted the product-owned 2026–27 redesign request. CI PASS did not imply aesthetic acceptance.

## Baseline
App Factory Core pinned at d660cccd57f2e252c573c484457f1ae9234b1e33; Master v27, Golden Index v29. Repository base main@3b54e90c6ad5d56bf864b4d0baa83bff6273c29e.
Design-App patterns: DCL-003 semantic chroma, DCL-004 quiet nav, DCL-006 media-first gallery, DCL-007 contextual actions, DCL-011 considered empty state, DCL-012 polish without UI slop.

## What changed
- True full-height gallery destination; no generic ModuleScreen around the old card.
- Branded header; independent segmented media filters and custom sort/load controls.
- Photo/video as a focused media stage preserving ContentScale.Fit and original proportions in fullscreen.
- Persistent review dock (Keep/Trash with Undo), visible above bottom navigation.
- App navigation uses product-owned icon/label selection surfaces instead of standard Material NavigationBar.
- Localized strings, permissions, empty states, paging, error and trash confirmation preserved.

## Safety and validation
- GalleryController, MediaStoreHelper, trash request and system confirmation are untouched.
- Keep and Trash mean review decisions, never direct deletion.
- CI, media fixture Maestro and visual screenshots required before merging; no Play/OEM certification inferred from emulator.
