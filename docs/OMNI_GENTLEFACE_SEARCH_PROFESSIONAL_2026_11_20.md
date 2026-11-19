# Omni Cleaner — Gentleface Search, Professional Minimal

Approved by product owner on 2026-11-20. Status: **source implemented in isolated GitHub feature branch; exact-head Android CI & screenshot QA pending**.

## Origin
- Design-App source: `BIBLIOTECA_COMPONENTI/sorgenti/gentleface/ICO-011.svg`, UID-0065, archived MIT, original `https://gentleface.com/icons/search`.
- The source SVG is a subtle round 16px magnifier with thin handle; this is translated into native Jetpack Compose Canvas strokes, without a dependency.
- User-approved aesthetic: Professional Minimal, restrained monochrome single icon in the video search field, a neutral clear control (X), no animation, gradient, extra card, glass or badge.

## Consumer behavior
- `GalleryProSection` search field retains `omni_gallery_pro_video_search` tag, localized label, single line, and immediate filtering of video name/album.
- Decorative search prefix `OmniGentlefaceSearchGlyph()` is excluded from screen-reader duplication.
- Clear button appears only when text is nonempty, has 48dp native touch semantics, localization in EN/IT/ES/FR/PT, test tag `omni_gallery_pro_video_search_clear` and actually executes `videoQuery = ""`.
- Clear action does not change pre-existing All/100MB+/5min+/4K presets or the WML-004 applied size range. Filtering remains scoped to the loaded page.
- No changes to media player, permissions, preview, system Trash, deletion, Pro entitlement or billing.

## QA
- [ ] Exact-head Android CI, lint, unit tests, debug/release build and AAB PASS.
- [ ] Exact-head screenshot workflow PASS; manually inspect Gallery Pro field states.
- [ ] Input/clear/keyword not found, keyboard focus, and TalkBack test.
- [ ] Narrow device widths, dark mode and 150% font scaling.
- [ ] Confirm parent PR #51 merges first and no default branch race; preserve #54 independently.
