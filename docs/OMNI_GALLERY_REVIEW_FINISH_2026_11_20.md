# Omni Cleaner — Proposal 15: Professional Minimal review completion

Approved 2026-11-20. Design reference DSK-011 Unslop UI (Design-App). Scope: Gallery Tinder loaded-page completion ONLY.

## Behavior and safety
- Show only if `state.items.isNotEmpty()`, `state.currentItem == null`, and neither initial load nor next-page load is in progress. Do not claim full library scan.
- When `state.hasMore`, label this as **loaded page reviewed**, and offer a real `controller.loadNextPage()` action. When no more loaded pages are signaled, label the loaded review complete, explicitly without asserting the entire device was scanned.
- Counts use `totalReviewedCount` (persisted reviewed actions) and `selectedForTrash.size` (actual pending selections). No invented kept count or fake percentage.
- "Review selection" only appears when non-empty and calls the existing queue review dialog, not Android Trash directly; load-more only when actual pagination is available.
- The original queue's Android confirmation remains separate: existing `showTrashConfirmation`, `createTrashRequest` and `trashLauncher` are **untouched**, as are `GalleryController`, gesture recognition, keep/trash/undo behavior and video playback.
- The old duplicate "Load more", "Review queue" buttons and old text completion line are suppressed only when the new finish panel is visible. Existing queue submit action remains unchanged.

## Localizations and design
- Native Jetpack Compose with a low-contrast 1dp outline, 12dp radius, neutral text hierarchy, no decorative gradient or infinite animation.
- EN / IT / ES / FR / PT translations.
- No new dependency, permission, persistence, source-media read or write.

## Gates
- [ ] Exact-head Android CI PASS.
- [ ] Exact-head screenshot workflow PASS.
- [ ] Manual Android QA with 0 items / final item / next page available / queue empty / queue non-empty / permission revoked / process restart / undo.
- [ ] TalkBack, 320dp screen, large text and contrast QA.
- [ ] Physical device Android Trash consent QA remains a separate release gate.
