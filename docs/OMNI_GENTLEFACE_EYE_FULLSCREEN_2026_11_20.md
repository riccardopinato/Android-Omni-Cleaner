# Omni Cleaner — Gentleface Eye (ICO-023) on real fullscreen preview

Approved by product owner: 2026-11-20.
Status: **implemented in isolated branch; exact-head CI and device visual QA pending**.

## Design-App provenance
- Design-App `ICO-023`, Gentleface Eye; source SVG `BIBLIOTECA_COMPONENTI/sorgenti/gentleface/ICO-023.svg`, blob `4899f9fc444a77f19cf1b7322ab8bd161db8544a`.
- Original official source: https://gentleface.com/icons/eye; Gentleface archived source under MIT attribution.
- Added `res/drawable/ic_gentleface_eye.xml`: native Android VectorDrawable with the **exact** SVG path data. No imported icon font, network image, React, Flutter or animation.

## Approved visual contract
- Existing `GalleryMediaPreview` open immersive control only, with identical click callback `showImmersivePreview = true`, button enabled gate and exact original `omni_gallery_immersive_open` test tag.
- Subtle monochrome 20dp icon to the left of the existing localized text; neutral 1dp outline, 10dp rounded corners; no decorative gradients or shadow animations.
- Icon is decorative to TalkBack (`contentDescription = null`); the original localized button text remains the accessible action name.
- No modifications to ContentScale.Fit, VideoView playback, immersive dialog, image/video ratio, undo/keep/trash, media selection, permission or database.

## Acceptance
- [ ] Android CI passes exact HEAD (lint, all JVM tests, debug/release builds).
- [ ] Exact-head screenshot workflow passes.
- [ ] Independently inspect this particular screenshot on actual media fixture.
- [ ] Confirm immersive open/close retains existing Maestro test tags and does not trigger Keep or Trash.
- [ ] 360dp width, text 150%, dark mode, TalkBack and real physical video codec check.
- [ ] Merge only when exact-head PR checks green; no premature certification.
