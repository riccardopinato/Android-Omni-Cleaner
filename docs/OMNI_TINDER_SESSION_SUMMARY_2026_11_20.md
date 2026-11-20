# Omni Cleaner — Proposal 14, Tinder session summary

Approved 2026-11-20. Visual reference: Design-App DSK-011 / Professional Minimal.

## Provenance and counters
- `GalleryUiState.totalReviewedCount`: cumulative reviewed action count persisted in GalleryPreferencesManager. It is not a denominator for progress and is not a lifetime unique-file count.
- `GalleryUiState.selectedForTrash.size`: actual current selected-for-Android-Trash queue (deduplicated by media ID; queue may survive reload/permission change). **Not a deleted or approved count.**
- `GalleryUiState.history.count(KEEP)`: genuinely available but only the bounded, in-memory Undo history (maximum 50 entries). Render **only when history is nonempty**, explicitly labeled “Kept · recent”. Never present `totalReviewedCount - selectedForTrash.size` as the lifetime kept count; persisted queue selection/removal and historic Android approvals make it false.
- No progress percentage, because total number of media to review is not known.

## UI
- `OmniTinderSessionSummary` is a read-only compact neutral-outline row, mounted only in `reviewOnly` Tinder screen, outside the photo/video gesture surface.
- Label warns that selected files have not been deleted and Android separately requests confirmation.
- EN/IT/ES/FR/PT translations. Existing hub review count unchanged.
- No code changes to `GalleryController`, `GalleryMediaPreview`, swipe callbacks, history, permissions or Android Trash.

## Release gates
- [ ] Exact-head Android CI and screenshots PASS before merge.
- [ ] Visual review with 360dp, 150% font, dark mode, TalkBack.
- [ ] Test review, undo, queue unmark, reload, persistence and confirmation to validate numbers.
- [ ] Real-phone destructive-operation QA is separate from source and screenshot checks.
