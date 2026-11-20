# Omni Cleaner — Proposta 13: Dettagli media Professional Minimal

Owner approved 2026-11-20.

## Visual source
- User-curated Design-App reference `DSK-011` Unslop UI, restrained neutral 1dp borders, 10dp radius and typography-led hierarchy.
- Scope: one informational metadata surface **below** the existing Tinder media frame, only in `GalleryCleanerSection(reviewOnly = true)`. Do not add decorative elements that reduce photo/video viewport.

## Implementation contract
- `OmniTinderMediaDetails.kt`: name, real MIME subtype, real file size; date only when `dateTaken > 0`, album only if provided, resolution only when both dimensions positive, duration only for video when positive. No fabricated values or content URIs displayed.
- `GalleryCleanerSection.kt`: the original hub compact metadata is unchanged. The Tinder detail replaces only the two text lines after the existing `GalleryMediaPreview`, **never** its swipe handler or callbacks.
- The read-only composable accepts only `GalleryMediaItem`; no Keep, Trash, trash request, write permission or MediaStore mutation callbacks.
- EN, IT, ES, FR, PT localization.
- No new permissions, queries, media decoding, network access, dependencies or migrations.

## QA
- [ ] Exact-head Android CI + screenshots both PASS before merge.
- [ ] Real device QA: 320–360dp wide, 150–200% fonts, TalkBack, dark theme, long filenames.
- [ ] Missing date/album/resolution/duration should disappear rather than show placeholders.
- [ ] Photo/video aspect ratio preserved, carousel separate, Tinder right=Keep and left=select Trash unchanged.
- [ ] No media can be deleted without existing Android system confirmation.

This is SOURCE_IMPLEMENTED, not a physical device certification.
