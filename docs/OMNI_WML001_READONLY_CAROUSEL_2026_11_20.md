# Omni Cleaner — WML-001 Read-only Carousel Professional Minimal

Approved on 2026-11-20 with explicit owner clarification: the existing **Tinder swipe** (right=Keep, left=select for Android Trash) must remain **unchanged**, and the Carousel must live in a distinct non-destructive Gallery hub surface.

## Source
- Design-App `sources/WATERMELON_UI_NINE_AUDIT_2026-11-17.md` and `PER_APP/omni-cleaner.md`.
- Watermelon source `WatermelonCorp/watermelon-platform` commit `9d6defcf89880d0a00fbccad773c046bb51d7afb`, `src/data/contents/animated-components/carousel-slider/base.tsx` blob `ccd0d565f1cd15ea44bf449ef7bdbfbc7cd0c25b`.
- Borrowed: bounded horizontal navigation concept, selected item and pagination. Discarded: excessive rotations, brightness/scale effects, bouncy spring, favourite control and remote sample images. No JS library integration.

## Scope and code
- `OmniReadOnlyMediaCarousel.kt`: renders a maximum of 12 **real loaded MediaStore entries** using existing `loadPreviewBitmap` in an IO coroutine; 185dp contained Fit preview, filename/size, previous/next buttons, independent horizontal gestures and a read-only full-screen dialog.
- Fullscreen photo stays `ContentScale.Fit` and uses the decoded original-aspect preview; fullscreen video reuses existing `GalleryVideoPlayer` with WML-003 player, without a second codec implementation.
- `GalleryCleanerSection.kt`: carousel only on Gallery **hub** (`!reviewOnly`), not inside the Tinder review screen. Existing `onOpenReview`, `GalleryMediaPreview`, Keep/Trash swipe gestures, Undo, and Android Trash confirmation all remain intact.
- No `onKeep`, `onTrash`, `createTrashRequest`, `removeFromTrashSelection` or delete callback is accepted in the Carousel signature or dialog. Fullscreen only opens/closes/plays, no mutation.
- When loaded page/URIs change, selection and open dialog reset. No external networking, no asset loading from the Watermelon demo.
- New text labels in EN/IT/ES/FR/PT.

## Acceptance and safeguards
- [ ] Exact-head Android CI + screenshot PASS.
- [ ] Confirm carousel appears only on hub, Tinder remains in detail screen and horizontal gestures cannot be interpreted across the two surfaces.
- [ ] Test 0 / 1 / 12 / >12 loaded items, page change, revoked URI permission, large media, videos, controls and closing fullscreen.
- [ ] TalkBack / text scaling / rotation / dark mode and memory discipline.
- [ ] Physical OEM test for actual video previews and Android Trash consent before release.

Source implemented; **runtime certified status requires passing gates**, not just code committed.
