# Omni Cleaner — proposal 16 UIV-070 selection checkbox

Approved 2026-11-20. Source: Design-App `UI-UX/UIVERSE/CODICE_SORGENTE/UIV-070_original.html` (source blob `62b562fdd2d95acc3d55096357957fd7256621e2`), archived as a multicolor animated round checkbox. The native Compose implementation deliberately uses only the original concept of a round checked mark, dropping the multicolor shadow and CSS animations to match Professional Minimal / DSK-011.

## Functional scope and safety
- Only the queued media item's prior X removal glyph changes to a 25dp circular checked mark with a 48dp touch target.
- The original `onRemove` callback still invokes `controller.removeFromTrashSelection(media.id)`. Items in the queue are selected by definition. Unchecking removes them from the queue and closes the dialog if it is empty; re-selection is possible only through the original Tinder review flow, not from the now-removed list row.
- Checkbox role, checked semantics, and the existing localized per-file remove description are preserved. Existing testTags remain stable.
- No `createTrashRequest`, MediaStore mutation, Android permission, Trash launcher, dialog confirmation or file deletion calls are added.
- Visual testing: 48dp touch, TalkBack, 200% fonts, dark theme and background neutral; no new deps.
- Merge gate: exact-head Android CI and screenshot workflows both PASS. Device QA is separate.
